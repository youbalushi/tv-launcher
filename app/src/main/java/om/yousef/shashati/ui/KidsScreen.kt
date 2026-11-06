package om.yousef.shashati.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import om.yousef.shashati.data.AppsRepository
import om.yousef.shashati.data.Channel
import om.yousef.shashati.data.ChannelRepository
import om.yousef.shashati.data.Prefs

private val kidColors = listOf(
    listOf(Color(0xFFFF6B6B), Color(0xFFC2255C)),
    listOf(Color(0xFF51CF66), Color(0xFF2B8A3E)),
    listOf(Color(0xFFFFA94D), Color(0xFFE8590C)),
    listOf(Color(0xFF9775FA), Color(0xFF5F3DC4)),
    listOf(Color(0xFF38D9A9), Color(0xFF087F5B)),
    listOf(Color(0xFFFFD43B), Color(0xFFF08C00)),
)

/** Kids world: only the apps the parent allowed, plus kids channels. Leaving needs the PIN. */
@Composable
fun KidsScreen(onPlayChannels: (List<Channel>) -> Unit, onExit: () -> Unit) {
    val context = LocalContext.current
    var askingPin by remember { mutableStateOf(false) }
    val allowed = remember { Prefs.kidsApps() }
    val apps = remember { AppsRepository.installed(context).filter { it.packageName in allowed } }
    val kidsChannels = remember { ChannelRepository.load(context).channels.filter { it.category == "kids" } }

    BackHandler { askingPin = !askingPin }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF121A48), Color(0xFF0B1030))))
            .drawBehind {
                val w = size.width; val h = size.height
                drawRect(Brush.radialGradient(listOf(Color(0x66FFAA46), Color.Transparent), Offset(w * 0.85f, h * 0.1f), w * 0.3f))
                drawRect(Brush.radialGradient(listOf(Color(0x595AC8FF), Color.Transparent), Offset(w * 0.12f, h * 0.25f), w * 0.28f))
                drawRect(Brush.radialGradient(listOf(Color(0x73A06EFF), Color.Transparent), Offset(w * 0.55f, h * 1.1f), w * 0.36f))
            }
    ) {
        if (askingPin) {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                PinGate("الخروج من عالم الأطفال") {
                    Prefs.kidsMode = false
                    askingPin = false
                    onExit()
                }
            }
            return@Box
        }

        val first = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            delay(150)
            runCatching { first.requestFocus() }
        }

        Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 26.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("عالم الأطفال", fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
                Focusable(
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(50),
                    background = Color(0x1FFFFFFF),
                    onClick = { askingPin = true }
                ) {
                    Text("خروج (بالرقم السري)", fontSize = 13.sp, modifier = Modifier.align(Alignment.Center).padding(horizontal = 18.dp))
                }
            }

            Text("للمشاهدة", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Focusable(
                    modifier = Modifier.size(240.dp, 110.dp),
                    shape = RoundedCornerShape(22.dp),
                    focusRequester = first,
                    background = Color(0xFF1C5FBF),
                    onClick = { if (kidsChannels.isNotEmpty()) onPlayChannels(kidsChannels) }
                ) {
                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF4DABF7), Color(0xFF1C5FBF)))))
                    Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                        Text("قنوات الأطفال", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (kidsChannels.isEmpty()) "لا توجد قنوات بعد" else "${kidsChannels.size} قناة", fontSize = 12.sp, color = Color(0xD9FFFFFF))
                    }
                }
            }

            Text("التطبيقات والألعاب", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            if (apps.isEmpty()) {
                Text(
                    "لم يُسمح بأي تطبيق بعد. يختار أحد الوالدين التطبيقات من الإعدادات ← الأطفال وقفل التطبيقات.",
                    fontSize = 14.sp, color = C.dim
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp), contentPadding = PaddingValues(6.dp)) {
                    items(apps.size) { i ->
                        val app = apps[i]
                        val banner = remember(app.packageName) {
                            app.banner?.let { runCatching { it.toBitmap(320, 180).asImageBitmap() }.getOrNull() }
                        }
                        Focusable(
                            modifier = Modifier.size(200.dp, 112.dp),
                            shape = RoundedCornerShape(22.dp),
                            onClick = { AppsRepository.launch(context, app.packageName) }
                        ) {
                            if (banner != null) {
                                Image(banner, app.label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Box(Modifier.fillMaxSize().background(Brush.linearGradient(kidColors[i % kidColors.size])))
                                Text(
                                    app.label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
