package om.yousef.shashati.data

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** All persistent settings for this TV. Each TV keeps its own copy. */
object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.getSharedPreferences("shashati", Context.MODE_PRIVATE)
    }

    private fun bool(key: String, def: Boolean) = object : ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = sp.getBoolean(key, def)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            sp.edit().putBoolean(key, value).apply()
        }
    }

    private fun int(key: String, def: Int) = object : ReadWriteProperty<Any?, Int> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = sp.getInt(key, def)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            sp.edit().putInt(key, value).apply()
        }
    }

    private fun long(key: String, def: Long) = object : ReadWriteProperty<Any?, Long> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = sp.getLong(key, def)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
            sp.edit().putLong(key, value).apply()
        }
    }

    private fun str(key: String) = object : ReadWriteProperty<Any?, String?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): String? = sp.getString(key, null)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
            sp.edit().putString(key, value).apply()
        }
    }

    private fun intList(key: String, def: List<Int>) = object : ReadWriteProperty<Any?, List<Int>> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): List<Int> {
            val raw = sp.getString(key, null) ?: return def
            val parsed = raw.split(",").mapNotNull { it.trim().toIntOrNull() }
            return if (parsed.size == def.size) parsed else def
        }
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: List<Int>) {
            sp.edit().putString(key, value.joinToString(",")).apply()
        }
    }

    // Home screen
    var showPrayerBar by bool("showPrayerBar", true)
    var showNextCard by bool("showNextCard", true)

    /** Favorite cards in order. "channels" is the built-in channels card; others are package names. */
    var favoritesRaw by str("favorites")

    // Location and prayer calculation
    var govIndex by int("govIndex", 0)
    var wilIndex by int("wilIndex", 0)
    /** Prayer times always use the Omani formula (ministry calendar); other methods were removed. */
    val method: Int get() = om.yousef.shashati.prayer.Methods.OMAN
    var offsets by intList("offsets", listOf(0, 0, 0, 0, 0))

    // Adhan and iqama notifications
    var adhanOn by bool("adhanOn", true)
    var iqamaOn by bool("iqamaOn", true)
    var iqamaMinutes by intList("iqama", listOf(20, 20, 20, 20, 20))
    var notifPos by int("notifPos", 0) // 0 top-left, 1 top-right, 2 bottom-left, 3 bottom-right
    var notifSeconds by int("notifSec", 10)
    var lastFiredKey by str("lastFired")

    // Screensaver
    var saverNight by bool("saverNight", true)
    var saverHijri by bool("saverHijri", true)
    var saverDhikr by bool("saverDhikr", true)
    var saverPrayers by bool("saverPrayers", true)
    var saverMinutes by int("saverMinutes", 10)

    // Kids mode and app lock
    var kidsMode by bool("kidsMode", false)
    var kidsAppsRaw by str("kidsApps")
    var lockedAppsRaw by str("lockedApps")
    var unlockedPkg by str("unlockedPkg")

    /** True only after the user chose "exit to the original home screen", until Home is pressed again. */
    var originalAllowed by bool("originalAllowed", false)

    fun kidsApps(): Set<String> = kidsAppsRaw?.split("|")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    fun lockedApps(): Set<String> = lockedAppsRaw?.split("|")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    fun setKidsApps(v: Set<String>) { kidsAppsRaw = v.joinToString("|") }
    fun setLockedApps(v: Set<String>) { lockedAppsRaw = v.joinToString("|") }

    // Security
    var askPin by bool("askPin", true)
    var pinHash by str("pinHash")
    var recoveryHash by str("recHash")
    var salt by str("salt")
    var fails by int("fails", 0)
    var lockCount by int("lockCount", 0)
    var lockUntil by long("lockUntil", 0L)

    // Caches
    var channelsJson by str("channelsJson")
    var channelFavs by str("channelFavs")
    /** Phones paired with this TV's remote service (token -> phone name, one per line as token|name). */
    var remoteTokensRaw by str("remoteTokens")

    // Home screen rows (pipe-separated ids) and weather cache
    var homeChannelsRaw by str("homeChannels")
    var homeAppsRaw by str("homeApps")
    var weatherJson by str("weatherJson")
    var weatherAt by long("weatherAt", 0L)

    /** Kahf / morning / evening adhkar played today (kind|date per line, last few days only). */
    private var playedRaw by str("played")
    fun playedToday(kind: String, day: java.time.LocalDate) = playedRaw?.lines()?.contains("$kind|$day") == true
    fun markPlayed(kind: String, day: java.time.LocalDate) {
        val keep = playedRaw?.lines()?.filter { it.isNotBlank() }?.takeLast(6).orEmpty()
        playedRaw = (keep + "$kind|$day").distinct().joinToString("\n")
    }

    /** One-time: adhkar were already played on 9 Nov 2026 before this feature existed. */
    private var playedSeeded by bool("playedSeeded", false)
    fun seedPlayed() {
        if (playedSeeded) return
        playedSeeded = true
        val d = java.time.LocalDate.of(2026, 11, 9)
        markPlayed("morning", d); markPlayed("evening", d)
    }
    var prayerCache by str("prayerCache")
    var prayerFetchedDay by str("prayerFetchedDay")

    val hasPin: Boolean get() = pinHash != null

    private fun hash(value: String): String {
        var s = salt
        if (s == null) {
            val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
            s = bytes.joinToString("") { "%02x".format(it) }
            salt = s
        }
        val digest = MessageDigest.getInstance("SHA-256").digest((s + value).toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun setPin(pin: String) { pinHash = hash(pin) }
    fun setRecovery(code: String) { recoveryHash = hash(code) }
    fun checkPin(pin: String) = pinHash != null && hash(pin) == pinHash
    fun checkRecovery(code: String) = recoveryHash != null && hash(code) == recoveryHash

    /** Seconds still blocked after too many wrong attempts, or 0. */
    fun blockedSeconds(): Long {
        val left = lockUntil - System.currentTimeMillis()
        return if (left > 0) (left + 999) / 1000 else 0
    }

    /** Records a wrong attempt; after 5 in a row input is blocked for 1, 5, then 15 minutes. */
    fun registerFail() {
        val f = fails + 1
        if (f < 5) {
            fails = f
            return
        }
        val minutes = listOf(1, 5, 15)[minOf(lockCount, 2)]
        fails = 0
        lockCount += 1
        lockUntil = System.currentTimeMillis() + minutes * 60_000L
    }

    fun registerSuccess() {
        fails = 0
        lockCount = 0
        lockUntil = 0
    }
}
