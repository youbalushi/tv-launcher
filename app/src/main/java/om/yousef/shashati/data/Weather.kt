package om.yousef.shashati.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WeatherNow(val temp: Int, val code: Int, val isDay: Boolean, val place: String) {
    /** Short Arabic description of the WMO weather code. */
    val condition: String
        get() = when (code) {
            0 -> if (isDay) "مشمس" else "صافٍ"
            1, 2 -> "غائم جزئياً"
            3 -> "غائم"
            45, 48 -> "ضباب"
            in 51..57 -> "رذاذ"
            in 61..67, in 80..82 -> "أمطار"
            in 71..77, 85, 86 -> "ثلوج"
            in 95..99 -> "عواصف رعدية"
            else -> ""
        }
}

/** Current weather for the selected wilaya from Open-Meteo (free, no key). */
object WeatherRepository {
    private const val MAX_AGE = 30 * 60_000L

    fun cached(): WeatherNow? {
        val raw = Prefs.weatherJson ?: return null
        return runCatching {
            val o = JSONObject(raw)
            if (o.optString("place") != Locations.current().name) return null
            WeatherNow(o.getInt("temp"), o.getInt("code"), o.getBoolean("day"), o.getString("place"))
        }.getOrNull()
    }

    suspend fun refreshIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        val w = Locations.current()
        if (cached() != null && System.currentTimeMillis() - Prefs.weatherAt < MAX_AGE) return@withContext false
        runCatching {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast?latitude=${w.lat}&longitude=${w.lng}" +
                    "&current=temperature_2m,weather_code,is_day&timezone=auto"
            )
            val c = url.openConnection() as HttpURLConnection
            c.connectTimeout = 10_000
            c.readTimeout = 10_000
            val text = c.inputStream.bufferedReader().use { it.readText() }
            c.disconnect()
            val cur = JSONObject(text).getJSONObject("current")
            val now = WeatherNow(
                Math.round(cur.getDouble("temperature_2m")).toInt(),
                cur.getInt("weather_code"),
                cur.optInt("is_day", 1) == 1,
                w.name,
            )
            Prefs.weatherJson = JSONObject().put("temp", now.temp).put("code", now.code).put("day", now.isDay).put("place", now.place).toString()
            Prefs.weatherAt = System.currentTimeMillis()
            true
        }.getOrDefault(false)
    }
}
