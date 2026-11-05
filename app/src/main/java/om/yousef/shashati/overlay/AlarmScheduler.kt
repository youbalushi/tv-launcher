package om.yousef.shashati.overlay

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import om.yousef.shashati.data.Prefs
import om.yousef.shashati.prayer.PrayerRepository
import java.time.LocalDateTime
import java.time.ZoneId

/** Schedules the single next adhan or iqama notification; each firing schedules the following one. */
object AlarmScheduler {
    const val EXTRA_INDEX = "index"
    const val EXTRA_IQAMA = "iqama"
    const val EXTRA_AT = "at"
    const val EXTRA_ADHAN_TIME = "adhanTime"

    data class Event(val index: Int, val iqama: Boolean, val at: LocalDateTime, val adhanAt: LocalDateTime)

    fun nextEvent(now: LocalDateTime = LocalDateTime.now()): Event? {
        val iqama = Prefs.iqamaMinutes
        val events = PrayerRepository.eventsAround(now).flatMap { e ->
            val list = mutableListOf<Event>()
            if (Prefs.adhanOn) list += Event(e.index, false, e.time, e.time)
            if (Prefs.iqamaOn) list += Event(e.index, true, e.time.plusMinutes(iqama[e.index].toLong()), e.time)
            list
        }
        return events.filter { it.at.isAfter(now) }.minByOrNull { it.at }
    }

    fun scheduleNext(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
        val ev = nextEvent() ?: return
        val millis = ev.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        intent.putExtra(EXTRA_INDEX, ev.index)
            .putExtra(EXTRA_IQAMA, ev.iqama)
            .putExtra(EXTRA_AT, millis)
            .putExtra(EXTRA_ADHAN_TIME, ev.adhanAt.toLocalTime().toString())
        val firing = PendingIntent.getBroadcast(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, firing)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, firing)
        }
    }
}
