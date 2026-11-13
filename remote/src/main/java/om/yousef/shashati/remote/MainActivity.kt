package om.yousef.shashati.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import om.yousef.shashati.remote.data.TvStore
import om.yousef.shashati.remote.net.Controller
import om.yousef.shashati.remote.net.FoundTv
import om.yousef.shashati.remote.ui.Background
import om.yousef.shashati.remote.ui.C
import om.yousef.shashati.remote.ui.PairScreen
import om.yousef.shashati.remote.ui.RemoteScreen
import om.yousef.shashati.remote.ui.RemoteTheme
import om.yousef.shashati.remote.ui.TvListScreen

class MainActivity : ComponentActivity() {
    private sealed interface Screen {
        data object Loading : Screen
        data object List : Screen
        data class Pair(val tv: FoundTv) : Screen
        data object Remote : Screen
        data object Install : Screen
    }

    private var screen by mutableStateOf<Screen>(Screen.Loading)
    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TvStore.init(this)
        lifecycleScope.launch {
            // Creating the phone's identity the first time takes a moment.
            withContext(Dispatchers.Default) { Controller.init(this@MainActivity) }
            val last = TvStore.lastHost?.let { TvStore.get(it) }
            if (last != null) { Controller.select(last); screen = Screen.Remote } else screen = Screen.List
            if (started) Controller.connect()
        }
        setContent {
            RemoteTheme {
                when (val s = screen) {
                    Screen.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Background(); CircularProgressIndicator(color = C.gold)
                    }
                    Screen.List -> TvListScreen(
                        onOpen = { tv -> open(tv) },
                        onPair = { found -> screen = Screen.Pair(found) },
                        onInstall = { screen = Screen.Install },
                    )
                    Screen.Install -> {
                        BackHandler { screen = Screen.List }
                        om.yousef.shashati.remote.ui.InstallScreen(onClose = { screen = Screen.List })
                    }
                    is Screen.Pair -> {
                        BackHandler { screen = if (Controller.tv != null) Screen.Remote else Screen.List }
                        PairScreen(
                            s.tv,
                            onDone = { tv -> open(tv) },
                            onCancel = { screen = if (Controller.tv != null) Screen.Remote else Screen.List },
                        )
                    }
                    Screen.Remote -> {
                        BackHandler { moveTaskToBack(true) }
                        RemoteScreen(
                            onSwitchTv = { Controller.disconnect(); Controller.select(null); TvStore.lastHost = null; screen = Screen.List },
                            onRepair = {
                                val tv = Controller.tv ?: return@RemoteScreen
                                Controller.disconnect()
                                screen = Screen.Pair(FoundTv(tv.host, tv.name, hasKeys = true, hasShashati = tv.token != null))
                            },
                        )
                    }
                }
            }
        }
    }

    private fun open(tv: om.yousef.shashati.remote.data.SavedTv) {
        TvStore.lastHost = tv.host
        Controller.disconnect()
        Controller.select(null)
        Controller.select(tv)
        Controller.connect()
        screen = Screen.Remote
    }

    override fun onStart() {
        super.onStart()
        started = true
        if (screen == Screen.Remote) Controller.connect()
    }

    override fun onStop() {
        super.onStop()
        started = false
        Controller.disconnect()
    }
}
