package om.yousef.shashati.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import om.yousef.shashati.data.Prefs

/** Number pad usable with the arrows + OK, or directly with the remote's number keys. */
@Composable
fun Keypad(onDigit: (Char) -> Unit, onDelete: () -> Unit, focusFirst: Boolean = true) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (focusFirst) {
            delay(120)
            runCatching { first.requestFocus() }
        }
    }
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (r in 0 until 4) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (c in 0 until 3) {
                        val k = keys[r * 3 + c]
                        if (k.isEmpty()) {
                            Spacer(Modifier.size(58.dp, 32.dp))
                        } else {
                            Focusable(
                                modifier = Modifier.size(58.dp, 32.dp),
                                shape = RoundedCornerShape(10.dp),
                                focusRequester = if (k == "1") first else null,
                                background = Color(0x1AFFFFFF),
                                onClick = { if (k == "⌫") onDelete() else onDigit(k[0]) }
                            ) {
                                Text(k, fontSize = 16.sp, modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Handles the remote's physical number keys and delete. */
fun Modifier.digitKeys(onDigit: (Char) -> Unit, onDelete: () -> Unit): Modifier = onPreviewKeyEvent { e ->
    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    val code = e.nativeKeyEvent.keyCode
    when {
        code in AndroidKeyEvent.KEYCODE_0..AndroidKeyEvent.KEYCODE_9 -> { onDigit('0' + (code - AndroidKeyEvent.KEYCODE_0)); true }
        code in AndroidKeyEvent.KEYCODE_NUMPAD_0..AndroidKeyEvent.KEYCODE_NUMPAD_9 -> { onDigit('0' + (code - AndroidKeyEvent.KEYCODE_NUMPAD_0)); true }
        code == AndroidKeyEvent.KEYCODE_DEL -> { onDelete(); true }
        else -> false
    }
}

@Composable
fun Dots(count: Int, filled: Int) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            repeat(count) { i ->
                Box(
                    Modifier.size(13.dp)
                        .border(1.5.dp, Color.White, CircleShape)
                        .background(if (i < filled) Color.White else Color.Transparent, CircleShape)
                )
            }
        }
    }
}

@Composable
fun GlassPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .background(Color(0x12FFFFFF), RoundedCornerShape(20.dp))
            .border(1.dp, C.border, RoundedCornerShape(20.dp))
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) { content() }
}

/**
 * Asks for the PIN (or the recovery code via "نسيت الرقم السري؟").
 * Five wrong tries in a row block input for 1, then 5, then 15 minutes.
 */
@Composable
fun PinGate(title: String, onSuccess: (recovered: Boolean) -> Unit) {
    var recoverMode by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var blocked by remember { mutableLongStateOf(Prefs.blockedSeconds()) }
    val confirmFocus = remember { FocusRequester() }
    LaunchedEffect(recoverMode, input.length) {
        if (recoverMode && input.length == 10) runCatching { confirmFocus.requestFocus() }
    }
    LaunchedEffect(blocked > 0) {
        while (Prefs.blockedSeconds() > 0) {
            blocked = Prefs.blockedSeconds()
            delay(1000)
        }
        blocked = 0
    }

    fun submit(value: String) {
        val ok = if (recoverMode) Prefs.checkRecovery(value) else Prefs.checkPin(value)
        if (ok) {
            Prefs.registerSuccess()
            onSuccess(recoverMode)
        } else {
            Prefs.registerFail()
            error = true
            input = ""
            blocked = Prefs.blockedSeconds()
        }
    }

    val onDigit: (Char) -> Unit = { d ->
        if (blocked == 0L) {
            val max = if (recoverMode) 10 else 4
            if (input.length < max) input += d
            error = false
            if (!recoverMode && input.length == 4) submit(input)
        }
    }
    val onDelete: () -> Unit = { input = input.dropLast(1) }

    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).digitKeys(onDigit, onDelete), contentAlignment = Alignment.TopCenter) {
        GlassPanel(Modifier.width(380.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                LockGlyph()
                if (blocked > 0) {
                    Text("تم إيقاف الإدخال مؤقتًا", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("5 محاولات خاطئة متتالية", fontSize = 13.sp, color = C.dim)
                    Text("%d:%02d".format(java.util.Locale.US, blocked / 60, blocked % 60), fontSize = 38.sp, fontWeight = FontWeight.Light, color = C.gold)
                    Text("تزيد المدة مع التكرار: دقيقة ← 5 دقائق ← 15 دقيقة", fontSize = 11.sp, color = C.faint)
                } else {
                    Text(if (recoverMode) "رمز الاسترداد" else title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (recoverMode) "من 4 إلى 10 أرقام · ${input.length} / 10" else "أدخل الرقم السري",
                        fontSize = 13.sp, color = C.dim
                    )
                    if (recoverMode) Dots(input.length.coerceAtLeast(1), input.length) else Dots(4, input.length)
                    if (error) Text("رمز خاطئ · بقيت ${5 - Prefs.fails} محاولات", fontSize = 12.sp, color = Color(0xFFFFB3B3))
                    Keypad(onDigit, onDelete)
                    if (recoverMode) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Pill("تأكيد", selected = input.length >= 4, modifier = Modifier.focusRequester(confirmFocus), fontSize = 14, hPad = 22.dp) {
                                if (input.length >= 4) submit(input)
                            }
                            Pill("رجوع", selected = false, fontSize = 14, hPad = 18.dp) {
                                recoverMode = false; input = ""; error = false
                            }
                        }
                    } else if (Prefs.recoveryHash != null) {
                        Pill("نسيت الرقم السري؟", selected = false, fontSize = 12) {
                            recoverMode = true; input = ""; error = false
                        }
                    }
                }
            }
        }
    }
}

