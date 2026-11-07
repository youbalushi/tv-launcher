package om.yousef.shashati.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class InstalledApp(val packageName: String, val label: String, val banner: Drawable?, val icon: Drawable?)

object AppsRepository {
    const val CHANNELS = "channels"

    private val knownDefaults = listOf(
        "com.google.android.youtube.tv",
        "com.netflix.ninja",
        "com.amazon.amazonvideo.livingroom",
    )

    fun installed(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val seen = LinkedHashMap<String, InstalledApp>()
        val queries = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
        )
        for (q in queries) {
            for (ri in pm.queryIntentActivities(q, 0)) {
                val pkg = ri.activityInfo.packageName
                if (pkg == context.packageName || seen.containsKey(pkg)) continue
                val banner = runCatching { ri.activityInfo.loadBanner(pm) ?: ri.activityInfo.applicationInfo.loadBanner(pm) }.getOrNull()
                seen[pkg] = InstalledApp(pkg, ri.loadLabel(pm).toString(), banner, runCatching { ri.loadIcon(pm) }.getOrNull())
            }
        }
        return seen.values.sortedBy { it.label }
    }

    fun favorites(context: Context): List<String> {
        val raw = Prefs.favoritesRaw
        if (raw != null) return raw.split("|").filter { it.isNotBlank() }
        val installed = installed(context).map { it.packageName }.toSet()
        val defaults = listOf(CHANNELS) + knownDefaults.filter { it in installed }
        Prefs.favoritesRaw = defaults.joinToString("|")
        return defaults
    }

    fun saveFavorites(list: List<String>) {
        Prefs.favoritesRaw = list.joinToString("|")
    }

    fun launch(context: Context, packageName: String): Boolean {
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(packageName)
            ?: pm.getLaunchIntentForPackage(packageName)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }

    /** Opens the TV's own home screen (Xiaomi / Google), skipping this app. */
    fun openOriginalLauncher(context: Context): Boolean {
        val pm = context.packageManager
        val homes = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_ALL
        ).filter { it.activityInfo.packageName != context.packageName }
        val preferred = listOf(
            "com.google.android.tvlauncher",
            "com.google.android.apps.tv.launcherx",
            "com.mitv.tvhome",
            "com.mitv.tvhome.atv",
        )
        Prefs.originalAllowed = true
        val target = homes.sortedBy { r ->
            val i = preferred.indexOf(r.activityInfo.packageName)
            if (i < 0) 99 else i
        }.firstOrNull() ?: return false
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .setComponent(ComponentName(target.activityInfo.packageName, target.activityInfo.name))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }
}
