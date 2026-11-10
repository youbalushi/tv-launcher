package om.yousef.shashati.prayer

import android.icu.util.IslamicCalendar
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object Dates {
    private val hijriMonths = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )
    private val gregMonths = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )
    private val days = listOf("الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")

    fun gregorian(d: LocalDate) = "${days[d.dayOfWeek.value - 1]} ${d.dayOfMonth} ${gregMonths[d.monthValue - 1]}"

    fun gregorianFull(d: LocalDate) = "${gregorian(d)} ${d.year}"

    fun hijri(d: LocalDate): String = runCatching {
        val cal = IslamicCalendar()
        cal.calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
        cal.timeInMillis = d.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val day = cal.get(android.icu.util.Calendar.DAY_OF_MONTH)
        val month = cal.get(android.icu.util.Calendar.MONTH)
        val year = cal.get(android.icu.util.Calendar.YEAR)
        "$day ${hijriMonths[month]} $year"
    }.getOrDefault("")

    /** 12-hour clock with ص / م. */
    fun time12(t: LocalTime, withSuffix: Boolean = false): String {
        val h = if (t.hour % 12 == 0) 12 else t.hour % 12
        val base = String.format(java.util.Locale.US, "%d:%02d", h, t.minute)
        return if (withSuffix) "$base ${if (t.hour < 12) "ص" else "م"}" else base
    }

    fun greeting(t: LocalTime) = if (t.hour in 4..11) "صباح الخير" else "مساء الخير"

    /** "4 ساعة و 3 دقائق" style text without the leading "بعد". */
    fun duration(totalMinutes: Long): String {
        val m = totalMinutes.coerceAtLeast(0)
        if (m < 1) return "الآن"
        fun mins(x: Long) = when { x == 1L -> "دقيقة"; x == 2L -> "دقيقتان"; x in 3..10 -> "$x دقائق"; else -> "$x دقيقة" }
        if (m < 60) return mins(m)
        val h = m / 60
        val r = m % 60
        val hours = when (h) { 1L -> "ساعة"; 2L -> "ساعتان"; in 3..10 -> "$h ساعات"; else -> "$h ساعة" }
        return if (r == 0L) hours else "$hours و ${mins(r)}"
    }

    private val adhkar = listOf(
        "سبحان الله وبحمده، سبحان الله العظيم",
        "لا إله إلا الله وحده لا شريك له",
        "اللهم صلِّ وسلم على نبينا محمد",
        "أستغفر الله العظيم وأتوب إليه",
        "لا حول ولا قوة إلا بالله",
        "الحمد لله رب العالمين",
        "حسبي الله لا إله إلا هو عليه توكلت",
        "سبحان الله، والحمد لله، ولا إله إلا الله، والله أكبر",
        "رضيت بالله ربًّا، وبالإسلام دينًا، وبمحمد ﷺ نبيًّا",
        "اللهم إنك عفو تحب العفو فاعفُ عنا",
        "ربنا آتنا في الدنيا حسنة وفي الآخرة حسنة",
        "لا إله إلا أنت سبحانك إني كنت من الظالمين",
        "يا حي يا قيوم برحمتك أستغيث",
        "اللهم أعنّي على ذكرك وشكرك وحسن عبادتك",
        "سبحان الله وبحمده عدد خلقه ورضا نفسه",
        "رب اغفر لي ولوالديّ",
        "اللهم إني أسألك الهدى والتقى والعفاف والغنى",
        "توكلت على الله",
        "الله أكبر كبيرًا، والحمد لله كثيرًا",
        "اللهم اجعل القرآن ربيع قلوبنا",
    )

    /** A different dhikr every 15 minutes. */
    fun dhikr(t: java.time.LocalDateTime): String {
        val slot = t.toLocalDate().toEpochDay() * 96 + t.hour * 4 + t.minute / 15
        return adhkar[(slot % adhkar.size).toInt()]
    }

    fun dhikr(d: LocalDate) = dhikr(d.atStartOfDay())
}
