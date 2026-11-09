package om.yousef.shashati.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import om.yousef.shashati.data.AppsRepository
import om.yousef.shashati.data.Locations
import om.yousef.shashati.data.Prefs
import om.yousef.shashati.overlay.AlarmScheduler
import om.yousef.shashati.overlay.OverlayNotifier
import om.yousef.shashati.prayer.Dates
import om.yousef.shashati.prayer.Methods
import om.yousef.shashati.prayer.PRAYER_NAMES
import om.yousef.shashati.prayer.PrayerRepository
import java.time.LocalDate

private val SECTIONS = listOf(
    "الشاشة الرئيسية",
    "بطاقات الرئيسية",
    "موقع وأوقات الصلاة",
    "الأذان والإقامة",
    "شاشة التوقف",
    "الأطفال وقفل التطبيقات",
    "الواجهة الأصلية والرقم السري",
)

@Composable
fun SettingsScreen(initialSection: Int, onBack: () -> Unit, onChanged: () -> Unit, onPreview: () -> Unit = {}, onStartKids: () -> Unit = {}) {
    var unlocked by remember { mutableStateOf(Prefs.hasPin && !Prefs.askPin) }
    var section by remember { mutableIntStateOf(initialSection) }
    var sub by remember { mutableStateOf<String?>(null) } // overlays inside a section

    BackHandler {
        if (sub != null) sub = null else onBack()
    }

    Box(Modifier.fillMaxSize()) {
        AppBackground()
        Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Focusable(modifier = Modifier.size(34.dp), shape = CircleShape, onClick = onBack) {
                    Text("›", fontSize = 22.sp, modifier = Modifier.align(Alignment.Center))
                }
                Text("الإعدادات", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(18.dp))

            when {
                !Prefs.hasPin -> PinSetup(askPin = true, askRecovery = true, onDone = { unlocked = true })
                !unlocked -> PinGate("الإعدادات محمية") { recovered ->
                    unlocked = true
                    if (recovered) { section = 6; sub = "changePin" }
                }
                else -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.width(220.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SECTIONS.forEachIndexed { i, name ->
                            Focusable(
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                background = if (section == i) Color.White else C.glass,
                                scaleOnFocus = 1.03f,
                                onClick = { section = i; sub = null }
                            ) {
                                Text(
                                    name, fontSize = 14.sp,
                                    fontWeight = if (section == i) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (section == i) C.dark else Color.White,
                                    modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                    Box(
                        Modifier.weight(1f).fillMaxSize()
                            .background(Color(0x0FFFFFFF), RoundedCornerShape(18.dp))
                            .padding(horizontal = 26.dp, vertical = 20.dp)
                    ) {
                        when (section) {
                            0 -> HomeSection(onChanged)
                            1 -> FavoritesSection(onChanged)
                            2 -> PrayerSection(onChanged)
                            3 -> AdhanSection()
                            4 -> SaverSection(onPreview)
                            5 -> KidsLockSection(onStartKids)
                            else -> SecuritySection(sub, { sub = it }, onChanged)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, hint: String? = null, value: Boolean, onToggle: () -> Unit) {
    Focusable(
        modifier = Modifier.fillMaxWidth().height(if (hint != null) 64.dp else 52.dp),
        background = Color(0x0FFFFFFF),
        scaleOnFocus = 1.02f,
        onClick = onToggle
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (hint != null) Text(hint, fontSize = 11.sp, color = C.dim)
            }
            Box(
                Modifier.size(52.dp, 28.dp).background(if (value) C.gold else Color(0x38FFFFFF), RoundedCornerShape(50)),
                contentAlignment = if (value) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Box(Modifier.padding(3.dp).size(22.dp).background(Color.White, CircleShape))
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun HomeSection(onChanged: () -> Unit) {
    var bar by remember { mutableStateOf(Prefs.showPrayerBar) }
    var card by remember { mutableStateOf(Prefs.showNextCard) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("الشاشة الرئيسية")
        ToggleRow("شريط أوقات الصلاة", "الصلوات الخمس في الشاشة الرئيسية", bar) {
            bar = !bar; Prefs.showPrayerBar = bar; onChanged()
        }
        ToggleRow("العد التنازلي للصلاة القادمة", "داخل شريط الصلاة أسفل الشاشة", card) {
            card = !card; Prefs.showNextCard = card; onChanged()
        }
    }
}

@Composable
private fun SmallButton(text: String, danger: Boolean = false, onClick: () -> Unit) {
    Focusable(
        modifier = Modifier.height(34.dp),
        shape = RoundedCornerShape(9.dp),
        background = if (danger) C.danger else Color(0x1AFFFFFF),
        scaleOnFocus = 1.08f,
        onClick = onClick
    ) {
        Text(
            text, fontSize = 12.sp, color = if (danger) Color(0xFFFFB3B3) else Color.White,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun FavoritesSection(onChanged: () -> Unit) {
    val context = LocalContext.current
    val installed = remember { AppsRepository.installed(context) }
    val allChannels = remember { om.yousef.shashati.data.ChannelRepository.load(context).channels }
    var chIds by remember { mutableStateOf(om.yousef.shashati.data.HomeTiles.channels(context).map { it.id }) }
    var appIds by remember { mutableStateOf(om.yousef.shashati.data.HomeTiles.apps(context).map { it.packageName }) }
    // what is being replaced: "ch" or "app" with the slot index
    var picking by remember { mutableStateOf<Pair<String, Int>?>(null) }

    fun chName(id: String) = allChannels.firstOrNull { it.id == id }?.name ?: id
    fun appName(p: String) = installed.firstOrNull { it.packageName == p }?.label ?: p

    val pick = picking
    if (pick != null) {
        val (kind, slot) = pick
        val options: List<Pair<String, String>> =
            if (kind == "ch") allChannels.map { it.id to it.name } else installed.map { it.packageName to it.label }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(if (kind == "ch") "اختر القناة رقم ${slot + 1}" else "اختر التطبيق رقم ${slot + 1}")
            val firstOption = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                delay(100)
                runCatching { firstOption.requestFocus() }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(options.size) { idx ->
                    val (id, name) = options[idx]
                    Focusable(modifier = Modifier.fillMaxWidth().height(44.dp), focusRequester = if (idx == 0) firstOption else null, onClick = {
                        if (kind == "ch") {
                            val l = chIds.toMutableList()
                            if (slot < l.size) l[slot] = id else l.add(id)
                            chIds = l.distinct()
                            om.yousef.shashati.data.HomeTiles.saveChannels(chIds)
                        } else {
                            val l = appIds.toMutableList()
                            if (slot < l.size) l[slot] = id else l.add(id)
                            appIds = l.distinct()
                            om.yousef.shashati.data.HomeTiles.saveApps(appIds)
                        }
                        picking = null
                        onChanged()
                    }) {
                        Text(
                            name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.Center).padding(horizontal = 10.dp)
                        )
                    }
                }
            }
        }
        BackHandler { picking = null }
        return
    }

    @Composable
    fun slots(kind: String, ids: List<String>, name: (String) -> String, save: (List<String>) -> Unit) {
        for (i in 0 until om.yousef.shashati.data.HomeTiles.SLOTS) {
            val id = ids.getOrNull(i)
            Row(
                Modifier.fillMaxWidth().background(Color(0x0FFFFFFF), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("${i + 1}", fontSize = 13.sp, color = C.faint, modifier = Modifier.width(20.dp))
                Text(id?.let(name) ?: "فارغ", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (id == null) C.faint else Color.White,
                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (id != null) {
                    SmallButton("▲") { if (i > 0) save(ids.toMutableList().apply { add(i - 1, removeAt(i)) }) }
                    SmallButton("▼") { if (i < ids.size - 1) save(ids.toMutableList().apply { add(i + 1, removeAt(i)) }) }
                }
                SmallButton(if (id == null) "اختيار" else "استبدال") { picking = kind to i }
            }
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("بطاقات الشاشة الرئيسية")
        Text("الترتيب من اليمين. بطاقتا «جميع القنوات» و«جميع التطبيقات» ثابتتان على اليسار.", fontSize = 12.sp, color = C.dim)
        Text("صف القنوات", fontSize = 14.sp, color = C.gold, modifier = Modifier.padding(top = 4.dp))
        slots("ch", chIds, ::chName) { chIds = it; om.yousef.shashati.data.HomeTiles.saveChannels(it); onChanged() }
        Text("صف التطبيقات", fontSize = 14.sp, color = C.gold, modifier = Modifier.padding(top = 6.dp))
        slots("app", appIds, ::appName) { appIds = it; om.yousef.shashati.data.HomeTiles.saveApps(it); onChanged() }
    }
}

@Composable
private fun Stepper(name: String, value: Int, suffix: String = "", modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    Column(
        modifier.background(Color(0x0FFFFFFF), RoundedCornerShape(12.dp)).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(name, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallButton("−") { onChange(value - 1) }
            Text("$value$suffix", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            SmallButton("+") { onChange(value + 1) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrayerSection(onChanged: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var gov by remember { mutableIntStateOf(Prefs.govIndex) }
    var wil by remember { mutableIntStateOf(Prefs.wilIndex) }
    var method by remember { mutableIntStateOf(Prefs.method) }
    var offsets by remember { mutableStateOf(Prefs.offsets) }
    var status by remember { mutableStateOf("") }

    fun refresh() {
        status = "جارٍ تحديث الأوقات…"
        scope.launch {
            val ok = PrayerRepository.refreshIfNeeded(force = true)
            AlarmScheduler.scheduleNext(context)
            onChanged()
            status = if (ok) "تم التحديث من الإنترنت" else "تعذّر الاتصال؛ تُستخدم الأوقات المحسوبة على الجهاز"
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("موقع وأوقات الصلاة")
        Text("المحافظة", fontSize = 13.sp, color = C.dim)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Locations.governorates.forEachIndexed { i, g ->
                Pill(g.name, selected = gov == i, fontSize = 13) {
                    gov = i; wil = 0
                    Prefs.govIndex = i; Prefs.wilIndex = 0
                    refresh()
                }
            }
        }
        Text("الولاية", fontSize = 13.sp, color = C.dim)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Locations.governorates[gov].wilayat.forEachIndexed { i, w ->
                Pill(w.name, selected = wil == i, fontSize = 13) {
                    wil = i; Prefs.wilIndex = i
                    refresh()
                }
            }
        }
        Text("طريقة الحساب", fontSize = 13.sp, color = C.dim)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Methods.all.forEach { m ->
                Pill(m.name, selected = method == m.id, fontSize = 13) {
                    method = m.id; Prefs.method = m.id
                    refresh()
                }
            }
        }
        Text("تعديل يدوي بالدقائق (لمطابقة تقويم وزارة الأوقاف)", fontSize = 13.sp, color = C.dim)
        val today = remember(offsets, gov, wil, method, status) { PrayerRepository.timesFor(LocalDate.now()) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0 until 5) {
                Stepper(
                    "${PRAYER_NAMES[i]} ${Dates.time12(today[i])}",
                    offsets[i],
                    modifier = Modifier.weight(1f)
                ) { v ->
                    val list = offsets.toMutableList().apply { this[i] = v.coerceIn(-30, 30) }
                    offsets = list
                    Prefs.offsets = list
                    AlarmScheduler.scheduleNext(context)
                    onChanged()
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Pill("تحديث الأوقات الآن", selected = false, fontSize = 13) { refresh() }
            Text(
                status.ifEmpty {
                    if (method == Methods.OMAN) "المعادلة العمانية: تُحسب على الجهاز بطريقة تقويم وزارة الأوقاف"
                    else if (PrayerRepository.isOnlineSource()) "المصدر: الإنترنت (يُحدَّث يوميًا)" else "المصدر: حساب على الجهاز"
                },
                fontSize = 12.sp, color = C.dim
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdhanSection() {
    val context = LocalContext.current
    var adhan by remember { mutableStateOf(Prefs.adhanOn) }
    var iqama by remember { mutableStateOf(Prefs.iqamaOn) }
    var minutes by remember { mutableStateOf(Prefs.iqamaMinutes) }
    var pos by remember { mutableIntStateOf(Prefs.notifPos) }
    var secs by remember { mutableIntStateOf(Prefs.notifSeconds) }
    var permission by remember { mutableStateOf(OverlayNotifier.canShow(context)) }

    fun reschedule() = AlarmScheduler.scheduleNext(context)

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("الأذان والإقامة")
        if (!permission) {
            Column(
                Modifier.fillMaxWidth().background(C.danger, RoundedCornerShape(12.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("الإشعارات تحتاج صلاحية «الظهور فوق التطبيقات»", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("تُمنح مرة واحدة من اللابتوب بالأمر الموجود في دليل التثبيت.", fontSize = 12.sp, color = C.dim)
                Pill("محاولة فتح إعداد الصلاحية", selected = false, fontSize = 12) {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    permission = OverlayNotifier.canShow(context)
                }
            }
        }
        ToggleRow("إشعار الأذان", value = adhan) { adhan = !adhan; Prefs.adhanOn = adhan; reschedule() }
        ToggleRow("إشعار الإقامة", value = iqama) { iqama = !iqama; Prefs.iqamaOn = iqama; reschedule() }
        Text("وقت الإقامة بعد الأذان (بالدقائق)", fontSize = 13.sp, color = C.dim)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0 until 5) {
                Stepper(PRAYER_NAMES[i], minutes[i], modifier = Modifier.weight(1f)) { v ->
                    val list = minutes.toMutableList().apply { this[i] = v.coerceIn(0, 60) }
                    minutes = list; Prefs.iqamaMinutes = list; reschedule()
                }
            }
        }
        Text("مكان الإشعار", fontSize = 13.sp, color = C.dim)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("أعلى اليسار", "أعلى اليمين", "أسفل اليسار", "أسفل اليمين").forEachIndexed { i, n ->
                Pill(n, selected = pos == i, fontSize = 13) { pos = i; Prefs.notifPos = i }
            }
        }
        Text("مدة ظهور الإشعار", fontSize = 13.sp, color = C.dim)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 15, 30).forEach { s ->
                Pill("$s ثانية", selected = secs == s, fontSize = 13) { secs = s; Prefs.notifSeconds = s }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill("تجربة إشعار الأذان", selected = false, fontSize = 13) {
                permission = OverlayNotifier.canShow(context)
                OverlayNotifier.show(context, PrayerRepository.next().index, false, null) {}
            }
            Pill("تجربة إشعار الإقامة", selected = false, fontSize = 13) {
                permission = OverlayNotifier.canShow(context)
                OverlayNotifier.show(context, PrayerRepository.next().index, true, null) {}
            }
        }
    }
}

@Composable
private fun SecuritySection(sub: String?, setSub: (String?) -> Unit, onChanged: () -> Unit) {
    val context = LocalContext.current
    var ask by remember { mutableStateOf(Prefs.askPin) }
    var message by remember { mutableStateOf("") }

    when (sub) {
        "exit" -> {
            PinGate("الخروج للواجهة الأصلية") {
                setSub(null)
                if (!AppsRepository.openOriginalLauncher(context)) message = "لم أجد الواجهة الأصلية على هذا التلفزيون"
            }
            return
        }
        "changePin" -> {
            PinSetup(askPin = true, askRecovery = false, onDone = { setSub(null); message = "تم تغيير الرقم السري" }, onCancel = { setSub(null) })
            return
        }
        "changeRecovery" -> {
            PinSetup(askPin = false, askRecovery = true, onDone = { setSub(null); message = "تم حفظ رمز الاسترداد" }, onCancel = { setSub(null) })
            return
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("الواجهة الأصلية والرقم السري")
        Focusable(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            background = Color(0x1AFFFFFF),
            scaleOnFocus = 1.02f,
            onClick = { setSub("exit") }
        ) {
            Column(Modifier.align(Alignment.CenterStart).padding(horizontal = 18.dp)) {
                Text("الخروج لواجهة التلفزيون الأصلية", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("يُطلب الرقم السري · للرجوع اضغط زر Home في الريموت", fontSize = 11.sp, color = C.dim)
            }
        }
        ToggleRow("طلب الرقم السري عند فتح الإعدادات", "يمنع الأطفال من تغيير الواجهة أو حذف البطاقات", ask) {
            ask = !ask; Prefs.askPin = ask; onChanged()
        }
        Focusable(modifier = Modifier.fillMaxWidth().height(52.dp), background = Color(0x0FFFFFFF), scaleOnFocus = 1.02f, onClick = { setSub("changePin") }) {
            Text("تغيير الرقم السري", fontSize = 15.sp, modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 18.dp))
        }
        Focusable(modifier = Modifier.fillMaxWidth().height(52.dp), background = Color(0x0FFFFFFF), scaleOnFocus = 1.02f, onClick = { setSub("changeRecovery") }) {
            Text("تغيير رمز الاسترداد (4 إلى 10 أرقام)", fontSize = 15.sp, modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 18.dp))
        }
        if (message.isNotEmpty()) Text(message, fontSize = 13.sp, color = C.gold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SaverSection(onPreview: () -> Unit) {
    val context = LocalContext.current
    var night by remember { mutableStateOf(Prefs.saverNight) }
    var hijri by remember { mutableStateOf(Prefs.saverHijri) }
    var dhikr by remember { mutableStateOf(Prefs.saverDhikr) }
    var prayers by remember { mutableStateOf(Prefs.saverPrayers) }
    var minutes by remember { mutableIntStateOf(Prefs.saverMinutes) }
    val canWrite = remember { Settings.System.canWrite(context) }
    var note by remember { mutableStateOf("") }

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("شاشة التوقف")
        Text("تظهر بعد عدم استخدام التلفزيون لمدة", fontSize = 13.sp, color = C.dim)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 15, 30).forEach { m ->
                Pill("$m دقيقة", selected = minutes == m, fontSize = 13) {
                    minutes = m
                    Prefs.saverMinutes = m
                    note = if (applySaverTimeout(context, m)) "تم ضبط المدة ✓"
                    else "لتطبيق المدة يلزم تشغيل ملف التثبيت مرة واحدة من اللابتوب"
                }
            }
        }
        if (note.isNotEmpty()) Text(note, fontSize = 12.sp, color = C.gold)
        if (!canWrite) Text("صلاحية ضبط مدة الانتظار تُمنح من ملف التثبيت على اللابتوب.", fontSize = 11.sp, color = C.faint)
        Text("الخلفية", fontSize = 13.sp, color = C.dim)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("ليل", selected = night, fontSize = 13) { night = true; Prefs.saverNight = true }
            Pill("غروب", selected = !night, fontSize = 13) { night = false; Prefs.saverNight = false }
        }
        ToggleRow("التاريخ الهجري", value = hijri) { hijri = !hijri; Prefs.saverHijri = hijri }
        ToggleRow("الأذكار", value = dhikr) { dhikr = !dhikr; Prefs.saverDhikr = dhikr }
        ToggleRow("أوقات الصلاة", value = prayers) { prayers = !prayers; Prefs.saverPrayers = prayers }
        Pill("معاينة شاشة التوقف", selected = true, fontSize = 13, hPad = 18.dp) { onPreview() }
    }
}

/** Sets how long the TV waits before starting the screensaver. Needs the WRITE_SETTINGS grant. */
fun applySaverTimeout(context: android.content.Context, minutes: Int): Boolean = runCatching {
    if (!Settings.System.canWrite(context)) return false
    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, minutes * 60_000)
}.getOrDefault(false)

@Composable
private fun KidsLockSection(onStartKids: () -> Unit) {
    val context = LocalContext.current
    val apps = remember { AppsRepository.installed(context) }
    var kids by remember { mutableStateOf(Prefs.kidsApps()) }
    var locked by remember { mutableStateOf(Prefs.lockedApps()) }

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionHeader("الأطفال وقفل التطبيقات")
            Pill("تشغيل عالم الأطفال الآن", selected = true, fontSize = 13, hPad = 16.dp) {
                Prefs.kidsMode = true
                onStartKids()
            }
        }
        Text(
            "«للأطفال»: يظهر في عالم الأطفال، وأي تطبيق غيره يُغلق تلقائيًا.  «قفل»: يُطلب الرقم السري لفتحه.",
            fontSize = 12.sp, color = C.dim
        )
        Text(
            "ألعاب مجانية تعمل بالريموت تناسب الأطفال: Crossy Road · Beach Buggy Racing · PAC-MAN · Orbia · BADLAND (ثبّتها من المتجر ثم اسمح بها هنا).",
            fontSize = 11.sp, color = C.faint
        )
        apps.forEach { app ->
            val forKids = app.packageName in kids
            val isLocked = app.packageName in locked
            Row(
                Modifier.fillMaxWidth().background(Color(0x0FFFFFFF), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(app.label, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Pill(if (forKids) "للأطفال ✓" else "للأطفال", selected = forKids, fontSize = 12) {
                    kids = if (forKids) kids - app.packageName else kids + app.packageName
                    Prefs.setKidsApps(kids)
                }
                Pill(if (isLocked) "مقفل 🔒" else "قفل", selected = isLocked, fontSize = 12) {
                    locked = if (isLocked) locked - app.packageName else locked + app.packageName
                    Prefs.setLockedApps(locked)
                }
            }
        }
    }
}
