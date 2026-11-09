package om.yousef.shashati

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import om.yousef.shashati.data.Prefs
import om.yousef.shashati.ui.AppTheme
import om.yousef.shashati.ui.HomeScreen
import om.yousef.shashati.ui.ScreensaverContent
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
    fun screensaver() = paparazzi.snapshot {
        AppTheme { ScreensaverContent() }
    }
}
