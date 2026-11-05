package om.yousef.shashati

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import om.yousef.shashati.data.AppsRepository
import om.yousef.shashati.data.ChannelRepository
import om.yousef.shashati.overlay.AlarmScheduler
import om.yousef.shashati.prayer.PrayerRepository
import om.yousef.shashati.ui.AppTheme
import om.yousef.shashati.ui.ChannelsScreen
import om.yousef.shashati.ui.HomeScreen
import om.yousef.shashati.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    private enum class Screen { Home, Channels, Settings }

    private var screen by mutableStateOf(Screen.Home)
    private var settingsSection by mutableIntStateOf(0)
    private var version by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                when (screen) {
                    Screen.Home -> {
                        BackHandler { /* launcher: stay here */ }
                        HomeScreen(
                            version = version,
                            onOpenChannels = { screen = Screen.Channels },
                            onOpenSettings = { s -> settingsSection = s; screen = Screen.Settings },
                            onLaunchApp = { pkg -> AppsRepository.launch(this, pkg) }
                        )
                    }
                    Screen.Channels -> {
                        BackHandler { screen = Screen.Home }
                        ChannelsScreen(
                            version = version,
                            onBack = { screen = Screen.Home },
                            onPlay = { list, i -> startActivity(PlayerActivity.intent(this, list, i)) }
                        )
                    }
                    Screen.Settings -> SettingsScreen(
                        initialSection = settingsSection,
                        onBack = { screen = Screen.Home },
                        onChanged = { version++ }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Pressing Home while inside a sub-screen returns to the main screen.
        if (intent.hasCategory(Intent.CATEGORY_HOME)) screen = Screen.Home
    }

    override fun onResume() {
        super.onResume()
        AlarmScheduler.scheduleNext(this)
        lifecycleScope.launch {
            val prayersOk = PrayerRepository.refreshIfNeeded()
            if (prayersOk) AlarmScheduler.scheduleNext(this@MainActivity)
            ChannelRepository.refresh()
            version++
        }
    }
}
