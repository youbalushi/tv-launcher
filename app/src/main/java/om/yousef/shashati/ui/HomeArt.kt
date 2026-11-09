package om.yousef.shashati.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Home background from the approved design: deep navy with a glowing sunset wave across the top.
 * (Drawn in code until the final artwork arrives.)
 */
@Composable
fun HomeBackground(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                val w = size.width
                val h = size.height
                drawRect(Brush.linearGradient(listOf(Color(0xFF0A1B3F), Color(0xFF123274), Color(0xFF0A1E4A), Color(0xFF071430)), Offset(0f, 0f), Offset(w, h)))
                drawRect(Brush.radialGradient(listOf(Color(0x701E56C8), Color.Transparent), Offset(w * 0.6f, h * 0.42f), w * 0.5f))
                // warm sunset glow behind the wave
                withTransform({ scale(1f, 0.42f, Offset(w * 0.3f, h * 0.2f)) }) {
                    drawCircle(Brush.radialGradient(listOf(Color(0xCCFFB07A), Color(0x88F07A6A), Color(0x3B8A4FC8), Color.Transparent), Offset(w * 0.3f, h * 0.2f), w * 0.28f), w * 0.28f, Offset(w * 0.3f, h * 0.2f))
                }
                // the wave: a soft filled band plus thin bright lines
                fun wave(dy: Float, amp: Float): Path = Path().apply {
                    moveTo(0f, h * (0.31f + dy))
                    cubicTo(w * 0.16f, h * (0.30f + dy), w * 0.26f, h * (0.12f + dy - amp), w * 0.42f, h * (0.12f + dy))
                    cubicTo(w * 0.55f, h * (0.12f + dy + amp), w * 0.62f, h * (0.24f + dy), w * 0.74f, h * (0.17f + dy))
                    cubicTo(w * 0.84f, h * (0.11f + dy), w * 0.92f, h * (0.06f + dy), w, h * (0.05f + dy))
                }
                val band = wave(0f, 0f).apply { lineTo(w, h * 0.32f); cubicTo(w * 0.7f, h * 0.36f, w * 0.3f, h * 0.38f, 0f, h * 0.4f); close() }
                drawPath(band, Brush.verticalGradient(listOf(Color(0x3326418A), Color(0x14000000), Color.Transparent), 0f, h * 0.4f))
                val glow = Brush.horizontalGradient(
                    listOf(Color.Transparent, Color(0xFFFFB07A), Color(0xFFFF8C7A), Color(0xFFB98AE6), Color(0xFF7FB2FF), Color(0x887FB2FF), Color.Transparent),
                    0f, w
                )
                drawPath(wave(0f, 0f), glow, alpha = 0.25f, style = Stroke(w * 0.012f, cap = StrokeCap.Round))
                drawPath(wave(0f, 0f), glow, alpha = 0.95f, style = Stroke(w * 0.0016f, cap = StrokeCap.Round))
                drawPath(wave(0.03f, 0.01f), glow, alpha = 0.45f, style = Stroke(w * 0.001f))
                drawPath(wave(0.075f, 0.02f), glow, alpha = 0.25f, style = Stroke(w * 0.0009f))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0x66050F26)), h * 0.55f, h))
            }
    )
}

private fun DrawScope.st(f: Float = 0.085f) = Stroke(width = size.width * f, cap = StrokeCap.Round, join = StrokeJoin.Round)

/** Small line icons used on the home screen. */
enum class Glyph { Tv, Grid, Calendar, Moon, Clock, Pin, Cast, Play, Photo, LiveTv, Dome }

