package om.yousef.shashati.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import om.yousef.shashati.data.AppsRepository
import om.yousef.shashati.data.Channel
import om.yousef.shashati.data.HomeTiles
import om.yousef.shashati.data.InstalledApp
import om.yousef.shashati.data.Prefs
import om.yousef.shashati.data.WeatherRepository
import om.yousef.shashati.prayer.Dates
import om.yousef.shashati.prayer.PRAYER_NAMES
import om.yousef.shashati.prayer.PrayerRepository
import java.time.Duration
import java.time.LocalDateTime

private val FocusGold = Color(0xFFFFB84A)
private val TileShape = RoundedCornerShape(15.dp)

/**
 * A card that shows the gold focus glow only while the remote is on it (approved design).
 */
@Composable
fun GlowTile(
    modifier: Modifier = Modifier,
    shape: Shape = TileShape,
    background: Brush,
    focusRequester: FocusRequester? = null,
    showBorder: Boolean = true,
    onClick: () -> Unit,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.045f else 1f, label = "tile")
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .then(
                if (focused) Modifier.shadow(20.dp, shape, ambientColor = FocusGold, spotColor = FocusGold)
                else Modifier
            )
            .clip(shape)
            .background(background, shape)
            .border(
                if (focused) 2.5.dp else 1.dp,
                if (focused) Brush.linearGradient(listOf(Color(0xFFFFE2A8), FocusGold, Color(0xFFFFE2A8)))
                else if (showBorder) Brush.linearGradient(listOf(Color(0x55FFFFFF), Color(0x1AFFFFFF))) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                shape
            )
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // glassy highlight
        Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(Color(0x24FFFFFF), Color.Transparent), Offset.Zero, Offset(260f, 260f))))
        content(focused)
    }
}

private val glassBrush = Brush.linearGradient(listOf(Color(0x30FFFFFF), Color(0x10FFFFFF)))

