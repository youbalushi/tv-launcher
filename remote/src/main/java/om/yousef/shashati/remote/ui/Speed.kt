package om.yousef.shashati.remote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import om.yousef.shashati.remote.data.TvStore
import om.yousef.shashati.remote.net.Discovery
import om.yousef.shashati.remote.net.FoundTv
import om.yousef.shashati.remote.net.TvInstaller

/** Speeds up a TV: faster animations and turning off apps the owner picks (reversible). */
@Composable
fun SpeedScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val found = remember { mutableStateListOf<FoundTv>() }
    DisposableEffect(Unit) {
        val d = Discovery(context) { list -> found.clear(); found.addAll(list) }
        d.start()
        onDispose { d.stop() }
    }
    val saved = remember { TvStore.all() }
    val choices = (saved.map { it.host to it.name } + found.map { it.host to it.name }).distinctBy { it.first }

    var host by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val apps = remember { mutableStateListOf<TvInstaller.App>() }
    val off = remember { mutableStateListOf<String>() }
    val logs = remember { mutableStateListOf<String>() }

    fun <T> work(block: suspend (TvInstaller, dadb.Dadb) -> T, done: (T) -> Unit) {
        busy = true; message = null; logs.clear()
        scope.launch {
            val inst = TvInstaller(context) { line -> scope.launch { logs.add(line) } }
            try {
                message = "على شاشة التلفزيون: إذا طلع «Allow USB debugging?» اختر Always allow ثم OK"
                val r = withContext(Dispatchers.IO) {
                    val tv = inst.connect(host.trim())
                    try { block(inst, tv) } finally { runCatching { tv.close() } }
                }
                message = null
                done(r)
            } catch (e: Exception) {
                message = "ما قدرت أوصل للتلفزيون أو صار خطأ: ${e.message ?: e.javaClass.simpleName}"
                logs.add(e.toString())
            } finally { busy = false }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Background()
        Column(
            Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BackHeader("تسريع التلفزيون", "حركات أسرع وتعطيل التطبيقات اللي ما تستخدمها") { if (!busy) onClose() }
            Card {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("يحتاج USB debugging مفعّل في التلفزيون (نفس التثبيت).", color = C.dim, fontSize = 13.sp)
                    Text("التعطيل ما يحذف شي، وتقدر ترجّع أي تطبيق من نفس الصفحة بإلغاء علامته.", color = C.dim, fontSize = 13.sp)
                }
            }
            Text("التلفزيون", color = C.gold, fontWeight = FontWeight.Medium)
            if (choices.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    choices.forEach { (h, n) -> Pill("$n · $h", host == h) { host = h; apps.clear(); off.clear() } }
                }
            }
            Field(
                host, { host = it.filter { c -> c.isDigit() || c == '.' }; apps.clear(); off.clear() }, "عنوان التلفزيون، مثال 192.168.100.17",
                Modifier.fillMaxWidth(), KeyboardOptions(keyboardType = KeyboardType.Decimal), ltr = true
            )
            GoldButton(
                if (busy && apps.isEmpty()) "جاري الفحص…" else "١. افحص التطبيقات",
                Modifier.fillMaxWidth(), enabled = !busy && host.count { it == '.' } == 3
            ) {
                work({ inst, tv -> inst.scanApps(tv) }) { list ->
                    apps.clear(); apps.addAll(list)
                    off.clear(); off.addAll(list.filter { it.disabled || it.suggested }.map { it.pkg })
                }
            }

            if (apps.isNotEmpty()) {
                Text("اختر اللي تبي تعطّله", color = C.gold, fontWeight = FontWeight.Medium)
                Text("المعلَّم مسبقًا آمن تعطيله. الباقي حسب استخدامك.", color = C.faint, fontSize = 12.sp)
                Card {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        apps.forEach { a ->
                            val on = a.pkg in off
                            Row(
                                Modifier.fillMaxWidth().clickable { if (on) off.remove(a.pkg) else off.add(a.pkg) }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(22.dp)
                                        .background(if (on) C.gold else Color.Transparent, RoundedCornerShape(6.dp))
                                        .border(1.5.dp, if (on) C.gold else C.faint, RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) { if (on) Text("✓", color = C.dark, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(a.label + if (a.disabled) "  (معطّل حاليًا)" else "", color = Color.White, fontSize = 15.sp)
                                    if (a.note.isNotEmpty()) Text(a.note, color = C.faint, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                GoldButton(if (busy) "جاري التطبيق…" else "٢. طبّق التسريع", Modifier.fillMaxWidth(), enabled = !busy) {
                    val list = apps.toList(); val chosen = off.toSet()
                    work({ inst, tv -> inst.speedUp(tv, list, chosen); inst.scanApps(tv) }) { fresh ->
                        apps.clear(); apps.addAll(fresh)
                        off.clear(); off.addAll(fresh.filter { it.disabled }.map { it.pkg })
                        message = "تم ✓ الحركات صارت أسرع، والتطبيقات المختارة تعطّلت"
                    }
                }
            }

            Card {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("خطوة يدوية (مرة وحدة):", color = C.gold, fontWeight = FontWeight.Medium)
                    Text("Google Play في التلفزيون ← الإعدادات ← تحديث التطبيقات تلقائيًا ← «عدم التحديث تلقائيًا»", color = C.dim, fontSize = 13.sp)
                    Text("وأعد تشغيل التلفزيون من الكهرباء مرة بالأسبوع.", color = C.dim, fontSize = 13.sp)
                }
            }
            if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = C.gold, strokeWidth = 2.dp)
            message?.let {
                Text(
                    it, Modifier.fillMaxWidth().background(Color(0x22FFFFFF), RoundedCornerShape(14.dp)).padding(14.dp),
                    color = if (it.startsWith("تم")) C.green else Color(0xFFFFE2A8), fontSize = 14.sp
                )
            }
            if (logs.isNotEmpty()) {
                Text("التفاصيل", color = C.faint, fontSize = 12.sp)
                Text(logs.takeLast(30).joinToString("\n"), color = C.faint, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}
