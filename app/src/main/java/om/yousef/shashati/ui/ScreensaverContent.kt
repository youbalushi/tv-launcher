package om.yousef.shashati.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import om.yousef.shashati.data.Locations
import om.yousef.shashati.data.Prefs
import om.yousef.shashati.prayer.Dates
import om.yousef.shashati.prayer.PRAYER_NAMES
import om.yousef.shashati.prayer.PrayerRepository
import java.time.Duration
import java.time.LocalDateTime
import kotlin.random.Random

private data class Star(val x: Float, val y: Float, val r: Float, val phase: Float)

/** The Islamic screensaver: mosque silhouette, night or sunset sky, clock, dates, dhikr and prayer times. */
@Composable
fun ScreensaverContent() {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }
    val night = Prefs.saverNight
    val stars = remember {
        val rnd = Random(7)
        List(70) { Star(rnd.nextFloat(), rnd.nextFloat() * 0.55f, 0.6f + rnd.nextFloat() * 1.2f, rnd.nextFloat() * 6.28f) }
    }
    val anim = rememberInfiniteTransition(label = "saver")
    val twinkle by anim.animateFloat(0f, 6.28f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "tw")
    val drift by anim.animateFloat(-1f, 1f, infiniteRepeatable(tween(40000), RepeatMode.Reverse), label = "drift")

    // Move the text block slightly every minute so nothing stays burnt into the screen.
    val shift = remember(now.minute) { Offset((now.minute % 5 - 2) * 6f, (now.minute % 3 - 1) * 6f) }

    val (next, today) = remember(now.withSecond(0).withNano(0)) {
        PrayerRepository.next(now) to PrayerRepository.timesFor(now.toLocalDate())
    }

    Box(
        Modifier.fillMaxSize().background(
            if (night) Brush.verticalGradient(listOf(Color(0xFF050913), Color(0xFF0C1830), Color(0xFF1A2C4C)))
            else Brush.verticalGradient(listOf(Color(0xFF1C1433), Color(0xFF5A2F4A), Color(0xFFC4683A), Color(0xFFEDA45A)))
        )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val glowColor = if (night) Color(0x405A8CDC) else Color(0x73FFC878)
            drawRect(Brush.radialGradient(listOf(glowColor, Color.Transparent), Offset(w * (0.5f + drift * 0.04f), h * 0.86f), w * 0.36f))
            if (night) {
                stars.forEach { s ->
                    val a = 0.25f + 0.75f * ((kotlin.math.sin(twinkle + s.phase) + 1f) / 2f)
                    drawCircle(Color.White.copy(alpha = a), radius = s.r * density, center = Offset(s.x * w, s.y * h))
                }
            }
            drawMosque(night)
        }

        Column(
            Modifier.fillMaxSize().padding(top = 44.dp).offset(shift.x.dp, shift.y.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(Locations.current().name, fontSize = 15.sp, color = C.dim)
            Text(Dates.time12(now.toLocalTime()), fontSize = 104.sp, fontWeight = FontWeight.ExtraLight, lineHeight = 108.sp)
            Text(
                Dates.gregorian(now.toLocalDate()) + if (Prefs.saverHijri) "  ·  " + Dates.hijri(now.toLocalDate()) else "",
                fontSize = 17.sp, color = Color(0xD9FFFFFF)
            )
            if (Prefs.saverDhikr) {
                Text(
                    Dates.dhikr(now.toLocalDate()), fontFamily = Amiri, fontSize = 26.sp, lineHeight = 40.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp).widthIn(max = 720.dp)
                )
            }
            if (Prefs.saverPrayers) {
                Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    for (i in 0 until 5) {
                        val isNext = next.index == i && next.time.toLocalDate() == now.toLocalDate()
                        Column(
                            Modifier
                                .background(if (isNext) C.goldSoft else Color(0x14FFFFFF), RoundedCornerShape(11.dp))
                                .border(if (isNext) 1.5.dp else 1.dp, if (isNext) C.gold else Color(0x24FFFFFF), RoundedCornerShape(11.dp))
                                .padding(horizontal = 15.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                if (isNext) "${PRAYER_NAMES[i]} · ${remainingText(Duration.between(now, next.time).toMinutes() + 1)}" else PRAYER_NAMES[i],
                                fontSize = 12.sp, color = if (isNext) C.gold else C.dim,
                                fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(Dates.time12(today[i]), fontSize = 17.sp, fontWeight = if (isNext) FontWeight.Medium else FontWeight.Light)
                        }
                    }
                }
            }
        }
    }
}

