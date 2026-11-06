package om.yousef.shashati.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import om.yousef.shashati.MainActivity
import om.yousef.shashati.data.Prefs

/**
 * 1. Catches the remote's Home button and opens شاشتي (Xiaomi ignores the default-home setting).
 * 2. Kids mode: any app not allowed for children is closed and the kids screen comes back.
 * 3. App lock: opening a locked app asks for the PIN first.
 */
class HomeKeyService : AccessibilityService() {

    private fun isNeutral(pkg: String) =
        pkg == packageName ||
            pkg == "android" ||
            pkg.startsWith("com.android.systemui") ||
            pkg.contains("inputmethod") ||
            pkg.contains("keyboard") ||
            pkg == "com.google.android.tvrecommendations"

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (isNeutral(pkg)) return

        if (Prefs.kidsMode) {
            if (pkg !in Prefs.kidsApps()) openSelf(MainActivity.EXTRA_KIDS, "1")
            return
        }

        val session = Prefs.unlockedPkg
        if (session != null && session != pkg) Prefs.unlockedPkg = null
        if (pkg in Prefs.lockedApps() && Prefs.unlockedPkg != pkg) {
            openSelf(MainActivity.EXTRA_LOCK, pkg)
        }
    }

    private fun openSelf(extra: String, value: String) {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra(extra, value)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        runCatching { startActivity(intent) }
    }

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_HOME) return false
        if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) {
            val intent = Intent(this, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            runCatching { startActivity(intent) }
        }
        return true
    }
}