@Composable
fun LineGlyph(g: Glyph, modifier: Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        val s = size.width
        fun p(vararg pts: Pair<Float, Float>, close: Boolean = false, fill: Boolean = false) {
            val path = Path()
            pts.forEachIndexed { i, (x, y) -> if (i == 0) path.moveTo(x * s, y * s) else path.lineTo(x * s, y * s) }
            if (close) path.close()
            if (fill) drawPath(path, color) else drawPath(path, color, style = st())
        }
        when (g) {
            Glyph.Tv -> {
                drawRoundRect(color, Offset(s * 0.08f, s * 0.28f), Size(s * 0.84f, s * 0.6f), CornerRadius(s * 0.1f), style = st())
                p(0.34f to 0.08f, 0.5f to 0.25f, 0.66f to 0.08f)
            }
            Glyph.Grid -> listOf(0.08f to 0.08f, 0.56f to 0.08f, 0.08f to 0.56f, 0.56f to 0.56f).forEach { (x, y) ->
                drawRoundRect(color, Offset(x * s, y * s), Size(s * 0.36f, s * 0.36f), CornerRadius(s * 0.08f), style = st())
            }
            Glyph.Calendar -> {
                drawRoundRect(color, Offset(s * 0.1f, s * 0.18f), Size(s * 0.8f, s * 0.72f), CornerRadius(s * 0.1f), style = st(0.075f))
                p(0.1f to 0.4f, 0.9f to 0.4f); p(0.32f to 0.08f, 0.32f to 0.26f); p(0.68f to 0.08f, 0.68f to 0.26f)
            }
            Glyph.Moon -> {
                val path = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(s * 0.1f, s * 0.1f, s * 0.9f, s * 0.9f))
                }
                drawPath(path, color, style = st(0.075f))
                drawCircle(Color(0xFF0E2554), s * 0.36f, Offset(s * 0.68f, s * 0.36f))
                drawCircle(color, s * 0.05f, Offset(s * 0.78f, s * 0.2f))
            }
            Glyph.Clock -> {
                drawCircle(color, s * 0.4f, Offset(s / 2, s / 2), style = st())
                p(0.5f to 0.27f, 0.5f to 0.5f, 0.66f to 0.6f)
            }
            Glyph.Pin -> {
                val path = Path().apply {
                    moveTo(s * 0.5f, s * 0.95f)
                    cubicTo(s * 0.1f, s * 0.55f, s * 0.15f, s * 0.08f, s * 0.5f, s * 0.08f)
                    cubicTo(s * 0.85f, s * 0.08f, s * 0.9f, s * 0.55f, s * 0.5f, s * 0.95f)
                }
                drawPath(path, color)
                drawCircle(Color(0xFF16305F), s * 0.13f, Offset(s * 0.5f, s * 0.38f))
            }
            Glyph.Cast -> {
                for ((i, r) in listOf(0.42f, 0.28f).withIndex()) {
                    drawArc(color, 215f, 110f, false, Offset(s * (0.5f - r), s * (0.62f - r)), Size(s * r * 2, s * r * 2), style = st(0.1f))
                    if (i == 1) Unit
                }
                drawCircle(color, s * 0.08f, Offset(s * 0.5f, s * 0.6f))
                drawRect(color, Offset(s * 0.2f, s * 0.8f), Size(s * 0.6f, s * 0.1f))
            }
            Glyph.Play -> {
                drawRoundRect(color.copy(alpha = 0.35f), Offset(s * 0.02f, s * 0.2f), Size(s * 0.7f, s * 0.62f), CornerRadius(s * 0.12f))
                drawRoundRect(color, Offset(s * 0.2f, s * 0.12f), Size(s * 0.75f, s * 0.68f), CornerRadius(s * 0.12f))
                p(0.48f to 0.3f, 0.75f to 0.46f, 0.48f to 0.62f, close = true, fill = true)
            }
            Glyph.Photo -> {
                drawCircle(color, s * 0.13f, Offset(s * 0.5f, s * 0.28f))
                p(0.05f to 0.85f, 0.32f to 0.5f, 0.5f to 0.7f, 0.65f to 0.55f, 0.95f to 0.85f, close = true, fill = true)
            }
            Glyph.LiveTv -> {
                drawRoundRect(color, Offset(s * 0.06f, s * 0.25f), Size(s * 0.88f, s * 0.62f), CornerRadius(s * 0.14f), style = st(0.1f))
                p(0.32f to 0.06f, 0.5f to 0.22f, 0.68f to 0.06f)
                drawRoundRect(color, Offset(s * 0.2f, s * 0.38f), Size(s * 0.42f, s * 0.36f), CornerRadius(s * 0.06f))
                drawCircle(color, s * 0.05f, Offset(s * 0.78f, s * 0.45f)); drawCircle(color, s * 0.05f, Offset(s * 0.78f, s * 0.62f))
            }
            Glyph.Dome -> {
                // a small mosque: dome, crescent and base
                val dome = Path().apply {
                    moveTo(s * 0.18f, s * 0.62f)
                    cubicTo(s * 0.18f, s * 0.36f, s * 0.38f, s * 0.3f, s * 0.5f, s * 0.22f)
                    cubicTo(s * 0.62f, s * 0.3f, s * 0.82f, s * 0.36f, s * 0.82f, s * 0.62f)
                    close()
                }
                drawPath(dome, color)
                drawLine(color, Offset(s * 0.5f, s * 0.22f), Offset(s * 0.5f, s * 0.08f), s * 0.05f)
                drawCircle(color, s * 0.045f, Offset(s * 0.5f, s * 0.06f))
                drawRect(color, Offset(s * 0.1f, s * 0.66f), Size(s * 0.8f, s * 0.26f))
                drawRoundRect(Color(0xFF3A2A10), Offset(s * 0.42f, s * 0.72f), Size(s * 0.16f, s * 0.2f), CornerRadius(s * 0.08f))
            }
        }
    }
}

