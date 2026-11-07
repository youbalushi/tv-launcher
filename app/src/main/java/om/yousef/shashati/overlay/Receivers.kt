package om.yousef.shashati.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import om.yousef.shashati.data.Prefs
import om.yousef.shashati.prayer.PrayerRepository
import java.time.LocalTime

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val index = intent.getIntExtra(AlarmScheduler.EXTRA_INDEX, -1)
        val iqama = intent.getBooleanExtra(AlarmScheduler.EXTRA_IQAMA, false)
        val at = intent.getLongExtra(AlarmScheduler.EXTRA_AT, 0L)
        val adhanTime = intent.getStringExtra(AlarmScheduler.EXTRA_ADHAN_TIME)
        val app = context.applicationContext

        val key = "$at|$index|$iqama"
        val fresh = System.currentTimeMillis() - at < 2 * 60_000L
        val duplicate = Prefs.lastFiredKey == key

        // Schedule the following event right away.
        AlarmScheduler.scheduleNext(app)

        if (index !in 0..4 || !fresh || duplicate) return
        Prefs.lastFiredKey = key

        val pending = goAsync()
        val shown = OverlayNotifier.show(
            app, index, iqama,
            adhanTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        ) { pending.finish() }
        if (!shown) pending.finish()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        AlarmScheduler.scheduleNext(app)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && !Prefs.originalAllowed) {
            // Open شاشتي after a restart (allowed because the app may draw over other apps).
            runCatching {
                app.startActivity(
                    Intent(app, om.yousef.shashati.MainActivity::class.java)
                        .setAction(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PrayerRepository.refreshIfNeeded()
                Handler(Looper.getMainLooper()).post { AlarmScheduler.scheduleNext(app) }
            } finally {
                pending.finish()
            }
        }
    }
}
