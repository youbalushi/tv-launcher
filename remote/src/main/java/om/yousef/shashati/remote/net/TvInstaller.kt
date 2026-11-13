package om.yousef.shashati.remote.net

import android.content.Context
import dadb.AdbKeyPair
import dadb.Dadb
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Installs and sets up شاشتي on a TV through the TV's own debugging service (ADB over Wi-Fi),
 * doing exactly what install_shashati.bat did from the laptop.
 */
class TvInstaller(private val context: Context, private val log: (String) -> Unit) {

    companion object {
        const val APK_URL = "https://github.com/youbalushi/tv-launcher/releases/latest/download/shashati.apk"
        private const val PKG = "om.yousef.shashati"
        private const val DATA = "/sdcard/Android/data/$PKG/files"
    }

    private fun keys(): AdbKeyPair {
        val dir = File(context.filesDir, "adb").apply { mkdirs() }
        val priv = File(dir, "adbkey")
        val pub = File(dir, "adbkey.pub")
        if (!priv.exists() || !pub.exists()) AdbKeyPair.generate(priv, pub)
        return AdbKeyPair.read(priv, pub)
    }

    /** Downloads the latest شاشتي. */
    fun download(onProgress: (Int) -> Unit): File {
        val out = File(context.cacheDir, "shashati.apk")
        val c = URL(APK_URL).openConnection() as HttpURLConnection
        c.instanceFollowRedirects = true
        c.connectTimeout = 15_000
        c.readTimeout = 30_000
        val total = c.contentLengthLong
        c.inputStream.use { input ->
            out.outputStream().use { o ->
                val buf = ByteArray(64 * 1024)
                var done = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    o.write(buf, 0, n)
                    done += n
                    if (total > 0) onProgress((done * 100 / total).toInt())
                }
            }
        }
        c.disconnect()
        return out
    }

    /** Connects to the TV. The TV shows "Allow USB debugging?" the first time. */
    fun connect(host: String): Dadb {
        val d = Dadb.create(host, 5555, keys())
        // dadb connects lazily: run a command now so a refused or unanswered prompt fails at this step.
        val r = d.shell("echo ok")
        if (!r.output.contains("ok")) throw IllegalStateException("no answer from $host")
        log("متصل بـ $host")
        return d
    }

    fun shell(d: Dadb, cmd: String): String {
        val r = d.shell(cmd)
        val text = (r.output + r.errorOutput).trim()
        log("$ $cmd" + if (text.isNotEmpty()) "\n$text" else "")
        return text
    }

    fun install(d: Dadb, apk: File) {
        d.install(apk, "-r")
        log("تم التثبيت")
    }

    /** Same settings the laptop script applied. */
    fun setup(d: Dadb) {
        shell(d, "appops set $PKG SYSTEM_ALERT_WINDOW allow")
        shell(d, "cmd role add-role-holder android.app.role.HOME $PKG")
        shell(d, "cmd package set-home-activity $PKG/.MainActivity")
        val current = shell(d, "settings get secure enabled_accessibility_services")
        val service = "$PKG/$PKG.overlay.HomeKeyService"
        val list = current.split(":").map { it.trim() }.filter { it.isNotEmpty() && it != "null" && it != service }
        shell(d, "settings put secure enabled_accessibility_services " + (list + service).joinToString(":"))
        shell(d, "settings put secure accessibility_enabled 1")
        shell(d, "appops set $PKG WRITE_SETTINGS allow")
        shell(d, "settings put secure screensaver_enabled 1")
        shell(d, "settings put secure screensaver_components $PKG/$PKG.overlay.PrayerDream")
    }

    fun start(d: Dadb) {
        shell(d, "am start -n $PKG/.MainActivity")
    }

    /** Copies the owner's logos and adhkar recordings from another TV that already has them. */
    fun copyFiles(from: Dadb, to: Dadb): Int {
        val tmp = File(context.cacheDir, "copy").apply { deleteRecursively(); mkdirs() }
        var count = 0
        for (folder in listOf("logos", "adhkar")) {
            val names = from.shell("ls $DATA/$folder").output.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.contains("No such") }
            if (names.isEmpty()) continue
            to.shell("mkdir -p $DATA/$folder")
            for (name in names) {
                val f = File(tmp, name)
                runCatching {
                    from.pull(f, "$DATA/$folder/$name")
                    to.push(f, "$DATA/$folder/$name")
                    count++
                }.onFailure { log("تعذّر نسخ $name") }
                f.delete()
            }
            log("نُسخ ${names.size} ملف من $folder")
        }
        return count
    }
}
