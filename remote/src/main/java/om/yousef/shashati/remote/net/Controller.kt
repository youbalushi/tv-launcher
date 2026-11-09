package om.yousef.shashati.remote.net

import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import om.yousef.shashati.remote.atv.AtvRemote
import om.yousef.shashati.remote.atv.ClientIdentity
import om.yousef.shashati.remote.data.SavedTv

/** Keeps the connection to the TV that is being controlled right now. */
object Controller {
    lateinit var identity: ClientIdentity
        private set
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var tv by mutableStateOf<SavedTv?>(null)
        private set
    var keysState by mutableStateOf(AtvRemote.State.Offline)
        private set

    private var remote: AtvRemote? = null
    val api: ShashatiApi? get() = tv?.let { ShashatiApi(it.host, it.token) }

    fun init(context: Context) {
        if (!::identity.isInitialized) {
            identity = ClientIdentity.loadOrCreate(context.filesDir, "atvremote/" + (Build.MODEL ?: "phone"))
        }
    }

    fun select(newTv: SavedTv?) {
        if (newTv?.host != tv?.host) disconnect()
        tv = newTv
    }

    fun connect() {
        val t = tv ?: return
        if (!t.keysPaired || remote != null) return
        remote = AtvRemote(t.host, identity) { s -> scope.launch { keysState = s } }.also { it.start() }
    }

    fun disconnect() {
        remote?.stop()
        remote = null
        keysState = AtvRemote.State.Offline
    }

    /** Sends a key. Without Google pairing, back/home/volume still work through شاشتي. */
    fun key(code: Int, direction: Int = AtvRemote.SHORT) {
        val r = remote
        if (r != null && keysState == AtvRemote.State.Connected) {
            r.key(code, direction)
            return
        }
        val a = api ?: return
        scope.launch {
            when (code) {
                AtvRemote.BACK -> a.back()
                AtvRemote.HOME -> a.home()
                AtvRemote.VOL_UP -> a.volume("up")
                AtvRemote.VOL_DOWN -> a.volume("down")
                AtvRemote.MUTE -> a.volume("mute")
            }
        }
    }
}