/** Creates a new PIN (entered twice) and optionally a new recovery code. */
@Composable
fun PinSetup(askPin: Boolean, askRecovery: Boolean, onDone: () -> Unit, onCancel: (() -> Unit)? = null) {
    // steps: 0 = new pin, 1 = confirm pin, 2 = recovery code
    var step by remember { mutableStateOf(if (askPin) 0 else 2) }
    var first by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }
    val saveFocus = remember { FocusRequester() }
    LaunchedEffect(step, input.length) {
        if (step == 2 && input.length == 10) runCatching { saveFocus.requestFocus() }
    }

    fun finishPinOrNext() {
        if (askRecovery) { step = 2; input = "" } else onDone()
    }

    val onDigit: (Char) -> Unit = { d ->
        val max = if (step == 2) 10 else 4
        if (input.length < max) input += d
        mismatch = false
        if (step == 0 && input.length == 4) { first = input; input = ""; step = 1 }
        else if (step == 1 && input.length == 4) {
            if (input == first) { Prefs.setPin(input); Prefs.registerSuccess(); finishPinOrNext() }
            else { mismatch = true; input = ""; step = 0; first = "" }
        }
    }
    val onDelete: () -> Unit = { input = input.dropLast(1) }

    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).digitKeys(onDigit, onDelete), contentAlignment = Alignment.TopCenter) {
        GlassPanel(Modifier.width(400.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                LockGlyph()
                val title = when (step) {
                    0 -> "اختر رقمًا سريًا من 4 أرقام"
                    1 -> "أعد إدخال الرقم السري للتأكيد"
                    else -> "اختر رمز الاسترداد"
                }
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    when (step) {
                        2 -> "من 4 إلى 10 أرقام · احفظه في مكان آمن · ${input.length} / 10"
                        else -> "يُطلب لفتح الإعدادات والخروج للواجهة الأصلية"
                    },
                    fontSize = 12.sp, color = C.dim
                )
                if (step == 2) Dots(input.length.coerceAtLeast(1), input.length) else Dots(4, input.length)
                if (mismatch) Text("الرقمان غير متطابقين، حاول مرة أخرى", fontSize = 12.sp, color = Color(0xFFFFB3B3))
                if (step == 2 && input.length >= 4) Text("بعد إدخال الرمز انزل بالسهم إلى «حفظ» واضغط OK", fontSize = 11.sp, color = C.gold)
                Keypad(onDigit, onDelete)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (step == 2) {
                        Pill("حفظ", selected = input.length >= 4, modifier = Modifier.focusRequester(saveFocus), fontSize = 14, hPad = 22.dp) {
                            if (input.length >= 4) { Prefs.setRecovery(input); onDone() }
                        }
                    }
                    if (onCancel != null) Pill("إلغاء", selected = false, fontSize = 14, hPad = 18.dp) { onCancel() }
                }
            }
        }
    }
}

@Composable
private fun LockGlyph() {
    androidx.compose.foundation.Canvas(Modifier.size(22.dp)) {
        val s = size.width
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = s * 0.07f)
        drawRoundRect(
            Color.White,
            topLeft = androidx.compose.ui.geometry.Offset(s * 0.2f, s * 0.45f),
            size = androidx.compose.ui.geometry.Size(s * 0.6f, s * 0.45f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.08f),
            style = stroke
        )
        drawArc(
            Color.White, 180f, 180f, false,
            topLeft = androidx.compose.ui.geometry.Offset(s * 0.32f, s * 0.12f),
            size = androidx.compose.ui.geometry.Size(s * 0.36f, s * 0.6f),
            style = stroke
        )
    }
}
