package om.yousef.shashati.ui

import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import om.yousef.shashati.data.Channel
import om.yousef.shashati.data.ChannelRepository

private const val ALL = "__all"
private const val FAVS = "__favs"

@Composable
fun ChannelsScreen(version: Int, onBack: () -> Unit, onPlay: (List<Channel>, Int) -> Unit) {
    val context = LocalContext.current
    val list = remember(version) { ChannelRepository.load(context) }
    var tab by remember { mutableStateOf(ALL) }
    var favs by remember { mutableStateOf(ChannelRepository.favorites()) }
    val tabs = remember(list) { listOf(ALL to "الكل") + list.categories.map { it.id to it.name } + listOf(FAVS to "المفضلة") }
    val shown = remember(tab, list, favs) {
        when (tab) {
            ALL -> list.channels
            FAVS -> list.channels.filter { it.id in favs }
            else -> list.channels.filter { it.category == tab }
        }
    }
    val numberOf = remember(list) { list.channels.mapIndexed { i, c -> c.id to i + 1 }.toMap() }

    // One muted preview player for the selected card only (TVs cannot decode many streams at once).
    val player = remember { Players.create(context).apply { volume = 0f } }
    var focused by remember { mutableStateOf<Channel?>(null) }
    var previewReady by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf(true) }
    DisposableEffect(Unit) { onDispose { player.release() } }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_STOP) { player.stop(); active = false }
            if (e == Lifecycle.Event.ON_START) active = true
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(focused, active) {
        previewReady = false
        player.stop()
        val ch = focused ?: return@LaunchedEffect
        if (!active) return@LaunchedEffect
        delay(900)
        Players.play(player, ch, 0)
        previewReady = true
    }

    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { firstFocus.requestFocus() }
    }

    Box(Modifier.fillMaxSize()) {
        AppBackground()
        Column(
            Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Focusable(modifier = Modifier.size(34.dp), shape = CircleShape, onClick = onBack) {
                        Text("›", fontSize = 22.sp, modifier = Modifier.align(Alignment.Center))
                    }
                    Text("القنوات", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(4.dp)) {
                    items(tabs, key = { it.first }) { (id, name) ->
                        Pill(name, selected = tab == id, fontSize = 14, hPad = 16.dp, onClick = { tab = id })
                    }
                }
            }

            if (shown.isEmpty()) {
                Text(
                    if (tab == FAVS) "لا توجد قنوات مفضلة بعد. أثناء المشاهدة اضغط OK مطوّلًا لإضافة القناة."
                    else "لا توجد قنوات في هذا القسم",
                    fontSize = 16.sp, color = C.dim
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(shown, key = { _, c -> c.id }) { i, ch ->
                    Focusable(
                        modifier = Modifier.fillMaxWidth().height(84.dp),
                        focusRequester = if (i == 0) firstFocus else null,
                        scaleOnFocus = 1.04f,
                        background = C.glass,
                        focusedBackground = Color(0x29FFFFFF),
                        onFocus = { focused = ch },
                        onClick = {
                            player.stop()
                            onPlay(shown, i)
                        }
                    ) { isFocused ->
                        Row(
                            Modifier.fillMaxSize().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                Modifier
                                    .width(120.dp)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(Color(0x1FFFFFFF))
                            ) {
                                var logo by remember(ch.id) { mutableStateOf(om.yousef.shashati.data.Logos.cachedChannel(context, ch)) }
                                LaunchedEffect(ch.id) { if (logo == null) logo = om.yousef.shashati.data.Logos.channel(context, ch) }
                                if (isFocused && previewReady && focused == ch) {
                                    AndroidView(
                                        factory = { ctx -> TextureView(ctx).also { player.setVideoTextureView(it) } },
                                        onRelease = { player.clearVideoTextureView(it) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (logo != null) {
                                    androidx.compose.foundation.Image(
                                        logo!!, ch.name,
                                        Modifier.align(Alignment.Center).fillMaxWidth(0.78f).fillMaxHeight(0.6f),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                    )
                                } else {
                                    Text(
                                        ch.name.take(1), fontSize = 26.sp, fontWeight = FontWeight.SemiBold,
                                        color = Color(0x99FFFFFF), modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                                Row(
                                    Modifier.align(Alignment.TopStart).padding(5.dp)
                                        .background(Color(0x80000000), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(4.dp).background(C.live, CircleShape))
                                    Spacer(Modifier.width(3.dp))
                                    Text("مباشر", fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("${numberOf[ch.id] ?: ""}", fontSize = 11.sp, color = C.faint)
                                Text(
                                    ch.name, fontSize = 16.sp,
                                    fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Medium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                if (ch.id in favs) Text("★ مفضلة", fontSize = 10.sp, color = C.gold)
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Text("الأسهم للتنقل", fontSize = 12.sp, color = C.faint)
                Text("OK للمشاهدة بملء الشاشة", fontSize = 12.sp, color = C.faint)
                Text("Back للرجوع", fontSize = 12.sp, color = C.faint)
            }
        }
    }

    // Keep favourites fresh when returning from the player.
    LaunchedEffect(active) { if (active) favs = ChannelRepository.favorites() }
}
