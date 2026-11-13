package om.yousef.shashati.remote.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import om.yousef.shashati.remote.atv.AtvPairing
import om.yousef.shashati.remote.data.SavedTv
import om.yousef.shashati.remote.data.TvStore
import om.yousef.shashati.remote.net.Controller
import om.yousef.shashati.remote.net.Discovery
import om.yousef.shashati.remote.net.FoundTv
import om.yousef.shashati.remote.net.ShashatiApi

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .background(C.glass, RoundedCornerShape(22.dp))
            .border(1.dp, C.border, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) { content() }
}

@Composable
fun Field(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    big: Boolean = false,
    ltr: Boolean = false,
    focus: FocusRequester? = null,
    onDone: () -> Unit = {},
) {
    val style = TextStyle(
        fontFamily = Plex, color = Color.White,
        fontSize = if (big) 30.sp else 16.sp,
        textAlign = if (big) TextAlign.Center else TextAlign.Start,
        letterSpacing = if (big) 8.sp else 0.sp,
        fontWeight = if (big) FontWeight.SemiBold else FontWeight.Normal,
    )
    CompositionLocalProvider(LocalLayoutDirection provides if (ltr) LayoutDirection.Ltr else LayoutDirection.Rtl) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(C.gold),
            keyboardOptions = keyboard,
            keyboardActions = KeyboardActions(onAny = { onDone() }),
            modifier = modifier
                .then(if (focus != null) Modifier.focusRequester(focus) else Modifier)
                .background(Color(0x14FFFFFF), RoundedCornerShape(16.dp))
                .border(1.dp, C.border, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = if (big) 14.dp else 13.dp),
            decorationBox = { inner ->
                Box(contentAlignment = if (big) Alignment.Center else Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, style = style.copy(color = C.faint, fontSize = if (big) 18.sp else 15.sp, letterSpacing = 0.sp))
                    inner()
                }
            }
        )
    }
}

