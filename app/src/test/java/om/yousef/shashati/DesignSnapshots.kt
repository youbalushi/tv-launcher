package om.yousef.shashati

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import om.yousef.shashati.data.Prefs
import om.yousef.shashati.ui.AppTheme
import om.yousef.shashati.ui.HomeScreen
import om.yousef.shashati.ui.ScreensaverContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Renders the screens at 1080p TV size so the design can be checked against the reference images. */
class DesignSnapshots {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = 1920, screenHeight = 1080, xdpi = 320, ydpi = 320,
            density = Density.XHIGH, orientation = ScreenOrientation.LANDSCAPE, softButtons = false,
        ),
        showSystemUi = false,
        maxPercentDifference = 100.0,
    )

    @Before
    fun setUp() {
        Prefs.init(paparazzi.context)
    }

    @Test
    fun home() = paparazzi.snapshot {
        AppTheme { HomeScreen(version = 0, onOpenChannels = {}, onOpenSettings = {}, onLaunchApp = {}) }
    }

    @Test
    fun channelPanel() = paparazzi.snapshot {
        AppTheme {
            androidx.compose.foundation.layout.Box(
                androidx.compose.ui.Modifier.fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color(0xFF3A4A66))
            ) {
                val all = om.yousef.shashati.data.ChannelRepository.load(paparazzi.context).channels
                val news = all.filter { it.category == "news" }
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.TopEnd)) {
                    om.yousef.shashati.ui.ChannelPanel(
                        tabs = listOf("الكل", "عُمان", "إخبارية", "عائلية وخليجية", "المفضلة"), tab = 2,
                        list = news, selected = 2, playingId = news.first().id,
                        numbers = all.mapIndexed { i, c -> c.id to i + 1 }.toMap(),
                    )
                }
            }
        }
    }

    @Test
    fun screensaver() = paparazzi.snapshot {
        AppTheme { ScreensaverContent() }
    }
}
