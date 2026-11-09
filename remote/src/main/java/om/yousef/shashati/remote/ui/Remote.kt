package om.yousef.shashati.remote.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import om.yousef.shashati.remote.atv.AtvRemote
import om.yousef.shashati.remote.net.Controller
import org.json.JSONObject

private enum class Tab(val title: String) { Remote("الريموت"), Channels("القنوات"), Apps("التطبيقات") }

@Composable
fun RemoteScreen(onSwitchTv: () -> Unit, onRepair: () -> Unit) {
    val tv = Controller.tv ?: return
    var tab by remember { mutableStateOf(Tab.Remote) }
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toast) { if (toast != null) { delay(2500); toast = null } }

    Box(Modifier.fillMaxSize()) {
        Background()
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            // Top bar
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassButton(Modifier.weight(1f), shape = RoundedCornerShape(18.dp), onPress = onSwitchTv) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        val (dot, label) = when {
                            !tv.keysPaired -> C.gold to "الأزرار غير مربوطة"
                            Controller.keysState == AtvRemote.State.Connected -> C.green to "متصل"
                            Controller.keysState == AtvRemote.State.NeedsPairing -> C.red to "يحتاج ربط من جديد"
                            Controller.keysState == AtvRemote.State.Connecting -> C.gold to "جاري الاتصال…"
                            else -> C.red to "غير متصل"
                        }
                        Box(Modifier.size(10.dp).background(dot, CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tv.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(label, fontSize = 12.sp, color = C.faint)
                        }
                        Text("تغيير", fontSize = 13.sp, color = C.gold)
                    }
                }
                Spacer(Modifier.width(12.dp))
                GlassButton(Modifier.size(56.dp), shape = CircleShape, color = Color(0x33E0584F), borderColor = Color(0x66E0584F),
                    onPress = { Controller.key(AtvRemote.POWER) }) {
                    LineIcon(Ic.Power, size = 24.dp, color = Color(0xFFFF8A80))
                }
            }

            if (!tv.keysPaired || Controller.keysState == AtvRemote.State.NeedsPairing) {
                GlassButton(Modifier.fillMaxWidth().padding(horizontal = 16.dp), color = C.goldSoft, borderColor = C.gold, onPress = onRepair) {
                    Text(
                        "اضغط هنا لربط أزرار الأسهم والتشغيل مع هذا التلفزيون",
                        Modifier.padding(12.dp), fontSize = 13.sp, color = C.gold, textAlign = TextAlign.Center
                    )
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    Tab.Remote -> RemotePad(onToast = { toast = it })
                    Tab.Channels -> ChannelsTab(onToast = { toast = it })
                    Tab.Apps -> AppsTab(onToast = { toast = it })
                }
                toast?.let {
                    Text(
                        it,
                        Modifier.align(Alignment.BottomCenter).padding(12.dp)
                            .background(Color(0xEE1A2133), RoundedCornerShape(50)).border(1.dp, C.border, RoundedCornerShape(50))
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        fontSize = 14.sp
                    )
                }
            }

            // Bottom tabs
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                    .background(C.glass, RoundedCornerShape(50)).border(1.dp, C.border, RoundedCornerShape(50)).padding(5.dp),
            ) {
                Tab.entries.forEach { t ->
                    GlassButton(
                        Modifier.weight(1f), shape = RoundedCornerShape(50),
                        color = if (t == tab) Color.White else Color.Transparent, borderColor = Color.Transparent,
                        onPress = { tab = t }
                    ) {
                        Text(
                            t.title, Modifier.padding(vertical = 11.dp),
                            color = if (t == tab) C.dark else Color.White,
                            fontWeight = if (t == tab) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeBar(onToast: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    fun send(search: Boolean) {
        val api = Controller.api ?: return
        if (Controller.tv?.token == null) { onToast("اربط شاشتي أولاً عشان تقدر تكتب"); return }
        scope.launch {
            val r = api.text(text)
            if (r?.optBoolean("ok") == true) {
                if (search) { delay(150); Controller.key(AtvRemote.ENTER) }
                onToast(if (search) "تم البحث" else "تم إرسال الكتابة")
            } else if (r?.optString("reason") == "no_field") {
                onToast("افتح خانة الكتابة على التلفزيون أولاً")
            } else onToast("ما وصلت للتلفزيون")
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Field(
            text, { text = it }, "اكتب هنا وينكتب في التلفزيون", Modifier.weight(1f),
            KeyboardOptions(imeAction = ImeAction.Search), onDone = { send(true) }
        )
        Spacer(Modifier.width(8.dp))
        GlassButton(Modifier.size(50.dp), shape = CircleShape, color = C.gold, borderColor = Color.Transparent, onPress = { send(true) }) {
            Text("بحث", color = C.dark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RemotePad(onToast: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        TypeBar(onToast)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            RoundKey(Ic.Back, "رجوع") { Controller.key(AtvRemote.BACK) }
            RoundKey(Ic.Home, "الرئيسية") { Controller.key(AtvRemote.HOME) }
            RoundKey(Ic.Menu, "القائمة") { Controller.key(AtvRemote.MENU) }
        }

        DPad()

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            // Volume rocker
            Column(
                Modifier.background(C.glass, RoundedCornerShape(36.dp)).border(1.dp, C.border, RoundedCornerShape(36.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GlassButton(Modifier.size(68.dp, 60.dp), shape = RoundedCornerShape(36.dp), color = Color.Transparent, borderColor = Color.Transparent,
                    repeat = true, onPress = { Controller.key(AtvRemote.VOL_UP) }) { LineIcon(Ic.VolUp, size = 26.dp) }
                Text("الصوت", fontSize = 11.sp, color = C.faint)
                GlassButton(Modifier.size(68.dp, 60.dp), shape = RoundedCornerShape(36.dp), color = Color.Transparent, borderColor = Color.Transparent,
                    repeat = true, onPress = { Controller.key(AtvRemote.VOL_DOWN) }) { LineIcon(Ic.VolDown, size = 26.dp) }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                RoundKey(Ic.PlayPause, "تشغيل/إيقاف") { Controller.key(AtvRemote.PLAY_PAUSE) }
                RoundKey(Ic.Mute, "كتم") { Controller.key(AtvRemote.MUTE) }
            }
            // Channel rocker (inside شاشتي's player it moves between channels)
            Column(
                Modifier.background(C.glass, RoundedCornerShape(36.dp)).border(1.dp, C.border, RoundedCornerShape(36.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GlassButton(Modifier.size(68.dp, 60.dp), shape = RoundedCornerShape(36.dp), color = Color.Transparent, borderColor = Color.Transparent,
                    onPress = { Controller.key(AtvRemote.CH_UP) }) { LineIcon(Ic.Up, size = 26.dp) }
                Text("القناة", fontSize = 11.sp, color = C.faint)
                GlassButton(Modifier.size(68.dp, 60.dp), shape = RoundedCornerShape(36.dp), color = Color.Transparent, borderColor = Color.Transparent,
                    onPress = { Controller.key(AtvRemote.CH_DOWN) }) { LineIcon(Ic.Down, size = 26.dp) }
            }
        }
    }
}

@Composable
private fun RoundKey(icon: Ic, label: String, onPress: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GlassButton(Modifier.size(58.dp), shape = CircleShape, onPress = onPress) { LineIcon(icon, size = 24.dp) }
        Text(label, fontSize = 11.sp, color = C.faint, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun DPad() {
    // Arrows must match the physical directions, so this part is laid out left-to-right.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            Modifier.size(250.dp)
                .background(Brush.radialGradient(listOf(Color(0x24FFFFFF), Color(0x10FFFFFF))), CircleShape)
                .border(1.dp, C.border, CircleShape)
        ) {
            val arrow = Modifier.size(78.dp)
            GlassButton(arrow.align(Alignment.TopCenter), shape = CircleShape, color = Color.Transparent, borderColor = Color.Transparent,
                repeat = true, onPress = { Controller.key(AtvRemote.UP) }) { LineIcon(Ic.Up, size = 30.dp) }
            GlassButton(arrow.align(Alignment.BottomCenter), shape = CircleShape, color = Color.Transparent, borderColor = Color.Transparent,
                repeat = true, onPress = { Controller.key(AtvRemote.DOWN) }) { LineIcon(Ic.Down, size = 30.dp) }
            GlassButton(arrow.align(Alignment.CenterStart), shape = CircleShape, color = Color.Transparent, borderColor = Color.Transparent,
                repeat = true, onPress = { Controller.key(AtvRemote.LEFT) }) { LineIcon(Ic.Left, size = 30.dp) }
            GlassButton(arrow.align(Alignment.CenterEnd), shape = CircleShape, color = Color.Transparent, borderColor = Color.Transparent,
                repeat = true, onPress = { Controller.key(AtvRemote.RIGHT) }) { LineIcon(Ic.Right, size = 30.dp) }
            GlassButton(Modifier.size(96.dp).align(Alignment.Center), shape = CircleShape, color = C.gold, borderColor = Color.Transparent,
                onPress = { Controller.key(AtvRemote.OK) }) {
                Text("موافق", color = C.dark, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}

// ---------------- Channels ----------------

private data class Ch(val id: String, val name: String, val category: String, val color: Long)

@Composable
private fun NeedsShashati(text: String) {
    Box(Modifier.fillMaxSize().padding(30.dp), contentAlignment = Alignment.Center) {
        Text(text, color = C.dim, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ChannelsTab(onToast: (String) -> Unit) {
    val api = Controller.api
    if (Controller.tv?.token == null || api == null) {
        NeedsShashati("قائمة القنوات تحتاج شاشتي على التلفزيون.\nاحذف التلفزيون واربطه من جديد بعد تثبيت شاشتي.")
        return
    }
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<JSONObject?>(null) }
    var failed by remember { mutableStateOf(false) }
    var cat by remember { mutableStateOf("all") }
    LaunchedEffect(Controller.tv?.host) {
        val r = api.channels()
        if (r?.optBoolean("ok") == true) data = r else failed = true
    }
    val d = data
    if (d == null) {
        if (failed) NeedsShashati("ما قدرت أجيب القنوات. تأكد إن التلفزيون شغّال.")
        else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = C.gold) }
        return
    }
    val cats = d.getJSONArray("categories").let { a -> (0 until a.length()).map { a.getJSONObject(it).let { o -> o.getString("id") to o.getString("name") } } }
    val favs = d.getJSONArray("favorites").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
    val all = d.getJSONArray("channels").let { a ->
        (0 until a.length()).map { a.getJSONObject(it).let { o -> Ch(o.getString("id"), o.getString("name"), o.optString("category"), o.optLong("color")) } }
    }
    val shown = when (cat) {
        "all" -> all
        "fav" -> all.filter { it.id in favs }
        else -> all.filter { it.category == cat }
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Pill("الكل", cat == "all") { cat = "all" }
            if (favs.isNotEmpty()) Pill("المفضلة", cat == "fav") { cat = "fav" }
            cats.forEach { (id, name) -> Pill(name, cat == id) { cat = id } }
        }
        LazyVerticalGrid(
            GridCells.Fixed(2),
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(shown, key = { it.id }) { ch ->
                val base = Color(ch.color.toInt() or 0xFF000000.toInt())
                GlassButton(
                    Modifier.fillMaxWidth().height(78.dp), shape = RoundedCornerShape(18.dp), color = base.copy(alpha = 0.45f),
                    onPress = {
                        scope.launch {
                            val r = api.play(ch.id)
                            onToast(if (r?.optBoolean("ok") == true) "تشغيل ${ch.name}" else "ما قدرت أشغّل القناة")
                        }
                    }
                ) {
                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0x33FFFFFF), Color.Transparent))))
                    Text(ch.name, Modifier.padding(10.dp), fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 2)
                    if (ch.id in favs) Text("★", Modifier.align(Alignment.TopStart).padding(8.dp), color = C.gold, fontSize = 12.sp)
                }
            }
        }
    }
}

// ---------------- Apps ----------------

@Composable
private fun AppsTab(onToast: (String) -> Unit) {
    val api = Controller.api
    if (Controller.tv?.token == null || api == null) {
        NeedsShashati("فتح التطبيقات يحتاج شاشتي على التلفزيون.")
        return
    }
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<Triple<String, String, Boolean>>?>(null) }
    val icons = remember { mutableStateMapOf<String, Bitmap>() }
    LaunchedEffect(Controller.tv?.host) {
        val r = api.apps()
        apps = r?.optJSONArray("apps")?.let { a ->
            (0 until a.length()).map { a.getJSONObject(it).let { o -> Triple(o.getString("pkg"), o.getString("label"), o.optBoolean("locked")) } }
        } ?: emptyList()
        apps?.forEach { (pkg) -> launch { api.icon(pkg)?.let { icons[pkg] = it } } }
    }
    val list = apps
    if (list == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = C.gold) }
        return
    }
    LazyVerticalGrid(
        GridCells.Fixed(2),
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(list, key = { it.first }) { (pkg, label, locked) ->
            GlassButton(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                onPress = {
                    scope.launch {
                        val r = api.launch(pkg)
                        onToast(
                            when {
                                r?.optBoolean("ok") == true -> if (locked) "$label مقفل، أدخل الرقم السري على التلفزيون" else "فتح $label"
                                r?.optString("reason") == "kids" -> "وضع الأطفال مفعّل"
                                else -> "ما قدرت أفتح $label"
                            }
                        )
                    }
                }
            ) {
                Column {
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)).background(Color(0x22FFFFFF)), contentAlignment = Alignment.Center) {
                        val b = icons[pkg]
                        if (b != null) Image(b.asImageBitmap(), label, Modifier.fillMaxSize(), contentScale = if (b.width > b.height) ContentScale.Crop else ContentScale.Fit)
                        else Text(label.take(1), fontSize = 26.sp, color = C.dim)
                    }
                    Text(
                        (if (locked) "🔒 " else "") + label,
                        Modifier.padding(10.dp), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
