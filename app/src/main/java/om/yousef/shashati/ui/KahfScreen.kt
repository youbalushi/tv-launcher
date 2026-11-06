package om.yousef.shashati.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import om.yousef.shashati.data.KahfRepository
import om.yousef.shashati.data.Reciter
import java.io.File

private val cardColors = listOf(
    listOf(Color(0xFF1F4E5F), Color(0xFF0E2530)),
    listOf(Color(0xFF4A3A6E), Color(0xFF1E1730)),
    listOf(Color(0xFF2F5F3F), Color(0xFF12261A)),
    listOf(Color(0xFF6E4A24), Color(0xFF2A1C0E)),
    listOf(Color(0xFF2A4A7A), Color(0xFF0E1A30)),
    listOf(Color(0xFF6E2A3F), Color(0xFF2A0E18)),
)

@Composable
fun KahfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var playing by remember { mutableStateOf<Reciter?>(null) }
    var downloading by remember { mutableStateOf<Reciter?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }

    val current = playing
    if (current != null) {
        AudioPlayerScreen(
            title = "سورة الكهف",
            subtitle = "بصوت القارئ ${current.name}",
            file = KahfRepository.file(context, current),
            onClose = { playing = null }
        )
        return
    }

    BackHandler { if (downloading == null) onBack() }

    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { first.requestFocus() }
    }

    Box(Modifier.fillMaxSize()) {
        AppBackground()
        Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 26.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Focusable(modifier = Modifier.size(34.dp), shape = CircleShape, onClick = onBack) {
                    Text("›", fontSize = 22.sp, modifier = Modifier.align(Alignment.Center))
                }
                Column {
                    Text("سورة الكهف", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                    Text("اختر القارئ · تُحمَّل التلاوة أول مرة فقط ثم تعمل بدون إنترنت", fontSize = 12.sp, color = C.dim)
                }
            }

            val d = downloading
            if (d != null) {
                Column(
                    Modifier.fillMaxWidth().background(Color(0x14FFFFFF), RoundedCornerShape(14.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("جارٍ تحميل تلاوة ${d.name}… ${(progress * 100).toInt()}%", fontSize = 15.sp)
                    Box(Modifier.fillMaxWidth().height(5.dp).background(Color(0x24FFFFFF), RoundedCornerShape(50))) {
                        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(5.dp).background(C.gold, RoundedCornerShape(50)))
                    }
                }
            }
            error?.let { Text(it, fontSize = 13.sp, color = Color(0xFFFFB3B3)) }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(KahfRepository.reciters.size) { i ->
                    val r = KahfRepository.reciters[i]
                    val ready = remember(refresh) { KahfRepository.isDownloaded(context, r) }
                    val colors = cardColors[i % cardColors.size]
                    Focusable(
                        modifier = Modifier.fillMaxWidth().height(86.dp),
                        focusRequester = if (i == 0) first else null,
                        scaleOnFocus = 1.04f,
                        onClick = {
                            error = null
                            if (downloading != null) {
                                // already downloading another reciter
                            } else if (ready) {
                                playing = r
                            } else {
                                downloading = r
                                progress = 0f
                                scope.launch {
                                    val ok = KahfRepository.download(context, r) { p -> progress = p }
                                    downloading = null
                                    refresh++
                                    if (ok) playing = r else error = "تعذّر التحميل. تأكد من اتصال التلفزيون بالإنترنت وحاول مرة أخرى."
                                }
                            }
                        }
                    ) {
                        Box(Modifier.fillMaxSize().background(Brush.linearGradient(colors)))
                        Row(
                            Modifier.fillMaxSize().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                Modifier.size(52.dp).background(Color(0x26FFFFFF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(r.name.take(1), fontFamily = Amiri, fontSize = 26.sp, color = C.gold)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(r.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (ready) "${r.style} · محفوظة ✓" else "${r.style} · تحتاج تحميل",
                                    fontSize = 11.sp, color = if (ready) C.gold else C.dim
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return String.format(java.util.Locale.US, "%d:%02d", s / 60, s % 60)
}

/** Full-screen audio player with back/forward 30 s, pause/resume and stop. */
@Composable
fun AudioPlayerScreen(title: String, subtitle: String, file: File, onClose: () -> Unit) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            prepare()
            playWhenReady = true
        }
    }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(true) }
    var ended by remember { mutableStateOf(false) }

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(p: Boolean) { isPlaying = p }
            override fun onPlaybackStateChanged(state: Int) { ended = state == Player.STATE_ENDED }
        }
        player.addListener(listener)
        onDispose {
            view.keepScreenOn = false
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            position = player.currentPosition
            duration = player.duration.coerceAtLeast(0)
            delay(500)
        }
    }
    BackHandler { onClose() }

    val playFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { playFocus.requestFocus() }
    }

    Box(Modifier.fillMaxSize()) {
        AppBackground()
        Column(
            Modifier.align(Alignment.Center).width(620.dp)
                .background(Color(0x14FFFFFF), RoundedCornerShape(26.dp))
                .padding(horizontal = 36.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(title, fontFamily = Amiri, fontSize = 38.sp, color = C.gold)
            Text(subtitle, fontSize = 16.sp, color = C.dim)

            val frac = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.fillMaxWidth().height(6.dp).background(Color(0x24FFFFFF), RoundedCornerShape(50))) {
                    Box(Modifier.fillMaxWidth(frac).height(6.dp).background(C.gold, RoundedCornerShape(50)))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatMs(position), fontSize = 13.sp, color = C.dim)
                    Text(formatMs(duration), fontSize = 13.sp, color = C.dim)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                ControlButton("تأخير 30 ث") { player.seekTo((player.currentPosition - 30_000).coerceAtLeast(0)) }
                ControlButton(
                    if (ended) "إعادة" else if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                    primary = true, requester = playFocus
                ) {
                    if (ended) { player.seekTo(0); player.play() }
                    else if (player.isPlaying) player.pause() else player.play()
                }
                ControlButton("تقديم 30 ث") { player.seekTo((player.currentPosition + 30_000).coerceAtMost(player.duration.coerceAtLeast(0))) }
                ControlButton("إيقاف", danger = true) { player.stop(); onClose() }
            }
        }
    }
}

@Composable
private fun ControlButton(
    text: String,
    primary: Boolean = false,
    danger: Boolean = false,
    requester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    Focusable(
        modifier = Modifier.height(48.dp).width(if (primary) 150.dp else 112.dp),
        shape = RoundedCornerShape(50),
        focusRequester = requester,
        background = when {
            primary -> C.gold
            danger -> C.danger
            else -> Color(0x1FFFFFFF)
        },
        scaleOnFocus = 1.08f,
        onClick = onClick
    ) {
        Text(
            text, fontSize = 14.sp, fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal,
            color = if (primary) C.dark else if (danger) Color(0xFFFFB3B3) else Color.White,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
