package om.yousef.shashati.remote.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
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

private enum class St { Wait, Run, Ok, Fail, Skip }

/** Installs or updates شاشتي on a TV from the phone (no laptop needed). */
@Composable
fun InstallScreen(onClose: () -> Unit) {
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
    var copyFrom by remember { mutableStateOf<String?>(null) }
    var running by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    val steps = remember { mutableStateListOf(St.Wait, St.Wait, St.Wait, St.Wait, St.Wait) }
    val titles = listOf("تنزيل آخر نسخة من شاشتي", "الاتصال بالتلفزيون", "التثبيت", "تفعيل زر Home والأذان وشاشة التوقف", "نسخ الشعارات والأذكار")
    var message by remember { mutableStateOf<String?>(null) }
    val logs = remember { mutableStateListOf<String>() }

    fun run() {
        val target = host.trim()
        running = true
        message = null
        logs.clear()
        for (i in steps.indices) steps[i] = St.Wait
        scope.launch {
            val inst = TvInstaller(context) { line -> scope.launch { logs.add(line) } }
            var step = 0
            try {
                steps[0] = St.Run
                val apk = withContext(Dispatchers.IO) { inst.download { p -> scope.launch { progress = p } } }
                steps[0] = St.Ok

                step = 1; steps[1] = St.Run
                message = "على شاشة التلفزيون: إذا طلع «Allow USB debugging?» اختر Always allow ثم OK"
                val tv = withContext(Dispatchers.IO) { inst.connect(target) }
                message = null
                steps[1] = St.Ok
                try {
                    step = 2; steps[2] = St.Run
                    withContext(Dispatchers.IO) { inst.install(tv, apk) }
                    steps[2] = St.Ok

                    step = 3; steps[3] = St.Run
                    withContext(Dispatchers.IO) { inst.setup(tv) }
                    steps[3] = St.Ok

                    step = 4
                    val src = copyFrom
                    if (src == null) steps[4] = St.Skip else {
                        steps[4] = St.Run
                        withContext(Dispatchers.IO) {
                            val from = inst.connect(src)
                            try { inst.copyFiles(from, tv) } finally { runCatching { from.close() } }
                        }
                        steps[4] = St.Ok
                    }
                    withContext(Dispatchers.IO) { inst.start(tv) }
                    message = "تم ✓ شاشتي جاهزة على التلفزيون"
                } finally {
                    withContext(Dispatchers.IO) { runCatching { tv.close() } }
                }
            } catch (e: Exception) {
                steps[step] = St.Fail
                message = when (step) {
                    0 -> "ما قدرت أنزّل شاشتي. تأكد من الإنترنت."
                    1 -> "ما قدرت أوصل للتلفزيون. تأكد إن USB debugging مفعّل، وإن الجوال والتلفزيون على نفس الواي فاي، ووافقت على الرسالة في التلفزيون."
                    else -> "صار خطأ: ${e.message ?: e.javaClass.simpleName}"
                }
                logs.add(e.toString())
            } finally {
                running = false
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Background()
        Column(
            Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("تثبيت شاشتي", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButtonGlass("رجوع") { if (!running) onClose() }
            }
            Card {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("قبل البدء، على التلفزيون:", color = C.gold, fontWeight = FontWeight.Medium)
                    Text("١. الإعدادات ← About ← اضغط Android TV OS build سبع مرات", color = C.dim, fontSize = 13.sp)
                    Text("٢. Developer options ← شغّل USB debugging", color = C.dim, fontSize = 13.sp)
                    Text("٣. الجوال والتلفزيون على نفس الواي فاي", color = C.dim, fontSize = 13.sp)
                    Text("ينفع للتثبيت لأول مرة وللتحديث، والإعدادات تبقى.", color = C.faint, fontSize = 12.sp)
                }
            }

            Text("التلفزيون", color = C.gold, fontWeight = FontWeight.Medium)
            if (choices.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    choices.forEach { (h, n) -> Pill("$n · $h", host == h) { host = h } }
                }
            }
            Field(
                host, { host = it.filter { c -> c.isDigit() || c == '.' } }, "عنوان التلفزيون، مثال 192.168.100.17",
                Modifier.fillMaxWidth(), KeyboardOptions(keyboardType = KeyboardType.Decimal), ltr = true
            )

            Text("نسخ الشعارات وتسجيلات الأذكار من تلفزيون ثاني (اختياري)", color = C.gold, fontWeight = FontWeight.Medium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("بدون", copyFrom == null) { copyFrom = null }
                choices.filter { it.first != host }.forEach { (h, n) -> Pill("من $n · $h", copyFrom == h) { copyFrom = h } }
            }

            GoldButton(
                if (running) "جاري التثبيت…" else "ابدأ التثبيت",
                Modifier.fillMaxWidth(),
                enabled = !running && host.count { it == '.' } == 3
            ) { run() }

            Card {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    titles.forEachIndexed { i, t ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when (steps[i]) {
                                St.Run -> CircularProgressIndicator(Modifier.size(18.dp), color = C.gold, strokeWidth = 2.dp)
                                else -> Box(
                                    Modifier.size(18.dp).background(
                                        when (steps[i]) { St.Ok -> C.green; St.Fail -> C.red; St.Skip -> C.faint; else -> Color(0x33FFFFFF) },
                                        CircleShape
                                    )
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                t + if (i == 0 && steps[0] == St.Run) " ($progress%)" else if (steps[i] == St.Skip) " (تم التخطي)" else "",
                                color = if (steps[i] == St.Wait) C.faint else Color.White, fontSize = 15.sp
                            )
                        }
                    }
                }
            }
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