@Composable
fun TvListScreen(onOpen: (SavedTv) -> Unit, onPair: (FoundTv) -> Unit, onInstall: () -> Unit = {}, onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    var saved by remember { mutableStateOf(TvStore.all()) }
    val found = remember { mutableStateListOf<FoundTv>() }
    var manual by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        val d = Discovery(context) { list -> found.clear(); found.addAll(list) }
        d.start()
        onDispose { d.stop() }
    }

    Box(Modifier.fillMaxSize()) {
        Background()
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.padding(top = 28.dp, bottom = 8.dp)) {
                    BackHeader("ريموت شاشتي", "اختر التلفزيون اللي تبي تتحكم فيه", onBack)
                }
            }
            if (saved.isNotEmpty()) {
                item { Text("تلفزيوناتي", color = C.gold, fontWeight = FontWeight.Medium) }
                items(saved, key = { "s" + it.host }) { tv ->
                    val online = found.any { it.host == tv.host }
                    GlassButton(Modifier.fillMaxWidth(), onPress = { onOpen(tv) }) {
                        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(if (online) C.green else C.faint, CircleShape))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tv.name, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    (if (online) "متصل بالشبكة" else "غير ظاهر الآن") + " · " + tv.host,
                                    fontSize = 12.sp, color = C.faint
                                )
                            }
                            TextButtonGlass("حذف") {
                                TvStore.remove(tv.host)
                                saved = TvStore.all()
                            }
                        }
                    }
                }
            }
            val fresh = found.filter { f -> saved.none { it.host == f.host } }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Text("تلفزيونات في البيت", color = C.gold, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    CircularProgressIndicator(Modifier.size(16.dp), color = C.gold, strokeWidth = 2.dp)
                }
            }
            if (fresh.isEmpty()) {
                item {
                    Text(
                        "جاري البحث… تأكد إن الجوال والتلفزيون على نفس شبكة الواي فاي وإن التلفزيون شغّال.",
                        color = C.faint, fontSize = 13.sp
                    )
                }
            }
            items(fresh, key = { "f" + it.host }) { tv ->
                GlassButton(Modifier.fillMaxWidth(), onPress = { onPair(tv) }) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(tv.name, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            Text(
                                (if (tv.hasShashati) "فيه شاشتي" else "بدون شاشتي") + " · " + tv.host,
                                fontSize = 12.sp, color = C.faint
                            )
                        }
                        Text("ربط", color = C.gold, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item {
                GlassButton(Modifier.fillMaxWidth().padding(top = 8.dp), color = C.goldSoft, borderColor = C.gold, onPress = onInstall) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text("تثبيت شاشتي على تلفزيون", color = C.gold, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("تثبيت لأول مرة أو تحديث، بدون لابتوب", color = C.dim, fontSize = 13.sp)
                    }
                }
            }
            item {
                Card(Modifier.padding(top = 8.dp, bottom = 30.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ما طلع التلفزيون؟ اكتب عنوانه (IP)", color = C.dim, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Field(
                                manual, { manual = it.filter { c -> c.isDigit() || c == '.' } }, "مثال 192.168.1.20",
                                Modifier.weight(1f), KeyboardOptions(keyboardType = KeyboardType.Decimal), ltr = true
                            )
                            Spacer(Modifier.width(10.dp))
                            GoldButton("إضافة", enabled = manual.count { it == '.' } == 3) {
                                onPair(FoundTv(manual.trim(), "تلفزيون", hasKeys = true, hasShashati = true))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PairScreen(tv: FoundTv, onDone: (SavedTv) -> Unit, onCancel: () -> Unit) {
    val scope = rememberCoroutineScope()
    // Step 1: Google remote keys. Step 2: شاشتي (channels, apps, typing).
    var step by remember { mutableStateOf(1) }
    var busy by remember { mutableStateOf(false) }
    var codeShown by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var keysOk by remember { mutableStateOf(false) }
    var token by remember { mutableStateOf<String?>(null) }
    var tvName by remember { mutableStateOf(tv.name) }
    val pairing = remember { AtvPairing(tv.host, Controller.identity, "ريموت شاشتي · " + (Build.MODEL ?: "جوال")) }
    val api = remember { ShashatiApi(tv.host, null) }
    val focus = remember { FocusRequester() }

    DisposableEffect(Unit) { onDispose { Thread { pairing.close() }.start() } }

    fun finishAll() {
        val old = TvStore.get(tv.host)
        val t = SavedTv(tv.host, tvName, token ?: old?.token, keysOk || old?.keysPaired == true)
        TvStore.save(t)
        onDone(t)
    }

    fun goStep2() {
        step = 2; codeShown = false; code = ""; error = null
        scope.launch {
            busy = true
            val info = api.info()
            busy = false
            if (info == null || info.optString("app") != "shashati") {
                if (!keysOk) { error = "ما قدرت أوصل للتلفزيون. تأكد من العنوان والشبكة."; return@launch }
                finishAll()
            } else {
                info.optString("name").takeIf { it.isNotBlank() }?.let { tvName = it }
            }
        }
    }

    LaunchedEffect(codeShown) { if (codeShown) runCatching { focus.requestFocus() } }

    Box(Modifier.fillMaxSize()) {
        Background()
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BackHeader("ربط $tvName", "الخطوة $step من 2", onCancel)

            Card {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (step == 1) {
                        Text("أزرار الريموت", color = C.gold, fontWeight = FontWeight.Medium, fontSize = 18.sp)
                        Text(
                            if (!codeShown) "اضغط الزر وبيطلع على شاشة التلفزيون رمز من 6 خانات (أرقام وحروف)."
                            else "اكتب الرمز اللي ظاهر على التلفزيون:",
                            color = C.dim
                        )
                        if (codeShown) {
                            Field(
                                code, { v -> code = v.uppercase().filter { it in "0123456789ABCDEF" }.take(6) }, "XXXXXX",
                                Modifier.fillMaxWidth(),
                                KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrect = false, imeAction = ImeAction.Done),
                                big = true, ltr = true, focus = focus,
                            )
                        }
                    } else {
                        Text("قائمة القنوات والتطبيقات والكتابة", color = C.gold, fontWeight = FontWeight.Medium, fontSize = 18.sp)
                        Text(
                            if (!codeShown) "اضغط الزر وبيطلع على التلفزيون رمز من 4 أرقام من شاشتي."
                            else "اكتب الرقم اللي ظاهر على التلفزيون:",
                            color = C.dim
                        )
                        if (codeShown) {
                            Field(
                                code, { v -> code = v.filter { it.isDigit() }.take(4) }, "____",
                                Modifier.fillMaxWidth(),
                                KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                                big = true, ltr = true, focus = focus,
                            )
                        }
                    }
                    error?.let { Text(it, color = C.red, fontSize = 14.sp) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (busy) {
                            CircularProgressIndicator(Modifier.size(26.dp), color = C.gold, strokeWidth = 3.dp)
                        } else if (!codeShown) {
                            GoldButton("اعرض الرمز على التلفزيون") {
                                error = null
                                scope.launch {
                                    busy = true
                                    if (step == 1) {
                                        val r = withContext(Dispatchers.IO) { runCatching { pairing.start() } }
                                        if (r.isSuccess) codeShown = true
                                        else error = "التلفزيون ما رد. تأكد إنه شغّال وعلى نفس الواي فاي."
                                    } else {
                                        val r = api.pairStart()
                                        if (r?.optBoolean("ok") == true) codeShown = true
                                        else error = "شاشتي ما ردت. تأكد إن شاشتي مثبتة ومفعّلة على التلفزيون."
                                    }
                                    busy = false
                                }
                            }
                        } else {
                            GoldButton("تأكيد", enabled = code.length == (if (step == 1) 6 else 4)) {
                                error = null
                                scope.launch {
                                    busy = true
                                    if (step == 1) {
                                        val r = withContext(Dispatchers.IO) { runCatching { pairing.finish(code) } }
                                        when {
                                            r.getOrNull() == true -> { keysOk = true; goStep2() }
                                            r.isSuccess -> { error = "الرمز غير صحيح، تأكد منه وحاول مرة ثانية."; code = "" }
                                            else -> { error = "انقطع الاتصال. اضغط «اعرض الرمز» من جديد."; codeShown = false; code = "" }
                                        }
                                    } else {
                                        val r = api.pairFinish(code, Build.MODEL ?: "جوال")
                                        when {
                                            r?.optBoolean("ok") == true -> { token = r.optString("token"); finishAll() }
                                            r?.optString("reason") == "bad_code" -> { error = "الرقم غير صحيح."; code = "" }
                                            else -> { error = "انتهت صلاحية الرقم. اطلب رقم جديد."; codeShown = false; code = "" }
                                        }
                                    }
                                    busy = false
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        if (!busy) {
                            TextButtonGlass("تخطي") {
                                if (step == 1) goStep2() else if (keysOk) finishAll() else onCancel()
                            }
                        }
                    }
                }
            }

            Text(
                if (step == 1) "هذا الربط يخلي الأسهم وزر موافق وزر التشغيل تشتغل في كل التطبيقات (يوتيوب، نتفلكس…)."
                else "هذا الربط يخلّيك تختار القناة أو التطبيق من الجوال وتكتب في خانات البحث.",
                color = C.faint, fontSize = 13.sp
            )
        }
    }
}
