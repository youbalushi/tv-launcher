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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import om.yousef.shashati.data.Prefs
import om.yousef.shashati.prayer.Dates
import om.yousef.shashati.prayer.PRAYER_NAMES
import om.yousef.shashati.prayer.PrayerRepository
import java.time.LocalDateTime
import kotlin.random.Random

private data class Star(val x: Float, val y: Float, val r: Float, val phase: Float)

/**
 * Screensaver from the approved design: night sky, crescent, a lit mosque by the water,
 * big clock and dates, and the prayer bar with the next prayer highlighted.
 * The scene is drawn in code until the final artwork arrives.
 */
@Composable
fun ScreensaverContent() {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }
    val stars = remember {
        val rnd = Random(11)
        List(110) { Star(rnd.nextFloat(), rnd.nextFloat() * 0.5f, 0.4f + rnd.nextFloat() * 1.1f, rnd.nextFloat() * 6.28f) }
    }
    val anim = rememberInfiniteTransition(label = "saver")
    val twinkle by anim.animateFloat(0f, 6.28f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "tw")
    val ripple by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse), label = "rp")

    // Move the text a little every minute so nothing stays burnt into the screen.
    val shiftX = (now.minute % 5 - 2) * 3
    val shiftY = (now.minute % 3 - 1) * 3

    val (next, today) = remember(now.withSecond(0).withNano(0)) {
        PrayerRepository.next(now) to PrayerRepository.timesFor(now.toLocalDate())
    }
    val nextToday = next.time.toLocalDate() == now.toLocalDate()

    val textShadow = androidx.compose.ui.graphics.Shadow(Color(0x99000000), Offset(0f, 2f), 12f)
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF0A1638))) {
        val H = maxHeight
        // The owner's photo of the mosque at night (time, dates and dhikr sit in the sky above it).
        androidx.compose.foundation.Image(
            androidx.compose.ui.res.painterResource(om.yousef.shashati.R.drawable.saver_mosque), null,
            Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        // a little darkening at the top keeps the white text easy to read
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x59000820), 0.45f to Color.Transparent)))

        Column(
            Modifier.fillMaxSize().offset(shiftX.dp, shiftY.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(H * 0.035f))
            Text(
                Dates.time12(now.toLocalTime()), fontSize = 92.sp, fontWeight = FontWeight.Normal, lineHeight = 96.sp,
                letterSpacing = 2.sp, style = androidx.compose.ui.text.TextStyle(shadow = textShadow)
            )
            Text(
                Dates.gregorian(now.toLocalDate()) + if (Prefs.saverHijri) "، " + Dates.hijri(now.toLocalDate()) + " هـ" else "",
                fontSize = 18.sp, color = Color(0xF2FFFFFF), style = androidx.compose.ui.text.TextStyle(shadow = textShadow)
            )
            if (Prefs.saverDhikr) {
                Text(
                    Dates.dhikr(now), fontSize = 16.sp, color = Color(0xFFFFE2A8),
                    modifier = Modifier.padding(top = 6.dp), style = androidx.compose.ui.text.TextStyle(shadow = textShadow)
                )
            }

            Spacer(Modifier.weight(1f))

            if (Prefs.saverPrayers) {
                val bar = RoundedCornerShape(20.dp)
                Row(
                    Modifier.width(560.dp).height(80.dp)
                        .background(Brush.verticalGradient(listOf(Color(0x40B4C6EE), Color(0x2A2B3F7C))), bar)
                        .border(1.dp, Color(0x40FFFFFF), bar)
                        .padding(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val nextIdx = if (nextToday) next.index else 0
                    for (i in 0 until 5) {
                        val isNext = i == nextIdx
                        if (i > 0 && i != nextIdx && i - 1 != nextIdx) {
                            Box(Modifier.width(1.dp).height(44.dp).background(Color(0x33FFFFFF)))
                        }
                        if (isNext) {
                            val cell = RoundedCornerShape(16.dp)
                            Row(
                                Modifier.weight(1.25f).fillMaxHeight()
                                    .outerGlow(cell, Color(0xFFFFB84A))
                                    .background(Brush.verticalGradient(listOf(Color(0x66C9A15A), Color(0x55634A2A))), cell)
                                    .border(1.5.dp, Brush.linearGradient(listOf(Color(0xFFFFE2A8), Color(0xFFFFB84A))), cell),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(PRAYER_NAMES[i], fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    Text(Dates.time12(today[i]), fontSize = 23.sp, fontWeight = FontWeight.Medium, color = Color(0xFFFFD27A))
                                }
                                Spacer(Modifier.width(12.dp))
                                LineGlyph(Glyph.Dome, Modifier.size(30.dp), Color(0xFFFFD27A))
                            }
                        } else {
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(PRAYER_NAMES[i], fontSize = 14.sp, color = Color(0xE6FFFFFF))
                                Text(Dates.time12(today[i]), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // the prayer bar sits on the mosque's marble floor
            Spacer(Modifier.height(H * 0.05f))
        }
    }
}

/** Night sky, crescent, mountains, a lit mosque with palms, and its reflection on the water. */
private fun DrawScope.drawNightScene(stars: List<Star>, twinkle: Float, ripple: Float) {
    val w = size.width
    val h = size.height
    val horizon = h * 0.68f

    // sky
    drawRect(Brush.verticalGradient(listOf(Color(0xFF0B1840), Color(0xFF16296A), Color(0xFF2E3C86), Color(0xFF5A5294), Color(0xFF7A6496)), 0f, horizon))
    drawRect(Brush.radialGradient(listOf(Color(0x55FFB27A), Color.Transparent), Offset(w * 0.86f, horizon), w * 0.3f), Offset(0f, 0f), Size(w, horizon))
    stars.forEach { s ->
        val a = 0.3f + 0.7f * ((kotlin.math.sin(twinkle + s.phase) + 1f) / 2f)
        drawCircle(Color.White.copy(alpha = a), radius = s.r * density, center = Offset(s.x * w, s.y * h))
    }
    // a few soft clouds on the left
    withTransform({ scale(1f, 0.18f, Offset(w * 0.18f, h * 0.45f)) }) {
        drawCircle(Brush.radialGradient(listOf(Color(0x557D84C0), Color.Transparent), Offset(w * 0.18f, h * 0.45f), w * 0.2f), w * 0.2f, Offset(w * 0.18f, h * 0.45f))
    }
    withTransform({ scale(1f, 0.12f, Offset(w * 0.3f, h * 0.53f)) }) {
        drawCircle(Brush.radialGradient(listOf(Color(0x447D84C0), Color.Transparent), Offset(w * 0.3f, h * 0.53f), w * 0.18f), w * 0.18f, Offset(w * 0.3f, h * 0.53f))
    }

    // crescent moon with glow
    val mc = Offset(w * 0.175f, h * 0.2f)
    val mr = w * 0.05f
    drawCircle(Brush.radialGradient(listOf(Color(0x40FFE9C4), Color.Transparent), mc, mr * 2.6f), mr * 2.6f, mc)
    val disc = Path().apply { addOval(androidx.compose.ui.geometry.Rect(mc.x - mr, mc.y - mr, mc.x + mr, mc.y + mr)) }
    val cut = Path().apply { addOval(androidx.compose.ui.geometry.Rect(mc.x - mr * 0.62f, mc.y - mr * 1.18f, mc.x + mr * 1.3f, mc.y + mr * 0.7f)) }
    val moon = Path().apply { op(disc, cut, androidx.compose.ui.graphics.PathOperation.Difference) }
    drawPath(moon, Brush.linearGradient(listOf(Color(0xFFFFF4DE), Color(0xFFF2D9AE)), Offset(mc.x - mr, mc.y), Offset(mc.x + mr, mc.y + mr)))

    // mountains on the left
    val far = Path().apply {
        moveTo(0f, horizon - h * 0.05f)
        cubicTo(w * 0.08f, horizon - h * 0.09f, w * 0.18f, horizon - h * 0.1f, w * 0.26f, horizon - h * 0.065f)
        cubicTo(w * 0.33f, horizon - h * 0.04f, w * 0.38f, horizon - h * 0.085f, w * 0.46f, horizon - h * 0.06f)
        cubicTo(w * 0.52f, horizon - h * 0.04f, w * 0.56f, horizon - h * 0.02f, w * 0.6f, horizon)
        lineTo(0f, horizon); close()
    }
    drawPath(far, Brush.verticalGradient(listOf(Color(0xFF3B4686), Color(0xFF26306A)), horizon - h * 0.1f, horizon))
    // city lights on the far shore
    val rnd = Random(3)
    repeat(60) {
        val x = rnd.nextFloat() * w * 0.32f
        drawCircle(Color(0xFFFFD9A0).copy(alpha = 0.5f + rnd.nextFloat() * 0.5f), 0.8f * density, Offset(x, horizon - rnd.nextFloat() * h * 0.006f))
    }

    // water
    drawRect(Brush.verticalGradient(listOf(Color(0xFF34407E), Color(0xFF1C275A), Color(0xFF0E1838)), horizon, h), Offset(0f, horizon), Size(w, h - horizon))

    // mosque (right side)
    drawMosque(w, h, horizon)

    // reflections of the lit mosque on the water: short broken gold strokes that shimmer
    val gold = Color(0xFFFFC77A)
    val rr = Random(5)
    repeat(140) { k ->
        val x = w * (0.64f + rr.nextFloat() * 0.36f)
        val depth = rr.nextFloat()
        val y = horizon + h * 0.01f + depth * depth * h * 0.26f
        val len = w * (0.004f + rr.nextFloat() * 0.018f) * (1f - depth * 0.6f)
        val a = (0.08f + 0.32f * ((kotlin.math.sin(ripple * 6.28f + k * 0.7f) + 1f) / 2f)) * (1f - depth * 0.8f)
        drawRect(gold.copy(alpha = a), Offset(x - len / 2, y), Size(len, h * 0.0025f))
    }
    drawRect(Brush.verticalGradient(listOf(Color(0x40FFC77A), Color.Transparent), horizon, horizon + h * 0.18f), Offset(w * 0.64f, horizon), Size(w * 0.36f, h * 0.18f))
    // left shore lights reflection
    repeat(40) {
        val x = rr.nextFloat() * w * 0.32f
        drawRect(gold.copy(alpha = 0.12f + rr.nextFloat() * 0.15f), Offset(x, horizon + h * (0.004f + rr.nextFloat() * 0.03f)), Size(w * 0.006f, h * 0.002f))
    }
    // soft vignette
    drawRect(Brush.verticalGradient(listOf(Color(0x33000000), Color.Transparent, Color.Transparent, Color(0x55000000))))
}

private fun DrawScope.drawMosque(w: Float, h: Float, horizon: Float) {
    val stone = Brush.verticalGradient(listOf(Color(0xFFE9D9C4), Color(0xFFB9A48E)), h * 0.3f, horizon)
    val shade = Color(0xFF8E7C6E)
    val lit = Color(0xFFFFCF84)
    fun X(f: Float) = w * f
    fun Y(f: Float) = h * f

    // glow behind the building
    drawCircle(Brush.radialGradient(listOf(Color(0x66FFC27A), Color.Transparent), Offset(X(0.84f), Y(0.6f)), X(0.2f)), X(0.2f), Offset(X(0.84f), Y(0.6f)))

    fun dome(cx: Float, base: Float, r: Float, tall: Float) {
        val p = Path().apply {
            moveTo(X(cx - r), Y(base))
            cubicTo(X(cx - r), Y(base - tall * 0.7f), X(cx - r * 0.25f), Y(base - tall * 0.85f), X(cx), Y(base - tall))
            cubicTo(X(cx + r * 0.25f), Y(base - tall * 0.85f), X(cx + r), Y(base - tall * 0.7f), X(cx + r), Y(base))
            close()
        }
        drawPath(p, Brush.horizontalGradient(listOf(Color(0xFFCDB89F), Color(0xFFFFF0D8), Color(0xFFB8A189)), X(cx - r), X(cx + r)))
        drawLine(Color(0xFFE8C88E), Offset(X(cx), Y(base - tall)), Offset(X(cx), Y(base - tall - 0.035f)), X(0.0016f))
        drawCircle(Color(0xFFE8C88E), X(0.0028f), Offset(X(cx), Y(base - tall - 0.038f)))
    }
    fun minaret(cx: Float, top: Float, bw: Float) {
        drawRect(stone, Offset(X(cx - bw / 2), Y(top + 0.06f)), Size(X(bw), horizon - Y(top + 0.06f)))
        // balconies
        listOf(0.11f, 0.2f).forEach { dy ->
            drawRect(Color(0xFFD8C4AA), Offset(X(cx - bw * 0.85f), Y(top + dy)), Size(X(bw * 1.7f), Y(0.012f)))
            drawRect(lit.copy(alpha = 0.9f), Offset(X(cx - bw * 0.25f), Y(top + dy - 0.03f)), Size(X(bw * 0.5f), Y(0.022f)))
        }
        drawPath(Path().apply {
            moveTo(X(cx - bw * 0.55f), Y(top + 0.06f)); lineTo(X(cx), Y(top)); lineTo(X(cx + bw * 0.55f), Y(top + 0.06f)); close()
        }, Color(0xFFCDB89F))
        drawLine(Color(0xFFE8C88E), Offset(X(cx), Y(top)), Offset(X(cx), Y(top - 0.03f)), X(0.0014f))
    }

    // minarets
    minaret(0.745f, 0.27f, 0.014f)
    minaret(0.905f, 0.24f, 0.015f)
    // side wings
    drawRect(stone, Offset(X(0.66f), Y(0.6f)), Size(X(0.34f), horizon - Y(0.6f)))
    dome(0.7f, 0.6f, 0.022f, 0.06f)
    dome(0.955f, 0.6f, 0.022f, 0.06f)
    // main hall and big dome
    drawRect(stone, Offset(X(0.775f), Y(0.53f)), Size(X(0.11f), horizon - Y(0.53f)))
    dome(0.83f, 0.53f, 0.05f, 0.13f)
    dome(0.785f, 0.56f, 0.014f, 0.04f)
    dome(0.875f, 0.56f, 0.014f, 0.04f)
    // lit arches
    fun arch(cx: Float, base: Float, aw: Float, ah: Float) {
        val p = Path().apply {
            moveTo(X(cx - aw / 2), Y(base))
            lineTo(X(cx - aw / 2), Y(base - ah * 0.6f))
            cubicTo(X(cx - aw / 2), Y(base - ah), X(cx), Y(base - ah * 1.05f), X(cx), Y(base - ah * 1.08f))
            cubicTo(X(cx), Y(base - ah * 1.05f), X(cx + aw / 2), Y(base - ah), X(cx + aw / 2), Y(base - ah * 0.6f))
            lineTo(X(cx + aw / 2), Y(base))
            close()
        }
        drawPath(p, Brush.verticalGradient(listOf(Color(0xFFFFE3A6), lit, Color(0xFFE79A45)), Y(base - ah), Y(base)))
    }
    listOf(0.795f, 0.83f, 0.865f).forEach { arch(it, horizon - Y(0.012f), 0.022f, 0.085f) }
    listOf(0.68f, 0.705f, 0.73f, 0.93f, 0.955f, 0.98f).forEach { arch(it, horizon - Y(0.012f), 0.014f, 0.05f) }
    // terrace edge
    drawRect(Color(0xFFCFB89C), Offset(X(0.62f), horizon - Y(0.012f)), Size(X(0.38f), Y(0.012f)))
    drawRect(shade.copy(alpha = 0.5f), Offset(X(0.62f), horizon), Size(X(0.38f), Y(0.004f)))

    // palms
    fun palm(x: Float, top: Float, s: Float) {
        val dark = Color(0xFF0F1A33)
        drawLine(dark, Offset(X(x), horizon), Offset(X(x + 0.004f), Y(top)), X(0.004f * s))
        for (k in 0 until 7) {
            val a = Math.toRadians(-160.0 + k * 23.0)
            val len = X(0.035f * s)
            val end = Offset(X(x + 0.004f) + (len * Math.cos(a)).toFloat(), Y(top) + (len * 0.55f * Math.sin(a)).toFloat() + len * 0.25f)
            val p = Path().apply {
                moveTo(X(x + 0.004f), Y(top))
                quadraticTo((X(x + 0.004f) + end.x) / 2, Y(top) - len * 0.18f, end.x, end.y)
            }
            drawPath(p, dark, style = androidx.compose.ui.graphics.drawscope.Stroke(X(0.003f * s)))
        }
    }
    palm(0.6f, 0.55f, 1.1f); palm(0.635f, 0.58f, 0.9f); palm(0.66f, 0.53f, 1f)
    palm(0.985f, 0.5f, 1.1f); palm(0.965f, 0.56f, 0.8f)
}
