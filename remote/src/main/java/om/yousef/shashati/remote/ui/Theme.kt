package om.yousef.shashati.remote.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import om.yousef.shashati.remote.R

val Plex = FontFamily(
    Font(R.font.plex_regular, FontWeight.Normal),
    Font(R.font.plex_medium, FontWeight.Medium),
    Font(R.font.plex_semibold, FontWeight.SemiBold),
)

object C {
    val gold = Color(0xFFE2B45A)
    val goldSoft = Color(0x33E2B45A)
    val glass = Color(0x17FFFFFF)
    val glassStrong = Color(0x26FFFFFF)
    val pressed = Color(0x40FFFFFF)
    val border = Color(0x29FFFFFF)
    val dim = Color(0xB3FFFFFF)
    val faint = Color(0x80FFFFFF)
    val dark = Color(0xFF0A0F1C)
    val red = Color(0xFFE0584F)
    val green = Color(0xFF5FCB8A)
}

@Composable
fun RemoteTheme(content: @Composable () -> Unit) {
    val d = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(d.density, fontScale = d.fontScale.coerceAtMost(1.15f)),
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalTextStyle provides TextStyle(fontFamily = Plex, color = Color.White, fontSize = 15.sp),
    ) { content() }
}

@Composable
fun Background() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A0F1C), Color(0xFF05070D))))
            .drawBehind {
                val w = size.width
                val h = size.height
                drawRect(Brush.radialGradient(listOf(Color(0x8C2E5EAA), Color.Transparent), Offset(w * 0.85f, h * 0.08f), w * 0.7f))
                drawRect(Brush.radialGradient(listOf(Color(0x55C4783C), Color.Transparent), Offset(w * 0.1f, h * 0.35f), w * 0.6f))
                drawRect(Brush.radialGradient(listOf(Color(0x45147878), Color.Transparent), Offset(w * 0.5f, h * 1.05f), w * 0.8f))
            }
    )
}

/**
 * A glass button. [repeat] keeps sending while the finger stays down (arrows, volume).
 */
@Composable
fun GlassButton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    color: Color = C.glass,
    borderColor: Color = C.border,
    repeat: Boolean = false,
    onPress: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val view = LocalView.current
    val action by rememberUpdatedState(onPress)
    var down by remember { mutableStateOf(false) }
    Box(
        modifier
            .scale(if (down) 0.94f else 1f)
            .clip(shape)
            .background(if (down) C.pressed else color, shape)
            .border(1.dp, if (down) C.gold else borderColor, shape)
            .pointerInput(repeat) {
                coroutineScope {
                    awaitEachGesture {
                        // A button inside another button handles the tap alone.
                        awaitFirstDown().consume()
                        down = true
                        val job = if (repeat) this@coroutineScope.launch {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            action()
                            delay(450)
                            while (true) { action(); delay(120) }
                        } else null
                        val up = waitForUpOrCancellation()
                        job?.cancel()
                        down = false
                        if (!repeat && up != null) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            action()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun GoldButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    GlassButton(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (enabled) C.gold else C.glass,
        borderColor = Color.Transparent,
        onPress = { if (enabled) onClick() },
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
            color = if (enabled) C.dark else C.faint,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun TextButtonGlass(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassButton(modifier = modifier, shape = RoundedCornerShape(50), onPress = onClick) {
        Text(text, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp), fontSize = 14.sp)
    }
}

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    GlassButton(
        shape = RoundedCornerShape(50),
        color = if (selected) Color.White else C.glass,
        borderColor = if (selected) Color.Transparent else C.border,
        onPress = onClick,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 14.sp,
            color = if (selected) C.dark else Color.White,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}


/** Page header with a back arrow (on the right, pointing right, as in Arabic apps). */
@Composable
fun BackHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)?) {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            GlassButton(Modifier.size(46.dp), shape = androidx.compose.foundation.shape.CircleShape, onPress = onBack) {
                LineIcon(Ic.Right, size = 22.dp)
            }
            androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
        }
        androidx.compose.foundation.layout.Column {
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, color = C.dim, fontSize = 13.sp)
        }
    }
}
