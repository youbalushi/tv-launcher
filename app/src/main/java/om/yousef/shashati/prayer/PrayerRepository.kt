package om.yousef.shashati.prayer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import om.yousef.shashati.data.Locations
import om.yousef.shashati.data.Prefs
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

val PRAYER_NAMES = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")

data class PrayerEvent(val index: Int, val time: LocalDateTime)

/**
 * Prayer times come from the AlAdhan service (refreshed daily, two months cached),
 * with an offline calculation as fallback. Manual minute offsets are applied on top.
 */
object PrayerRepository {
    private val dayKey = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val apiDay = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    private const val TZ = 4.0 // Oman, no daylight saving

    private fun cacheKey(): String {
        val w = Locations.current()
        return "${w.lat},${w.lng},${Prefs.method}"
    }

    /** Raw times without offsets: online cache if available for this place/method, else calculated. */
    private fun baseTimes(date: LocalDate): List<LocalTime> {
        cachedDay(date)?.let { return it }
        val w = Locations.current()
        return PrayerCalc.compute(date, w.lat, w.lng, TZ, Methods.byId(Prefs.method))
    }

    private fun cachedDay(date: LocalDate): List<LocalTime>? = runCatching {
        val root = JSONObject(Prefs.prayerCache ?: return null)
        if (root.getString("key") != cacheKey()) return null
        val arr = root.getJSONObject("days").optJSONArray(date.format(dayKey)) ?: return null
        (0 until 5).map { LocalTime.parse(arr.getString(it)) }
    }.getOrNull()

    fun timesFor(date: LocalDate): List<LocalTime> {
        val offs = Prefs.offsets
        return baseTimes(date).mapIndexed { i, t -> t.plusMinutes(offs[i].toLong()) }
    }

    fun isOnlineSource(date: LocalDate = LocalDate.now()) = cachedDay(date) != null

    fun eventsAround(now: LocalDateTime): List<PrayerEvent> {
        val today = now.toLocalDate()
        return listOf(today.minusDays(1), today, today.plusDays(1)).flatMap { d ->
            timesFor(d).mapIndexed { i, t -> PrayerEvent(i, LocalDateTime.of(d, t)) }
        }
    }

    fun next(now: LocalDateTime = LocalDateTime.now()): PrayerEvent =
        eventsAround(now).first { it.time.isAfter(now) }

    fun previous(now: LocalDateTime = LocalDateTime.now()): PrayerEvent =
        eventsAround(now).last { !it.time.isAfter(now) }

    /** Fetches this month and next from the internet once per day (or when forced). */
    suspend fun refreshIfNeeded(force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val today = LocalDate.now().format(dayKey)
        val sameKey = runCatching { JSONObject(Prefs.prayerCache ?: "{}").optString("key") == cacheKey() }.getOrDefault(false)
        if (!force && Prefs.prayerFetchedDay == today && sameKey) return@withContext true
        runCatching {
            val w = Locations.current()
            val days = JSONObject()
            val month = YearMonth.now()
            for (ym in listOf(month, month.plusMonths(1))) {
                val url = "https://api.aladhan.com/v1/calendar/${ym.year}/${ym.monthValue}" +
                    "?latitude=${w.lat}&longitude=${w.lng}&method=${Prefs.method}&school=0" +
                    "&timezonestring=Asia/Muscat"
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 15_000
                conn.readTimeout = 15_000
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val data = JSONObject(text).getJSONArray("data")
                for (i in 0 until data.length()) {
                    val item = data.getJSONObject(i)
                    val t = item.getJSONObject("timings")
                    val date = LocalDate.parse(item.getJSONObject("date").getJSONObject("gregorian").getString("date"), apiDay)
                    val arr = JSONArray()
                    for (name in listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")) arr.put(t.getString(name).take(5))
                    days.put(date.format(dayKey), arr)
                }
            }
            Prefs.prayerCache = JSONObject().put("key", cacheKey()).put("days", days).toString()
            Prefs.prayerFetchedDay = today
            true
        }.getOrDefault(false)
    }
}
