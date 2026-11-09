package om.yousef.shashati.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Channel and app logos without their background, ready for the glass cards. Cached on the TV. */
object Logos {
    private const val VERSION = 1
    private val mem = HashMap<String, ImageBitmap?>()

    private fun dir(context: Context) = File(context.filesDir, "logos").apply { mkdirs() }

    private fun process(src: Bitmap): Bitmap {
        val maxW = 480
        val scaled = if (src.width > maxW) Bitmap.createScaledBitmap(src, maxW, (src.height * maxW.toFloat() / src.width).toInt().coerceAtLeast(1), true) else src
        val argb = if (scaled.config == Bitmap.Config.ARGB_8888) scaled else scaled.copy(Bitmap.Config.ARGB_8888, false)
        val px = IntArray(argb.width * argb.height)
        argb.getPixels(px, 0, argb.width, 0, 0, argb.width, argb.height)
        val out = LogoCleaner.clean(LogoCleaner.Img(px, argb.width, argb.height))
        return Bitmap.createBitmap(out.px, out.w, out.h, Bitmap.Config.ARGB_8888)
    }

    fun cachedChannel(context: Context, ch: Channel): ImageBitmap? {
        val key = "ch_${ch.id}"
        synchronized(mem) { if (mem.containsKey(key)) return mem[key] }
        val f = File(dir(context), "$key-v$VERSION.png")
        if (!f.exists()) return null
        val bmp = runCatching { BitmapFactory.decodeFile(f.path)?.asImageBitmap() }.getOrNull()
        synchronized(mem) { mem[key] = bmp }
        return bmp
    }

    /** Downloads (once) and cleans a channel logo. */
    suspend fun channel(context: Context, ch: Channel): ImageBitmap? {
        cachedChannel(context, ch)?.let { return it }
        val url = ch.logo ?: return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = 10_000
                c.readTimeout = 15_000
                c.instanceFollowRedirects = true
                c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android TV) Shashati")
                c.setRequestProperty("Accept", "image/png,image/jpeg,image/*;q=0.8")
                val bytes = c.inputStream.use { it.readBytes() }
                c.disconnect()
                val src = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@runCatching null
                val clean = process(src)
                File(dir(context), "ch_${ch.id}-v$VERSION.png").outputStream().use { clean.compress(Bitmap.CompressFormat.PNG, 100, it) }
                clean.asImageBitmap().also { synchronized(mem) { mem["ch_${ch.id}"] = it } }
            }.getOrNull()
        }
    }

    private fun render(d: Drawable, w: Int, h: Int): Bitmap {
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, w, h)
        d.draw(Canvas(b))
        return b
    }

    /** The app's own banner (or icon) with the background taken away. */
    fun app(app: InstalledApp): ImageBitmap? {
        val key = "app_${app.packageName}"
        synchronized(mem) { if (mem.containsKey(key)) return mem[key] }
        val bmp = runCatching {
            val banner = app.banner
            val icon = app.icon
            val raw = when {
                banner != null -> render(banner, 320, 180)
                icon is AdaptiveIconDrawable -> render(icon.foreground ?: icon, 216, 216)
                icon != null -> render(icon, 192, 192)
                else -> null
            }
            raw?.let { process(it).asImageBitmap() }
        }.getOrNull()
        synchronized(mem) { mem[key] = bmp }
        return bmp
    }
}