@Composable
fun HomeScreen(
    version: Int,
    onOpenChannels: () -> Unit,
    onOpenSettings: (section: Int) -> Unit,
    onLaunchApp: (String) -> Unit,
    onOpenAllApps: () -> Unit = {},
    onPlayChannel: (List<Channel>, Int) -> Unit = { _, _ -> },
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
    var weather by remember { mutableStateOf(WeatherRepository.cached()) }
    LaunchedEffect(version) {
        while (true) {
            WeatherRepository.refreshIfNeeded()
            weather = WeatherRepository.cached()
            delay(10 * 60_000L)
        }
    }

    val channels = remember(version) { HomeTiles.channels(context) }
    val allChannels = remember(version) { om.yousef.shashati.data.ChannelRepository.load(context).channels }
    val apps = remember(version) { HomeTiles.apps(context) }

    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { firstFocus.requestFocus() }
    }
    // Back on the home screen returns the focus to its default place.
    BackHandler { runCatching { firstFocus.requestFocus() } }

    val minuteKey = now.withSecond(0).withNano(0)
    val prayer = remember(minuteKey, version) {
        Triple(PrayerRepository.next(now), PrayerRepository.previous(now), PrayerRepository.timesFor(now.toLocalDate()))
    }
    val (next, prev, today) = prayer

    // Friday 7:00–18:00: Surah Al-Kahf. Saturday–Thursday: morning adhkar 6–10, evening adhkar 18–20.
    val hour = now.hour
    val friday = now.dayOfWeek == java.time.DayOfWeek.FRIDAY
    val hasMorning = remember(version) { om.yousef.shashati.data.AdhkarAudio.file(context, true) != null }
    val hasEvening = remember(version) { om.yousef.shashati.data.AdhkarAudio.file(context, false) != null }
    val special: String? = when {
        friday && hour in 7..17 -> "kahf"
        !friday && hour in 6..9 && hasMorning -> "morning"
        !friday && hour in 18..19 && hasEvening -> "evening"
        else -> null
    }?.takeIf { !Prefs.playedToday(it, now.toLocalDate()) } // once played today, the button goes away

    Box(Modifier.fillMaxSize()) {
        HomeBackground()

        Column(Modifier.fillMaxSize().padding(start = 38.dp, end = 38.dp, top = 14.dp, bottom = 30.dp)) {
            // ---------- Line 1: greeting (right) … dhikr, weather, Wi-Fi, settings (left) ----------
            Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(2.5.dp).height(16.dp).background(C.gold, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(9.dp))
                Text(Dates.greeting(now.toLocalTime()), fontSize = 15.sp, color = Color(0xF2FFFFFF))
                Spacer(Modifier.weight(1f))
                Text(
                    Dates.dhikr(now.toLocalDate()), fontSize = 15.sp, fontWeight = FontWeight.Medium,
                    color = Color(0xFFFFE2A8), maxLines = 1,
                )
                Spacer(Modifier.width(14.dp))
                // weather pill
                GlowTile(
                    Modifier.height(36.dp), shape = RoundedCornerShape(50), background = glassBrush,
                    onClick = { onOpenSettings(2) }
                ) {
                    Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        LineGlyph(Glyph.Pin, Modifier.size(13.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(weather?.place ?: om.yousef.shashati.data.Locations.current().name, fontSize = 13.sp)
                        Box(Modifier.padding(horizontal = 10.dp).width(1.dp).height(18.dp).background(Color(0x44FFFFFF)))
                        Text(weather?.let { "${it.temp}°" } ?: "--°", fontSize = 19.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(6.dp))
                        WeatherGlyph(weather?.code ?: 2, weather?.isDay ?: (hour in 6..17), Modifier.size(28.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                GlowTile(Modifier.size(36.dp), shape = CircleShape, background = glassBrush, onClick = { openWifiSettings(context) }) {
                    NetworkIndicator(Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                GlowTile(Modifier.size(36.dp), shape = CircleShape, background = glassBrush, onClick = { onOpenSettings(0) }) {
                    GearGlyph(Modifier.size(19.dp))
                }
            }

            // ---------- Line 2: clock (right) and the two dates (left), side by side ----------
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Dates.time12(now.toLocalTime()), fontSize = 64.sp, fontWeight = FontWeight.Medium,
                    lineHeight = 68.sp, letterSpacing = 1.sp,
                )
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DateLine(Glyph.Calendar, Dates.gregorianFull(now.toLocalDate()))
                    DateLine(Glyph.Moon, Dates.hijri(now.toLocalDate()) + " هـ")
                }
            }
            Spacer(Modifier.height(18.dp))

            // ---------- Channels ----------
            SectionHead(Glyph.Tv, "القنوات")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().height(68.dp), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                channels.forEachIndexed { i, ch ->
                    ChannelTile(ch, version, Modifier.weight(1f).fillMaxHeight(), if (i == 2) firstFocus else null) {
                        val group = allChannels.filter { it.category == ch.category }
                        onPlayChannel(group, group.indexOfFirst { it.id == ch.id }.coerceAtLeast(0))
                    }
                }
                repeat(HomeTiles.SLOTS - channels.size) { Spacer(Modifier.weight(1f)) }
                AllTile(Glyph.Tv, "جميع القنوات", Modifier.weight(1f).fillMaxHeight(), onOpenChannels)
            }

            Spacer(Modifier.height(16.dp))
            // ---------- Apps ----------
            SectionHead(Glyph.Grid, "التطبيقات")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().height(64.dp), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                apps.forEach { app ->
                    AppTile(app, version, Modifier.weight(1f).fillMaxHeight()) { onLaunchApp(app.packageName) }
                }
                repeat(HomeTiles.SLOTS - apps.size) { Spacer(Modifier.weight(1f)) }
                AllTile(Glyph.Grid, "جميع التطبيقات", Modifier.weight(1f).fillMaxHeight(), onOpenAllApps)
            }

            Spacer(Modifier.weight(1f))

            // ---------- Prayer strip ----------
            if (Prefs.showPrayerBar) {
                PrayerStrip(now, next, prev, today, special, onOpenSettings, onOpenKahf, onOpenAdhkar)
            }
        }
    }
}

@Composable
private fun DateLine(g: Glyph, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LineGlyph(g, Modifier.size(15.dp), Color(0xEEFFFFFF))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, color = Color(0xEEFFFFFF))
    }
}

