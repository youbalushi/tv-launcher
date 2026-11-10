package om.yousef.shashati.data

import android.content.Context

/**
 * The two rows of the home screen: 4 live channels and 4 apps
 * (plus the fixed "all channels" / "all apps" cards).
 */
object HomeTiles {
    const val SLOTS = 5

    /** Apps from the approved design, used when they exist on this TV. */
    val designApps = listOf(
        "com.xiaomi.mitv.smartshare",   // Miracast
        "com.xiaomi.mitv.mediaexplorer", // Media Player
        "com.mitv.gallery",             // Gallery
        "com.mitv.livetv",              // Live TV
    )

    fun channels(context: Context): List<Channel> {
        val all = ChannelRepository.load(context).channels
        val saved = Prefs.homeChannelsRaw?.split("|")?.filter { it.isNotBlank() }
        // the owner's choices first, then fill any empty card
        val ids = saved.orEmpty() + ChannelRepository.favorites().toList() + all.filter { it.category == "oman" }.map { it.id } + all.map { it.id }
        return ids.distinct().mapNotNull { id -> all.firstOrNull { it.id == id } }.take(SLOTS)
    }

    fun apps(context: Context): List<InstalledApp> {
        val installed = AppsRepository.installed(context)
        val saved = Prefs.homeAppsRaw?.split("|")?.filter { it.isNotBlank() }
        val favs = Prefs.favoritesRaw?.split("|").orEmpty()
        val ids = saved.orEmpty() + designApps + favs + installed.map { it.packageName }
        return ids.distinct().mapNotNull { p -> installed.firstOrNull { it.packageName == p } }.take(SLOTS)
    }

    fun saveChannels(ids: List<String>) { Prefs.homeChannelsRaw = ids.joinToString("|") }
    fun saveApps(ids: List<String>) { Prefs.homeAppsRaw = ids.joinToString("|") }
}
