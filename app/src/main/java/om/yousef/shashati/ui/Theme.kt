package om.yousef.shashati.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import om.yousef.shashati.R

val Plex = FontFamily(
    Font(R.font.plex_light, FontWeight.Light),
    Font(R.font.plex_regular, FontWeight.Normal),
    Font(R.font.plex_medium, FontWeight.Medium),
    Font(R.font.plex_semibold, FontWeight.SemiBold),
)
val Amiri = FontFamily(Font(R.font.amiri_regular))

object C {
    val gold = Color(0xFFE2B45A)
    val goldSoft = Color(0x2EE2B45A)
    val glass = Color(0x17FFFFFF)
    val glassStrong = Color(0x24FFFFFF)
    val border = Color(0x29FFFFFF)
    val dim = Color(0xB3FFFFFF)
    val faint = Color(0x80FFFFFF)
    val live = Color(0xFFD93A3A)
    val danger = Color(0x38D93A3A)
    val dark = Color(0xFF0A0F1C)
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalTextStyle provides TextStyle(fontFamily = Plex, color = Color.White, fontSize = 14.sp),
    ) { content() }
}

/** Dark background with soft colour glows, like the approved design. */
@Composable
fun AppBackground(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A0F1C), Color(0xFF05070D))))
            .drawBehind {
                val w = size.width
                val h = size.height
                drawRect(Brush.radialGradient(listOf(Color(0x8C2E5EAA), Color.Transparent), Offset(w * 0.78f, h * 0.18f), w * 0.30f))
                drawRect(Brush.radialGradient(listOf(Color(0x61C4783C), Color.Transparent), Offset(w * 0.18f, h * 0.30f), w * 0.26f))
                drawRect(Brush.radialGradient(listOf(Color(0x59147878), Color.Transparent), Offset(w * 0.5f, h * 1.1f), w * 0.34f))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0xD905070D)), startY = h * 0.35f, endY = h))
            }
    )
}

/**
 * A remote-friendly focusable box: it grows a little and gets a white frame when selected,
 * and OK on the remote triggers [onClick].
 */
@Composable
fun Focusable(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    focusRequester: FocusRequester? = null,
    scaleOnFocus: Float = 1.06f,
    background: Color = C.glass,
    focusedBackground: Color? = null,
    showBorder: Boolean = true,
    onFocus: (() -> Unit)? = null,
    onClick: () -> Unit,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) scaleOnFocus else 1f, label = "focusScale")
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus?.invoke()
            }
            .clip(shape)
            .background(if (focused && focusedBackground != null) focusedBackground else background, shape)
            .border(
                width = if (focused) 3.dp else 1.dp,
                color = if (focused) Color.White else if (showBorder) C.border else Color.Transparent,
                shape = shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        content = { content(focused) }
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xD9FFFFFF))
}

@Composable
fun Pill(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    fontSize: Int = 12,
    hPad: Dp = 12.dp,
    onClick: () -> Unit,
) {
    Focusable(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        background = if (selected) Color.White else C.glass,
        scaleOnFocus = 1.08f,
        onClick = onClick,
    ) {
        Text(
            text,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = hPad, vertical = 6.dp),
            fontSize = fontSize.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) C.dark else Color.White
        )
    }
}

/** Remaining time in Arabic, e.g. "بعد 14 دقيقة" or "بعد ساعتين و 5 دقائق". */
fun remainingText(totalMinutes: Long): String {
    val m = totalMinutes.coerceAtLeast(0)
    if (m < 1) return "الآن"
    if (m < 60) return "بعد $m دقيقة"
    val h = m / 60
    val r = m % 60
    val hours = when (h) { 1L -> "ساعة"; 2L -> "ساعتين"; else -> "$h ساعات" }
    return if (r == 0L) "بعد $hours" else "بعد $hours و $r دقيقة"
}
