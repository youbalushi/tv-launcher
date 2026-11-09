package om.yousef.shashati.remote.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Simple line icons drawn in code, so the app needs no icon library. */
enum class Ic { Up, Down, Left, Right, Home, Back, Power, PlayPause, VolUp, VolDown, Mute, Keyboard, Rewind, Forward, Menu }

@Composable
fun LineIcon(icon: Ic, modifier: Modifier = Modifier, size: Dp = 24.dp, color: Color = Color.White) {
    Canvas(modifier.size(size)) {
        val s = this.size.width
        val st = Stroke(width = s * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(vararg pts: Pair<Float, Float>, close: Boolean = false) {
            val path = Path()
            pts.forEachIndexed { i, (x, y) -> if (i == 0) path.moveTo(x * s, y * s) else path.lineTo(x * s, y * s) }
            if (close) path.close()
            drawPath(path, color, style = st)
        }
        when (icon) {
            Ic.Up -> p(0.25f to 0.62f, 0.5f to 0.37f, 0.75f to 0.62f)
            Ic.Down -> p(0.25f to 0.38f, 0.5f to 0.63f, 0.75f to 0.38f)
            Ic.Left -> p(0.62f to 0.25f, 0.37f to 0.5f, 0.62f to 0.75f)
            Ic.Right -> p(0.38f to 0.25f, 0.63f to 0.5f, 0.38f to 0.75f)
            Ic.Home -> { p(0.15f to 0.48f, 0.5f to 0.18f, 0.85f to 0.48f); p(0.25f to 0.4f, 0.25f to 0.84f, 0.75f to 0.84f, 0.75f to 0.4f) }
            Ic.Back -> { p(0.4f to 0.22f, 0.15f to 0.45f, 0.4f to 0.68f); arcBack(s, st, color) }
            Ic.Power -> {
                drawArc(color, -60f, 300f, false, Offset(s * 0.18f, s * 0.2f), Size(s * 0.64f, s * 0.64f), style = st)
                p(0.5f to 0.1f, 0.5f to 0.48f)
            }
            Ic.PlayPause -> { p(0.12f to 0.25f, 0.45f to 0.5f, 0.12f to 0.75f, close = true); p(0.65f to 0.27f, 0.65f to 0.73f); p(0.85f to 0.27f, 0.85f to 0.73f) }
            Ic.VolUp, Ic.VolDown, Ic.Mute -> {
                p(0.12f to 0.4f, 0.28f to 0.4f, 0.5f to 0.2f, 0.5f to 0.8f, 0.28f to 0.6f, 0.12f to 0.6f, close = true)
                when (icon) {
                    Ic.VolUp -> { p(0.64f to 0.5f, 0.9f to 0.5f); p(0.77f to 0.37f, 0.77f to 0.63f) }
                    Ic.VolDown -> p(0.64f to 0.5f, 0.9f to 0.5f)
                    else -> { p(0.65f to 0.38f, 0.88f to 0.62f); p(0.88f to 0.38f, 0.65f to 0.62f) }
                }
            }
            Ic.Keyboard -> {
                drawRoundRect(color, Offset(s * 0.08f, s * 0.25f), Size(s * 0.84f, s * 0.5f), androidx.compose.ui.geometry.CornerRadius(s * 0.08f), style = st)
                p(0.3f to 0.6f, 0.7f to 0.6f)
                listOf(0.25f, 0.42f, 0.58f, 0.75f).forEach { x -> drawCircle(color, s * 0.035f, Offset(x * s, s * 0.42f)) }
            }
            Ic.Rewind -> { p(0.5f to 0.25f, 0.2f to 0.5f, 0.5f to 0.75f, close = true); p(0.85f to 0.25f, 0.55f to 0.5f, 0.85f to 0.75f, close = true) }
            Ic.Forward -> { p(0.5f to 0.25f, 0.8f to 0.5f, 0.5f to 0.75f, close = true); p(0.15f to 0.25f, 0.45f to 0.5f, 0.15f to 0.75f, close = true) }
            Ic.Menu -> { p(0.2f to 0.3f, 0.8f to 0.3f); p(0.2f to 0.5f, 0.8f to 0.5f); p(0.2f to 0.7f, 0.8f to 0.7f) }
        }
    }
}

private fun DrawScope.arcBack(s: Float, st: Stroke, color: Color) {
    val path = Path().apply {
        moveTo(0.15f * s, 0.45f * s)
        lineTo(0.62f * s, 0.45f * s)
        cubicTo(0.95f * s, 0.45f * s, 0.95f * s, 0.85f * s, 0.62f * s, 0.85f * s)
        lineTo(0.4f * s, 0.85f * s)
    }
    drawPath(path, color, style = st)
}