/** Mosque silhouette along the bottom of the screen (an original drawing). */
private fun DrawScope.drawMosque(night: Boolean) {
    val w = size.width
    val h = size.height
    val sx = w / 1920f
    val base = h - 420f * sx
    fun p(x: Float, y: Float) = Offset(x * sx, base + y * sx)

    val hill = Path().apply {
        moveTo(p(0f, 330f).x, p(0f, 330f).y)
        quadraticBezierTo(p(300f, 270f).x, p(300f, 270f).y, p(620f, 315f).x, p(620f, 315f).y)
        quadraticBezierTo(p(940f, 360f).x, p(940f, 360f).y, p(1240f, 300f).x, p(1240f, 300f).y)
        quadraticBezierTo(p(1580f, 250f).x, p(1580f, 250f).y, p(1920f, 320f).x, p(1920f, 320f).y)
        lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(hill, if (night) Color(0xFF070C18) else Color(0xFF2A1626))

    val c = if (night) Color(0xFF03060D) else Color(0xFF1A0D18)
    fun rect(x: Float, y: Float, rw: Float, rh: Float) = drawRect(c, p(x, y), Size(rw * sx, rh * sx))
    fun dome(x1: Float, x2: Float, baseY: Float, topY: Float) {
        val mid = (x1 + x2) / 2
        val path = Path().apply {
            moveTo(p(x1, baseY).x, p(x1, baseY).y)
            quadraticBezierTo(p(x1, topY + (baseY - topY) * 0.35f).x, p(x1, topY + (baseY - topY) * 0.35f).y, p(mid, topY).x, p(mid, topY).y)
            quadraticBezierTo(p(x2, topY + (baseY - topY) * 0.35f).x, p(x2, topY + (baseY - topY) * 0.35f).y, p(x2, baseY).x, p(x2, baseY).y)
            close()
        }
        drawPath(path, c)
    }
    fun cone(x: Float, w2: Float, baseY: Float, topY: Float) {
        val path = Path().apply {
            moveTo(p(x - w2, baseY).x, p(x - w2, baseY).y)
            lineTo(p(x, topY).x, p(x, topY).y)
            lineTo(p(x + w2, baseY).x, p(x + w2, baseY).y)
            close()
        }
        drawPath(path, c)
    }
    // Minarets
    rect(560f, 120f, 34f, 300f); rect(552f, 190f, 50f, 12f); cone(577f, 21f, 120f, 58f)
    rect(1326f, 120f, 34f, 300f); rect(1318f, 190f, 50f, 12f); cone(1343f, 21f, 120f, 58f)
    // Side halls and domes
    rect(640f, 285f, 170f, 135f); dome(655f, 795f, 285f, 212f)
    rect(1110f, 285f, 170f, 135f); dome(1125f, 1265f, 285f, 212f)
    // Main hall and dome
    rect(780f, 250f, 360f, 170f); dome(800f, 1120f, 250f, 96f)
    rect(957f, 58f, 6f, 40f)
    drawCircle(c, 9f * sx, p(960f, 52f))

    // Warm window light
    val light = if (night) Color(0x59E2B45A) else Color(0x59FFC878)
    fun arch(x: Float, y: Float, aw: Float, ah: Float) {
        val path = Path().apply {
            moveTo(p(x, y + ah).x, p(x, y + ah).y)
            lineTo(p(x, y + aw / 2).x, p(x, y + aw / 2).y)
            quadraticBezierTo(p(x + aw / 2, y - aw / 3).x, p(x + aw / 2, y - aw / 3).y, p(x + aw, y + aw / 2).x, p(x + aw, y + aw / 2).y)
            lineTo(p(x + aw, y + ah).x, p(x + aw, y + ah).y)
            close()
        }
        drawPath(path, light)
    }
    arch(930f, 335f, 60f, 85f)
    arch(850f, 318f, 36f, 62f)
    arch(1034f, 318f, 36f, 62f)
}
