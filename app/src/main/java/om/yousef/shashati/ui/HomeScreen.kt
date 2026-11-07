package om.yousef.shashati.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import om.yousef.shashati.data.AppsRepository
import om.yousef.shashati.data.InstalledApp
import om.yousef.shashati.data.Locations
import om.yousef.shashati.data.Prefs
import om.yousef.shashati.prayer.Dates
import om.yousef.shashati.prayer.PRAYER_NAMES
import om.yousef.shashati.prayer.PrayerRepository
import java.time.Duration
import java.time.LocalDateTime

private class AppTile(val app: InstalledApp, val banner: ImageBitmap?, val icon: ImageBitmap?)

private fun InstalledApp.tile(): AppTile = AppTile(
    this,
    banner?.let { runCatching { it.toBitmap(320, 180).asImageBitmap() }.getOrNull() },
    icon?.let { runCatching { it.toBitmap(96, 96).asImageBitmap() }.getOrNull() },
)

@Composable
fun HomeScreen(
    version: Int,
    onOpenChannels: () -> Unit,
    onOpenSettings: (section: Int) -> Unit,
    onLaunchApp: (String) -> Unit,
    onOpenKahf: () -> Unit = {},
    onOpenAdhkar: (morning: Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }

    val tiles = remember(version) { AppsRepository.installed(context).map { it.tile() } }
    val favorites = remember(version) { AppsRepository.favorites(context) }
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { firstFocus.requestFocus() }
    }

    // Prayer data (re-read every minute or when data changes)
    val minuteKey = now.withSecond(0).withNano(0)
    val prayer = remember(minuteKey, version) {
        val next = PrayerRepository.next(now)
        val prev = PrayerRepository.previous(now)
        Triple(next, prev, PrayerRepository.timesFor(now.toLocalDate()))
    }
    val (next, prev, today) = prayer

    // Friday 7:00–18:00: Surah Al-Kahf. Saturday–Thursday: morning adhkar 6–10, evening adhkar 18–20.
    val hour = now.hour
    val friday = now.dayOfWeek == java.time.DayOfWeek.FRIDAY
    val special: String? = when {
        friday && hour in 7..17 -> "kahf"
        // Morning/evening adhkar are paused until a later update.
        else -> null
    }

    // Everything is sized to fit one TV screen (about 960 × 540 dp) without scrolling.
    Box(Modifier.fillMaxSize()) {
        AppBackground()

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 44.dp, end = 44.dp, top = 18.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Hero: clock, dates and dhikr on one side; settings + next prayer on the other
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text(Dates.greeting(now.toLocalTime()), fontSize = 13.sp, color = C.dim)
                    Text(Dates.time12(now.toLocalTime()), fontSize = 60.sp, fontWeight = FontWeight.Light, lineHeight = 64.sp)
                    Text(
                        Dates.gregorian(now.toLocalDate()) + " · " + Dates.hijri(now.toLocalDate()),
                        fontSize = 14.sp, color = Color(0xD1FFFFFF)
                    )
                    Text(Dates.dhikr(now.toLocalDate()), fontSize = 13.sp, lineHeight = 22.sp, color = C.faint, modifier = Modifier.padding(top = 2.dp))
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NetworkIndicator()
                        Focusable(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            background = Color(0x1AFFFFFF),
                            scaleOnFocus = 1.12f,
                            onClick = { onOpenSettings(0) }
                        ) {
                            GearGlyph(Modifier.align(Alignment.Center).size(18.dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                        if (Prefs.showNextCard) {
                            val total = Duration.between(prev.time, next.time).toMinutes().coerceAtLeast(1)
                            val left = Duration.between(now, next.time).toMinutes() + 1
                            val progress = (1f - left.toFloat() / total).coerceIn(0f, 1f)
                            Column(
                                Modifier
                                    .width(230.dp)
                                    .background(Color(0x17FFFFFF), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(6.dp).background(C.gold, CircleShape))
                                    Spacer(Modifier.width(6.dp))
                                    Text("الصلاة القادمة", fontSize = 11.sp, color = C.dim)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                    Text(PRAYER_NAMES[next.index], fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                                    Text(Dates.time12(next.time.toLocalTime()), fontSize = 18.sp, fontWeight = FontWeight.Light)
                                }
                                Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0x24FFFFFF), RoundedCornerShape(50))) {
                                    Box(Modifier.fillMaxWidth(progress).height(3.dp).background(C.gold, RoundedCornerShape(50)))
                                }
                                Text(remainingText(left), fontSize = 12.sp, color = C.gold, fontWeight = FontWeight.Medium)
                            }
                        }
                        if (special != null) {
                            Focusable(
                                modifier = Modifier.height(44.dp),
                                shape = RoundedCornerShape(50),
                                background = C.goldSoft,
                                scaleOnFocus = 1.08f,
                                onClick = {
                                    when (special) {
                                        "kahf" -> onOpenKahf()
                                        "morning" -> onOpenAdhkar(true)
                                        else -> onOpenAdhkar(false)
                                    }
                                }
                            ) {
                                Text(
                                    when (special) {
                                        "kahf" -> "▶ تشغيل سورة الكهف"
                                        "morning" -> "▶ تشغيل أذكار الصباح"
                                        else -> "▶ تشغيل أذكار المساء"
                                    },
                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.gold,
                                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Favorites
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SectionTitle("المفضلة")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 6.dp)
                ) {
                    itemsIndexed(favorites, key = { _, it -> it }) { i, fav ->
                        val requester = if (i == 0) firstFocus else null
                        if (fav == AppsRepository.CHANNELS) {
                            Focusable(
                                modifier = Modifier.size(148.dp, 83.dp),
                                focusRequester = requester,
                                background = Color(0xFF1B3358),
                                onClick = onOpenChannels
                            ) {
                                Box(
                                    Modifier.fillMaxSize().background(
                                        Brush.linearGradient(listOf(Color(0xFF1F3A63), Color(0xFF0D1A30)))
                                    )
                                )
                                TvGlyph(Modifier.align(Alignment.TopStart).padding(10.dp).size(20.dp))
                                Text(
                                    "القنوات", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
                                )
                            }
                        } else {
                            val tile = tiles.firstOrNull { it.app.packageName == fav }
                            if (tile != null) AppCard(tile, requester, 148, 83) { onLaunchApp(tile.app.packageName) }
                        }
                    }
                    item(key = "add") {
                        Focusable(
                            modifier = Modifier.size(148.dp, 83.dp),
                            background = Color(0x0DFFFFFF),
                            onClick = { onOpenSettings(1) }
                        ) {
                            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("+", fontSize = 22.sp, fontWeight = FontWeight.Light)
                                Text("إضافة تطبيق", fontSize = 11.sp, color = C.dim)
                            }
                        }
                    }
                }
            }

            // Prayer times bar
            if (Prefs.showPrayerBar) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionTitle("أوقات الصلاة · ${Locations.current().name}")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (i in 0 until 5) {
                            val isNext = next.index == i && next.time.toLocalDate() == now.toLocalDate()
                            Row(
                                Modifier
                                    .weight(1f)
                                    .background(if (isNext) C.goldSoft else Color(0x12FFFFFF), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(PRAYER_NAMES[i], fontSize = 13.sp, fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Normal)
                                Text(
                                    Dates.time12(today[i]), fontSize = 15.sp,
                                    fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Light,
                                    color = if (isNext) C.gold else Color.White
                                )
                            }
                        }
                    }
                }
            }

            // All apps
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SectionTitle("جميع التطبيقات")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 6.dp)
                ) {
                    items(tiles, key = { "all-" + it.app.packageName }) { tile ->
                        AppCard(tile, null, width = 120, height = 68) { onLaunchApp(tile.app.packageName) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppCard(tile: AppTile, requester: FocusRequester?, width: Int = 170, height: Int = 96, onClick: () -> Unit) {
    Focusable(
        modifier = Modifier.size(width.dp, height.dp),
        focusRequester = requester,
        background = Color(0x17FFFFFF),
        onClick = onClick
    ) {
        if (tile.banner != null) {
            Image(tile.banner, contentDescription = tile.app.label, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Column(
                Modifier.align(Alignment.Center).padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tile.icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(36.dp)) }
                Text(tile.app.label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun TvGlyph(modifier: Modifier) {
    Canvas(modifier) {
        val s = size.width
        val stroke = Stroke(width = s * 0.07f, cap = StrokeCap.Round)
        drawRoundRect(
            Color.White, topLeft = Offset(s * 0.1f, s * 0.25f),
            size = androidx.compose.ui.geometry.Size(s * 0.8f, s * 0.52f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.08f), style = stroke
        )
        drawLine(Color.White, Offset(s * 0.36f, s * 0.08f), Offset(s * 0.5f, s * 0.22f), s * 0.07f, StrokeCap.Round)
        drawLine(Color.White, Offset(s * 0.64f, s * 0.08f), Offset(s * 0.5f, s * 0.22f), s * 0.07f, StrokeCap.Round)
        drawLine(Color.White, Offset(s * 0.32f, s * 0.9f), Offset(s * 0.68f, s * 0.9f), s * 0.07f, StrokeCap.Round)
    }
}

@Composable
private fun GearGlyph(modifier: Modifier) {
    Canvas(modifier) {
        val s = size.width
        val c = Offset(s / 2, s / 2)
        val stroke = Stroke(width = s * 0.1f, cap = StrokeCap.Round)
        drawCircle(Color.White, radius = s * 0.16f, center = c, style = stroke)
        drawCircle(Color.White, radius = s * 0.32f, center = c, style = stroke)
        for (k in 0 until 8) {
            val a = Math.toRadians(k * 45.0)
            val inner = Offset(c.x + (s * 0.32f * Math.cos(a)).toFloat(), c.y + (s * 0.32f * Math.sin(a)).toFloat())
            val outer = Offset(c.x + (s * 0.46f * Math.cos(a)).toFloat(), c.y + (s * 0.46f * Math.sin(a)).toFloat())
            drawLine(Color.White, inner, outer, s * 0.12f, StrokeCap.Round)
        }
    }
}
