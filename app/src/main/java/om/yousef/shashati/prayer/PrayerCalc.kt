package om.yousef.shashati.prayer

import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/** Calculation method: Fajr angle, and Isha as an angle or a fixed number of minutes after Maghrib. */
data class Method(
    val id: Int,
    val name: String,
    val fajr: Double,
    val ishaAngle: Double?,
    val ishaMinutes: Int?,
    /** Fixed precaution minutes added to Fajr, Dhuhr, Asr, Maghrib, Isha. */
    val tamkin: List<Int> = listOf(0, 0, 0, 0, 0),
    /** Round up to the next minute (as the Omani calendar does) instead of to the nearest. */
    val roundUp: Boolean = false,
)

object Methods {
    /** Custom method for Oman (Ministry of Awqaf). Angles are calibrated against the ministry's published times. */
    const val OMAN = 99

    val all = listOf(
        // Fitted to the Ministry of Awqaf app (Muscat, 5 Nov 2026): 18° / 18°,
        // +1 min Fajr and Isha, +6 min Dhuhr, Asr and Maghrib, rounded up.
        Method(OMAN, "المعادلة العمانية", 18.0, 18.0, null, listOf(1, 6, 6, 6, 1), roundUp = true),
        Method(3, "رابطة العالم الإسلامي", 18.0, 17.0, null),
        Method(4, "أم القرى", 18.5, null, 90),
        Method(8, "منطقة الخليج", 19.5, null, 90),
        Method(5, "الهيئة المصرية", 19.5, 17.5, null),
        Method(2, "أمريكا الشمالية", 15.0, 15.0, null),
    )

    fun byId(id: Int) = all.firstOrNull { it.id == id } ?: all[0]
}

/**
 * Offline astronomical prayer times (same formulas as PrayTimes.org).
 * Used only when the online source cannot be reached.
 */
object PrayerCalc {
    private fun dtr(d: Double) = d * Math.PI / 180.0
    private fun rtd(r: Double) = r * 180.0 / Math.PI
    private fun dsin(d: Double) = sin(dtr(d))
    private fun dcos(d: Double) = cos(dtr(d))
    private fun dtan(d: Double) = tan(dtr(d))
    private fun darcsin(x: Double) = rtd(asin(x))
    private fun darccos(x: Double) = rtd(acos(x.coerceIn(-1.0, 1.0)))
    private fun darctan2(y: Double, x: Double) = rtd(atan2(y, x))
    private fun darccot(x: Double) = rtd(atan(1.0 / x))
    private fun fix(a: Double, b: Double): Double { val r = a - b * floor(a / b); return if (r < 0) r + b else r }
    private fun fixAngle(a: Double) = fix(a, 360.0)
    private fun fixHour(a: Double) = fix(a, 24.0)

    private fun julian(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    /** Returns Fajr, Dhuhr, Asr, Maghrib, Isha (local time, timezone in hours). */
    fun compute(date: LocalDate, lat: Double, lng: Double, tz: Double, method: Method): List<LocalTime> {
        val jDate = julian(date.year, date.monthValue, date.dayOfMonth) - lng / (15.0 * 24.0)

        fun sunPos(jd: Double): Pair<Double, Double> {
            val d = jd - 2451545.0
            val g = fixAngle(357.529 + 0.98560028 * d)
            val q = fixAngle(280.459 + 0.98564736 * d)
            val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
            val e = 23.439 - 0.00000036 * d
            val ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15.0
            val eqt = q / 15.0 - fixHour(ra)
            val decl = darcsin(dsin(e) * dsin(l))
            return decl to eqt
        }

        fun midDay(t: Double): Double = fixHour(12 - sunPos(jDate + t).second)

        fun sunAngleTime(angle: Double, t: Double, ccw: Boolean): Double {
            val decl = sunPos(jDate + t).first
            val noon = midDay(t)
            val tt = darccos((-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat))) / 15.0
            return noon + if (ccw) -tt else tt
        }

        fun asrTime(t: Double): Double {
            val decl = sunPos(jDate + t).first
            val angle = -darccot(1.0 + dtan(abs(lat - decl)))
            return sunAngleTime(angle, t, false)
        }

        val fajr = sunAngleTime(method.fajr, 5.0 / 24, true)
        val dhuhr = midDay(12.0 / 24)
        val asr = asrTime(13.0 / 24)
        val maghrib = sunAngleTime(0.833, 18.0 / 24, false)
        val isha = if (method.ishaMinutes != null) maghrib + method.ishaMinutes / 60.0
        else sunAngleTime(method.ishaAngle ?: 17.0, 18.0 / 24, false)

        val adjust = tz - lng / 15.0
        return listOf(fajr, dhuhr, asr, maghrib, isha).mapIndexed { i, h ->
            val exact = fixHour(h + adjust) * 60 + method.tamkin[i]
            val minutes = if (method.roundUp) kotlin.math.ceil(exact - 1e-6).toInt() else exact.roundToInt()
            val total = ((minutes % (24 * 60)) + 24 * 60) % (24 * 60)
            LocalTime.of(total / 60, total % 60)
        }
    }
}
