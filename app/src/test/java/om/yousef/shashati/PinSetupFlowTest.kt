package om.yousef.shashati

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import om.yousef.shashati.data.Prefs
import om.yousef.shashati.ui.AppTheme
import om.yousef.shashati.ui.SettingsScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** First-time PIN + recovery setup must open the settings after «حفظ». */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], qualifiers = "w960dp-h540dp-land")
class PinSetupFlowTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun saveRecoveryOpensSettings() {
        Prefs.init(ApplicationProvider.getApplicationContext())
        rule.setContent { AppTheme { SettingsScreen(initialSection = 0, onBack = {}, onChanged = {}) } }
        fun tap(d: String) = rule.onAllNodesWithText(d)[0].performClick()
        repeat(2) { "1234".forEach { tap(it.toString()) }; rule.waitForIdle() }
        "123456".forEach { tap(it.toString()) }
        rule.waitForIdle()
        rule.onNodeWithText("حفظ").performClick()
        rule.waitForIdle()
        val opened = rule.onAllNodesWithText("بطاقات الرئيسية").fetchSemanticsNodes().isNotEmpty()
        println("::error::hasPin=${Prefs.hasPin} recovery=${Prefs.recoveryHash != null} settingsOpened=$opened")
        assertTrue("settings did not open", opened)
    }
}
