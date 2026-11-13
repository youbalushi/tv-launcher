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

    /** Google Drive file id from any share link (file/d/ID, open?id=ID, uc?id=ID) or a bare id. */
    fun driveId(link: String): String? {
        val t = link.trim()
        Regex("/d/([A-Za-z0-9_-]{20,})").find(t)?.let { return it.groupValues[1] }
        Regex("[?&]id=([A-Za-z0-9_-]{20,})").find(t)?.let { return it.groupValues[1] }
        return if (t.matches(Regex("[A-Za-z0-9_-]{20,}"))) t else null
    }

    /**
     * Downloads shashati_files.zip from Google Drive (shared "anyone with the link").
     * Large files get a "can't scan for viruses" page first; its form is followed automatically.
     */
    fun downloadDrive(link: String, onProgress: (Int) -> Unit): File {
        val id = driveId(link) ?: throw IllegalArgumentException("رابط درايف غير صحيح")
        val out = File(context.cacheDir, "shashati_files.zip")
        var url = "https://drive.usercontent.google.com/download?id=$id&export=download&confirm=t"
        repeat(3) {
            val c = URL(url).openConnection() as HttpURLConnection
            c.instanceFollowRedirects = true
            c.connectTimeout = 15_000
            c.readTimeout = 60_000
            val total = c.contentLengthLong
            val type = c.contentType ?: ""
            if (type.startsWith("text/html")) {
                val html = c.inputStream.bufferedReader().use { it.readText() }
                c.disconnect()
                val action = Regex("action=\"([^\"]+)\"").find(html)?.groupValues?.get(1)
                    ?: throw IllegalStateException("درايف رفض التنزيل. تأكد إن الملف مشارك لأي أحد عنده الرابط")
                val inputs = Regex("name=\"([^\"]+)\" value=\"([^\"]*)\"").findAll(html)
                    .joinToString("&") { "${it.groupValues[1]}=${java.net.URLEncoder.encode(it.groupValues[2], "UTF-8")}" }
                url = action.replace("&amp;", "&") + (if (action.contains("?")) "&" else "?") + inputs
                return@repeat
            }
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
            log("تم تنزيل ملفات شاشتي من درايف (${out.length() / 1024 / 1024} ميجا)")
            return out
        }
        throw IllegalStateException("ما قدرت أنزّل الملف من درايف")
    }

    /** Puts every logos/… and adhkar/… file from the zip on the TV. */
    fun pushZip(d: Dadb, zip: File): Int {
        val tmp = File(context.cacheDir, "zip").apply { deleteRecursively(); mkdirs() }
        var count = 0
        java.util.zip.ZipFile(zip).use { z ->
            for (e in z.entries()) {
                if (e.isDirectory) continue
                val parts = e.name.replace('\\', '/').split('/').filter { it.isNotEmpty() }
                val folder = parts.dropLast(1).lastOrNull()?.lowercase() ?: continue
                val name = parts.last()
                if (folder != "logos" && folder != "adhkar") continue
                if (name.startsWith(".")) continue
                val f = File(tmp, name)
                z.getInputStream(e).use { i -> f.outputStream().use { i.copyTo(it) } }
                d.shell("mkdir -p $DATA/$folder")
                d.push(f, "$DATA/$folder/$name")
                f.delete()
                count++
            }
        }
        log("نُقل $count ملف (شعارات وأذكار)")
        return count
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
