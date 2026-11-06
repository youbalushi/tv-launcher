package om.yousef.shashati.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class Reciter(val id: String, val name: String, val style: String, val url: String)

/**
 * Surah Al-Kahf recitations (Hafs) from mp3quran.net, which offers them free.
 * Each one is downloaded the first time it is chosen and then kept on the TV for offline use.
 */
object KahfRepository {
    val reciters = listOf(
        Reciter("maher", "ماهر المعيقلي", "مرتّل", "https://cdn.mp3quran.net/audio/maher-muaiqly/r1/018.mp3"),
        Reciter("hazza", "هزاع البلوشي", "مرتّل", "https://cdn.mp3quran.net/audio/hazza-balushi/r1/018.mp3"),
        Reciter("dukhain", "هيثم الدخين", "تلاوة مميزة", "https://cdn.mp3quran.net/audio/haitham-dukhain/r1/018.mp3"),
        Reciter("afasy", "مشاري العفاسي", "مرتّل", "https://cdn.mp3quran.net/audio/mishary-alafasy/r1/018.mp3"),
        Reciter("husary", "محمود خليل الحصري", "مرتّل", "https://cdn.mp3quran.net/audio/mahmoud-husary/r1/018.mp3"),
        Reciter("sudais", "عبدالرحمن السديس", "مرتّل", "https://cdn.mp3quran.net/audio/abdulrahman-sudais/r1/018.mp3"),
        Reciter("yasser", "ياسر الدوسري", "مرتّل", "https://cdn.mp3quran.net/audio/yasser-dosari/r1/018.mp3"),
        Reciter("ghamdi", "سعد الغامدي", "مرتّل", "https://cdn.mp3quran.net/audio/saad-ghamdi/r1/018.mp3"),
        Reciter("abdulbasit", "عبدالباسط عبدالصمد", "مرتّل", "https://cdn.mp3quran.net/audio/abdulbasit-abdulsamad/r3/018.mp3"),
    )

    private fun dir(context: Context) = File(context.filesDir, "kahf").apply { mkdirs() }

    fun file(context: Context, r: Reciter) = File(dir(context), "${r.id}.mp3")

    fun isDownloaded(context: Context, r: Reciter) = file(context, r).let { it.exists() && it.length() > 100_000 }

    /** Downloads the recitation, reporting progress from 0 to 1. Returns true on success. */
    suspend fun download(context: Context, r: Reciter, onProgress: (Float) -> Unit): Boolean = withContext(Dispatchers.IO) {
        val target = file(context, r)
        val temp = File(target.parentFile, "${r.id}.part")
        runCatching {
            val conn = URL(r.url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.instanceFollowRedirects = true
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                temp.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) onProgress(done.toFloat() / total)
                    }
                }
            }
            conn.disconnect()
            if (temp.length() < 100_000) error("too small")
            temp.renameTo(target)
        }.getOrElse {
            temp.delete()
            false
        }
    }
}
