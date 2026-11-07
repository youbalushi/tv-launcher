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
import om.yousef.shashati.ui.AdhkarScreen
import om.yousef.shashati.ui.AppTheme
import om.yousef.shashati.ui.KahfScreen
import om.yousef.shashati.ui.ChannelsScreen
import om.yousef.shashati.ui.HomeScreen
import om.yousef.shashati.ui.ScreensaverContent
import om.yousef.shashati.ui.SettingsScreen
import om.yousef.shashati.ui.KidsScreen
import om.yousef.shashati.ui.PinGate
import om.yousef.shashati.ui.AppBackground
import om.yousef.shashati.data.Prefs
import androidx.compose.foundation.focusable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

class MainActivity : ComponentActivity() {
    private enum class Screen { Home, Channels, Settings, Kahf, Adhkar, SaverPreview, Kids, Lock }

    private var screen by mutableStateOf(Screen.Home)
    private var settingsSection by mutableIntStateOf(0)
    private var version by mutableIntStateOf(0)
    private var adhkarMorning by mutableStateOf(true)
    private var lockPkg by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            AppTheme {
                when (screen) {
                    Screen.Home -> {
                        BackHandler { /* launcher: stay here */ }
                        HomeScreen(
                            version = version,
                            onOpenChannels = { screen = Screen.Channels },
                            onOpenSettings = { s -> settingsSection = s; screen = Screen.Settings },
                            onLaunchApp = { pkg -> AppsRepository.launch(this, pkg) },
                            onOpenKahf = { screen = Screen.Kahf },
                            onOpenAdhkar = { m -> adhkarMorning = m; screen = Screen.Adhkar }
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
                    Screen.Kahf -> KahfScreen(onBack = { screen = Screen.Home })
                    Screen.Adhkar -> AdhkarScreen(morning = adhkarMorning, onBack = { screen = Screen.Home })
                    Screen.Settings -> SettingsScreen(
                        initialSection = settingsSection,
                        onBack = { screen = Screen.Home },
                        onChanged = { version++ },
                        onPreview = { screen = Screen.SaverPreview },
                        onStartKids = { screen = Screen.Kids }
                    )
                    Screen.Kids -> KidsScreen(
                        onPlayChannels = { list -> startActivity(PlayerActivity.intent(this, list, 0)) },
                        onExit = { screen = Screen.Home }
                    )
                    Screen.Lock -> {
                        BackHandler { lockPkg = null; screen = Screen.Home }
                        Box(Modifier.fillMaxSize()) {
                            AppBackground()
                            PinGate("هذا التطبيق مقفل") {
                                val pkg = lockPkg
                                lockPkg = null
                                screen = Screen.Home
                                if (pkg != null) {
                                    Prefs.unlockedPkg = pkg
                                    AppsRepository.launch(this@MainActivity, pkg)
                                }
                            }
                        }
                    }
                    Screen.SaverPreview -> {
                        BackHandler { settingsSection = 4; screen = Screen.Settings }
                        val fr = remember { FocusRequester() }
                        LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
                        Box(
                            Modifier.fillMaxSize()
                                .focusRequester(fr)
                                .focusable()
                                .onPreviewKeyEvent { e ->
                                    if (e.type == KeyEventType.KeyUp) { settingsSection = 4; screen = Screen.Settings }
                                    true
                                }
                        ) { ScreensaverContent() }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** Home button, kids-mode return and app-lock requests all arrive here. */
    private fun handleIntent(intent: Intent?) {
        when {
            Prefs.kidsMode -> screen = Screen.Kids
            intent?.getStringExtra(EXTRA_LOCK) != null -> {
                lockPkg = intent.getStringExtra(EXTRA_LOCK)
                screen = Screen.Lock
            }
            intent?.hasCategory(Intent.CATEGORY_HOME) == true -> screen = Screen.Home
        }
    }

    companion object {
        const val EXTRA_KIDS = "kids"
        const val EXTRA_LOCK = "lockPkg"
    }

    override fun onResume() {
        super.onResume()
        Prefs.originalAllowed = false
        if (Prefs.kidsMode && screen != Screen.Kids) screen = Screen.Kids
        AlarmScheduler.scheduleNext(this)
        lifecycleScope.launch {
            val prayersOk = PrayerRepository.refreshIfNeeded()
            if (prayersOk) AlarmScheduler.scheduleNext(this@MainActivity)
            ChannelRepository.refresh()
            version++
        }
    }
}