@Composable
private fun SectionHead(g: Glyph, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LineGlyph(g, Modifier.size(19.dp), C.gold)
        Spacer(Modifier.width(9.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun Long.toColor() = Color(this.toInt() or 0xFF000000.toInt())

private fun lighter(c: Color, f: Float) = Color(c.red + (1 - c.red) * f, c.green + (1 - c.green) * f, c.blue + (1 - c.blue) * f, 1f)

@Composable
private fun ChannelTile(ch: Channel, version: Int, modifier: Modifier, requester: FocusRequester?, onClick: () -> Unit) {
    val context = LocalContext.current
    var logo by remember(ch.id, version) { mutableStateOf(om.yousef.shashati.data.Logos.cachedChannel(context, ch)) }
    LaunchedEffect(ch.id) { if (logo == null) logo = om.yousef.shashati.data.Logos.channel(context, ch) }
    GlowTile(modifier, focusRequester = requester, background = glassBrush, onClick = onClick) {
        LogoOrName(logo, ch.name)
    }
}

/** The logo in its own colours, centred on the glass; the name in white if there is no logo. */
@Composable
private fun BoxScope.LogoOrName(logo: ImageBitmap?, name: String) {
    if (logo != null) {
        Image(
            logo, name,
            Modifier.align(Alignment.Center).fillMaxWidth(0.74f).fillMaxHeight(0.68f),
            contentScale = ContentScale.Fit
        )
    } else {
        Text(
            name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 10.dp)
        )
    }
}

@Composable
private fun AllTile(g: Glyph, title: String, modifier: Modifier, onClick: () -> Unit) {
    GlowTile(modifier, background = glassBrush, onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LineGlyph(g, Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun AppTile(app: InstalledApp, version: Int, modifier: Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val logo = remember(app.packageName, version) { om.yousef.shashati.data.Logos.app(context, app) }
    GlowTile(modifier, background = glassBrush, onClick = onClick) {
        LogoOrName(logo, app.label)
    }
}

@Composable
private fun PrayerStrip(
    now: LocalDateTime,
    next: om.yousef.shashati.prayer.PrayerEvent,
    prev: om.yousef.shashati.prayer.PrayerEvent,
    today: List<java.time.LocalTime>,
    special: String?,
    onOpenSettings: (Int) -> Unit,
    onOpenKahf: () -> Unit,
    onOpenAdhkar: (Boolean) -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().height(62.dp)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0x2421417A), Color(0x3DA9C2EA))), shape)
            // the gold edge glows on the right, next to "الصلاة القادمة"
            .border(1.dp, Brush.horizontalGradient(listOf(Color(0x33FFFFFF), Color(0x44FFFFFF), Color(0x99FFC46B))), shape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Next prayer (right end)
        Row(Modifier.width(150.dp).padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            LineGlyph(Glyph.Dome, Modifier.size(30.dp), C.gold)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("الصلاة القادمة", fontSize = 11.sp, color = C.gold)
                Text(PRAYER_NAMES[next.index], fontSize = 19.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
            }
        }
        Box(Modifier.width(1.dp).height(36.dp).background(Color(0x33FFFFFF)))

        // Countdown, or the Kahf / adhkar button when it is time
        Box(Modifier.width(190.dp).fillMaxHeight().padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            if (special != null) {
                GlowTile(
                    Modifier.fillMaxWidth().height(38.dp), shape = RoundedCornerShape(50),
                    background = Brush.horizontalGradient(listOf(Color(0x55FFC46B), Color(0x33FFC46B))),
                    onClick = {
                        Prefs.markPlayed(special, now.toLocalDate())
                        when (special) { "kahf" -> onOpenKahf(); "morning" -> onOpenAdhkar(true); else -> onOpenAdhkar(false) }
                    }
                ) {
                    Text(
                        when (special) { "kahf" -> "▶  سورة الكهف"; "morning" -> "▶  أذكار الصباح"; else -> "▶  أذكار المساء" },
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFDFA0)
                    )
                }
            } else if (Prefs.showNextCard) {
                val total = Duration.between(prev.time, next.time).toMinutes().coerceAtLeast(1)
                val left = Duration.between(now, next.time).toMinutes() + 1
                val progress = (1f - left.toFloat() / total).coerceIn(0f, 1f)
                Column(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LineGlyph(Glyph.Clock, Modifier.size(12.dp), C.gold)
                        Spacer(Modifier.width(5.dp))
                        Text("بعد", fontSize = 11.sp, color = Color(0xDDFFFFFF))
                    }
                    Text(Dates.duration(left), fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0x26FFFFFF), RoundedCornerShape(50))) {
                        Box(
                            Modifier.fillMaxWidth(progress).height(3.dp)
                                .background(Brush.horizontalGradient(listOf(Color(0xFFFFD27A), Color(0xFFFFB13B))), RoundedCornerShape(50))
                        )
                    }
                }
            }
        }
        Box(Modifier.width(1.dp).height(36.dp).background(Color(0x33FFFFFF)))

        // The five prayers (Fajr on the right)
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            for (i in 0 until 5) {
                if (i > 0) Box(Modifier.width(1.dp).height(30.dp).background(Color(0x26FFFFFF)))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(PRAYER_NAMES[i], fontSize = 12.sp, color = Color(0xDDFFFFFF))
                    Text(Dates.time12(today[i]), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Dusk mosque picture (left end)
        MosqueThumb(Modifier.width(130.dp).fillMaxHeight())
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


/** Opens the TV's Wi-Fi network list (falls back to the general network / settings screens). */
private fun openWifiSettings(context: android.content.Context) {
    val actions = listOf(
        android.provider.Settings.ACTION_WIFI_SETTINGS,
        "android.settings.NETWORK_SETTINGS",
        android.provider.Settings.ACTION_WIRELESS_SETTINGS,
        android.provider.Settings.ACTION_SETTINGS,
    )
    for (a in actions) {
        val i = android.content.Intent(a).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        if (i.resolveActivity(context.packageManager) != null && runCatching { context.startActivity(i) }.isSuccess) return
    }
}
