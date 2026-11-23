package om.yousef.shashati

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import om.yousef.shashati.data.Channel
import om.yousef.shashati.data.ChannelRepository
import om.yousef.shashati.ui.AppTheme
import om.yousef.shashati.ui.C
import om.yousef.shashati.ui.Players

/** Full-screen live channel. Up/Down change channel, OK opens the channel list, long OK toggles favourite. */
@OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {
    private lateinit var exo: ExoPlayer
    private var channels: List<Channel> = emptyList()
    private var numbers: Map<String, Int> = emptyMap()
    private var index by mutableIntStateOf(0)
    private var urlIndex = 0
    private var showInfo by mutableStateOf(true)
    private var loading by mutableStateOf(true)
    private var failed by mutableStateOf(false)
    private var favs by mutableStateOf(ChannelRepository.favorites())
    private var hideJob: Job? = null

    // Channel list over the video (OK opens it; the channel keeps playing).
    private var allChannels: List<Channel> = emptyList()
    private var tabs: List<Pair<String, String>> = emptyList() // id to name; "__all", categories, "__favs"
    private var panelOpen by mutableStateOf(false)
    private var panelTab by mutableIntStateOf(0)
    private var panelSel by mutableIntStateOf(0)
    private var panelJob: Job? = null
    private var okLong = false
    private var okDown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val loaded = ChannelRepository.load(this)
        val all = loaded.channels
        allChannels = all
        tabs = listOf("__all" to "الكل") + loaded.categories.filter { c -> all.any { it.category == c.id } }.map { it.id to it.name } + listOf("__favs" to "المفضلة")
        numbers = all.mapIndexed { i, c -> c.id to i + 1 }.toMap()
        val ids = intent.getStringArrayExtra(EXTRA_IDS)?.toList() ?: all.map { it.id }
        channels = ids.mapNotNull { id -> all.firstOrNull { it.id == id } }
        if (channels.isEmpty()) { finish(); return }
        index = intent.getIntExtra(EXTRA_INDEX, 0).coerceIn(0, channels.size - 1)

        exo = Players.create(this)
        exo.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                tryNextAddress()
            }

            override fun onPlaybackStateChanged(state: Int) {
                loading = state == Player.STATE_BUFFERING || state == Player.STATE_IDLE
                if (state == Player.STATE_READY) failed = false
            }
        })

        setContent {
            AppTheme {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                player = exo
                                keepScreenOn = true
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    val ch = channels[index]
                    if (failed) {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("تعذّر تشغيل هذه القناة الآن", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.size(8.dp))
                            Text("جرّب قناة أخرى بالأسهم ▲ ▼ أو حاول لاحقًا", fontSize = 15.sp, color = C.dim)
                        }
                    } else if (loading) {
                        Text("جارٍ التحميل…", fontSize = 18.sp, color = C.dim, modifier = Modifier.align(Alignment.Center))
                    }

                    AnimatedVisibility(
                        visible = showInfo && !panelOpen,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Box(
                            Modifier.fillMaxWidth().background(
                                Brush.verticalGradient(listOf(Color.Transparent, Color(0xC0000000)))
                            )
                        ) {
                            Row(
                                Modifier
                                    .padding(horizontal = 48.dp, vertical = 32.dp)
                                    .fillMaxWidth()
                                    .background(Color(0x8C141822), RoundedCornerShape(18.dp))
                                    .padding(horizontal = 22.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("${numbers[ch.id] ?: ""}", fontSize = 32.sp, fontWeight = FontWeight.Light, color = C.dim)
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(ch.name, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                                            Spacer(Modifier.width(10.dp))
                                            Row(
                                                Modifier.background(C.live, RoundedCornerShape(5.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(Modifier.size(5.dp).background(Color.White, CircleShape))
                                                Spacer(Modifier.width(4.dp))
                                                Text("مباشر", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                        Text(
                                            if (ch.id in favs) "★ في المفضلة · اضغط OK مطوّلًا للإزالة"
                                            else "اضغط OK مطوّلًا للإضافة إلى المفضلة",
                                            fontSize = 13.sp, color = if (ch.id in favs) C.gold else C.dim
                                        )
                                    }
                                }
                                Text("▲ ▼ تقليب القنوات", fontSize = 13.sp, color = C.dim)
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = panelOpen,
                        enter = fadeIn() + androidx.compose.animation.slideInHorizontally { -it / 3 },
                        exit = fadeOut(),
                        // TopEnd is the left side in this right-to-left layout
                        modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight()
                    ) {
                        om.yousef.shashati.ui.ChannelPanel(
                            tabs = tabs.map { it.second },
                            tab = panelTab,
                            list = tabList(panelTab),
                            selected = panelSel,
                            playingId = channels[index].id,
                            numbers = numbers,
                        )
                    }
                }
            }
        }
        playCurrent()
    }

    private fun tabList(tab: Int): List<Channel> = when (val id = tabs.getOrNull(tab)?.first) {
        "__all", null -> allChannels
        "__favs" -> allChannels.filter { it.id in favs }
        else -> allChannels.filter { it.category == id }
    }

    private fun openPanel() {
        val cur = channels[index]
        panelTab = 0 // always opens on «الكل»
        panelSel = tabList(panelTab).indexOfFirst { it.id == cur.id }.coerceAtLeast(0)
        panelOpen = true
        hideJob?.cancel()
        showInfo = false
        touchPanel()
    }

    private fun touchPanel() {
        panelJob?.cancel()
        // The list stays until OK on the playing channel or BACK — no auto-hide.
    }

    private fun closePanel() {
        panelOpen = false
        panelJob?.cancel()
    }

    private fun playFromPanel() {
        val list = tabList(panelTab)
        val ch = list.getOrNull(panelSel) ?: return
        // OK on the channel already playing closes the list; any other channel plays and the list stays.
        if (ch.id == channels[index].id) { closePanel(); return }
        channels = list
        index = panelSel
        playCurrent()
        hideJob?.cancel()
        showInfo = false
        touchPanel()
    }

    private fun playCurrent() {
        urlIndex = 0
        failed = false
        loading = true
        Players.play(exo, channels[index], urlIndex)
        flashInfo()
    }

    private fun tryNextAddress() {
        val ch = channels[index]
        urlIndex++
        if (urlIndex < ch.urls.size) {
            Players.play(exo, ch, urlIndex)
        } else {
            failed = true
            loading = false
            showInfo = true
        }
    }

    private fun change(delta: Int) {
        index = (index + delta + channels.size) % channels.size
        playCurrent()
    }

    private fun flashInfo() {
        showInfo = true
        hideJob?.cancel()
        hideJob = lifecycleScope.launch {
            delay(5000)
            if (!failed) showInfo = false
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        val isOk = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER

        // OK: short press opens the list (or plays the chosen channel); long press = favourite.
        if (isOk) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (event.repeatCount == 0) { okDown = true; okLong = false }
                else if (event.repeatCount == 1 && !panelOpen) {
                    okLong = true
                    ChannelRepository.toggleFavorite(channels[index].id)
                    favs = ChannelRepository.favorites()
                    flashInfo()
                }
            } else if (event.action == KeyEvent.ACTION_UP && okDown) {
                okDown = false
                if (!okLong) { if (panelOpen) playFromPanel() else openPanel() }
            }
            return true
        }

        if (panelOpen) {
            if (code == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_UP) closePanel()
                return true
            }
            if (event.action != KeyEvent.ACTION_DOWN) return true
            touchPanel()
            val size = tabList(panelTab).size
            when (code) {
                KeyEvent.KEYCODE_DPAD_UP -> if (size > 0) panelSel = (panelSel - 1 + size) % size
                KeyEvent.KEYCODE_DPAD_DOWN -> if (size > 0) panelSel = (panelSel + 1) % size
                // Tabs run right-to-left: Left goes to the next tab, Right to the previous one.
                KeyEvent.KEYCODE_DPAD_LEFT -> { panelTab = (panelTab + 1) % tabs.size; panelSel = 0 }
                KeyEvent.KEYCODE_DPAD_RIGHT -> { panelTab = (panelTab - 1 + tabs.size) % tabs.size; panelSel = 0 }
                KeyEvent.KEYCODE_CHANNEL_UP -> { closePanel(); change(-1) }
                KeyEvent.KEYCODE_CHANNEL_DOWN -> { closePanel(); change(1) }
                else -> return super.dispatchKeyEvent(event)
            }
            return true
        }

        if (event.action != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event)
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP -> { if (event.repeatCount == 0) change(-1); return true }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> { if (event.repeatCount == 0) change(1); return true }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_INFO -> { flashInfo(); return true }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onStop() {
        super.onStop()
        if (::exo.isInitialized) exo.pause()
    }

    override fun onStart() {
        super.onStart()
        if (::exo.isInitialized && channels.isNotEmpty()) exo.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::exo.isInitialized) exo.release()
    }

    companion object {
        private const val EXTRA_IDS = "ids"
        private const val EXTRA_INDEX = "index"

        fun intent(context: Context, list: List<Channel>, index: Int): Intent =
            Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_IDS, list.map { it.id }.toTypedArray())
                .putExtra(EXTRA_INDEX, index)
    }
}