/** Weather icon by WMO code: sun, moon, clouds, rain or storm. */
@Composable
fun WeatherGlyph(code: Int, day: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        val s = size.width
        val sun = Color(0xFFFFC94A)
        val cloud = Color(0xFFEFF3FF)
        val clear = code == 0
        val partly = code in 1..2
        if (clear || partly) {
            val c = if (partly) Offset(s * 0.62f, s * 0.36f) else Offset(s / 2, s / 2)
            val r = if (partly) s * 0.2f else s * 0.26f
            if (day) {
                drawCircle(sun, r, c)
                for (k in 0 until 8) {
                    val a = Math.toRadians(k * 45.0)
                    drawLine(sun, Offset(c.x + (r * 1.35f * Math.cos(a)).toFloat(), c.y + (r * 1.35f * Math.sin(a)).toFloat()),
                        Offset(c.x + (r * 1.75f * Math.cos(a)).toFloat(), c.y + (r * 1.75f * Math.sin(a)).toFloat()), s * 0.05f, StrokeCap.Round)
                }
            } else {
                drawCircle(Color(0xFFF2E3C2), r, c)
                drawCircle(Color(0xFF1A3366), r * 0.85f, Offset(c.x + r * 0.5f, c.y - r * 0.35f))
            }
        }
        if (!clear) {
            val y = if (partly) s * 0.5f else s * 0.42f
            val path = Path().apply {
                moveTo(s * 0.18f, y + s * 0.3f)
                cubicTo(s * 0.02f, y + s * 0.3f, s * 0.04f, y + s * 0.06f, s * 0.22f, y + s * 0.08f)
                cubicTo(s * 0.26f, y - s * 0.12f, s * 0.56f, y - s * 0.14f, s * 0.62f, y + s * 0.04f)
                cubicTo(s * 0.86f, y + s * 0.0f, s * 0.92f, y + s * 0.3f, s * 0.74f, y + s * 0.3f)
                close()
            }
            drawPath(path, Brush.verticalGradient(listOf(Color.White, Color(0xFFC9D6EE)), y - s * 0.1f, y + s * 0.3f))
            if (code in 51..67 || code in 80..82 || code >= 95) {
                listOf(0.3f, 0.48f, 0.66f).forEach { x ->
                    drawLine(Color(0xFF8CC8FF), Offset(s * x, y + s * 0.38f), Offset(s * (x - 0.06f), y + s * 0.5f), s * 0.05f, StrokeCap.Round)
                }
            }
            if (code >= 95) {
                drawPath(Path().apply {
                    moveTo(s * 0.52f, y + s * 0.32f); lineTo(s * 0.42f, y + s * 0.48f); lineTo(s * 0.52f, y + s * 0.48f); lineTo(s * 0.44f, y + s * 0.64f)
                }, sun, style = Stroke(s * 0.05f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            if (code == 45 || code == 48) {
                listOf(0.2f, 0.35f).forEach { dy -> drawLine(cloud, Offset(s * 0.15f, y + s * (0.3f + dy)), Offset(s * 0.85f, y + s * (0.3f + dy)), s * 0.05f, StrokeCap.Round) }
            }
        }
    }
}

/** The little dusk mosque scene at the end of the prayer strip (fades into the glass). */
@Composable
fun MosqueThumb(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFF3B3A7A), Color(0xFF9B5C8A), Color(0xFFF0A070)), 0f, h))
        drawCircle(Brush.radialGradient(listOf(Color(0xAAFFD39A), Color.Transparent), Offset(w * 0.55f, h * 0.85f), w * 0.5f), w * 0.5f, Offset(w * 0.55f, h * 0.85f))
        val dark = Color(0xFF1B1C3D)
        // minaret
        drawRect(dark, Offset(w * 0.17f, h * 0.18f), Size(w * 0.07f, h * 0.82f))
        drawPath(Path().apply { moveTo(w * 0.165f, h * 0.18f); lineTo(w * 0.205f, h * 0.02f); lineTo(w * 0.245f, h * 0.18f); close() }, dark)
        drawRect(dark, Offset(w * 0.15f, h * 0.38f), Size(w * 0.11f, h * 0.03f))
        // main dome
        drawPath(Path().apply {
            moveTo(w * 0.36f, h * 0.68f)
            cubicTo(w * 0.36f, h * 0.42f, w * 0.48f, h * 0.38f, w * 0.53f, h * 0.3f)
            cubicTo(w * 0.58f, h * 0.38f, w * 0.7f, h * 0.42f, w * 0.7f, h * 0.68f)
            close()
        }, dark)
        drawLine(dark, Offset(w * 0.53f, h * 0.3f), Offset(w * 0.53f, h * 0.2f), w * 0.012f)
        // side domes and base
        drawCircle(dark, w * 0.07f, Offset(w * 0.31f, h * 0.74f))
        drawCircle(dark, w * 0.07f, Offset(w * 0.76f, h * 0.74f))
        drawRect(dark, Offset(w * 0.12f, h * 0.72f), Size(w * 0.82f, h * 0.28f))
        // lit windows
        val lit = Color(0xFFFFD58A)
        listOf(0.3f, 0.42f, 0.53f, 0.64f, 0.76f).forEach { x ->
            drawRoundRect(lit, Offset(w * x - w * 0.015f, h * 0.8f), Size(w * 0.03f, h * 0.1f), CornerRadius(w * 0.015f))
        }
        drawRect(lit.copy(alpha = 0.8f), Offset(w * 0.19f, h * 0.45f), Size(w * 0.025f, h * 0.06f))
        // fade towards the strip
        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent, Color(0xF0182C5C)), 0f, w))
    }
}
