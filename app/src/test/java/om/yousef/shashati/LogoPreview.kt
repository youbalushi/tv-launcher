package om.yousef.shashati

import om.yousef.shashati.data.LogoCleaner
import org.json.JSONObject
import org.junit.Test
import java.awt.Color
import java.awt.GradientPaint
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.imageio.ImageIO

/** Downloads every channel logo, cleans it, and draws a contact sheet on glass cards for review. */
class LogoPreview {
    @Test
    fun sheet() {
        val text = File("../channels.json").readText()
        val arr = JSONObject(text).getJSONArray("channels")
        val items = (0 until arr.length()).map { arr.getJSONObject(it) }.filter { it.optString("logo").isNotBlank() }
        val cols = 6
        val cw = 300
        val ch = 130
        val rows = (items.size + cols - 1) / cols
        val sheet = BufferedImage(cols * (cw + 20) + 20, rows * (ch + 20) + 20, BufferedImage.TYPE_INT_ARGB)
        val g = sheet.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.paint = GradientPaint(0f, 0f, Color(0x0A1B3F), sheet.width.toFloat(), sheet.height.toFloat(), Color(0x123274))
        g.fillRect(0, 0, sheet.width, sheet.height)
        items.forEachIndexed { i, o ->
            val x = 20 + (i % cols) * (cw + 20)
            val y = 20 + (i / cols) * (ch + 20)
            g.color = Color(255, 255, 255, 34)
            g.fill(RoundRectangle2D.Float(x.toFloat(), y.toFloat(), cw.toFloat(), ch.toFloat(), 30f, 30f))
            g.color = Color(255, 255, 255, 70)
            g.draw(RoundRectangle2D.Float(x.toFloat(), y.toFloat(), cw.toFloat(), ch.toFloat(), 30f, 30f))
            val img = runCatching {
                val c = URL(o.getString("logo")).openConnection() as HttpURLConnection
                c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android TV) Shashati")
                c.connectTimeout = 10000; c.readTimeout = 15000
                ImageIO.read(c.inputStream)
            }.getOrNull()
            if (img == null) {
                g.color = Color.RED
                g.drawString("FAILED " + o.getString("id"), x + 10, y + 20)
                return@forEachIndexed
            }
            val src = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB).also { it.createGraphics().drawImage(img, 0, 0, null) }
            val px = src.getRGB(0, 0, src.width, src.height, null, 0, src.width)
            val out = LogoCleaner.clean(LogoCleaner.Img(px, src.width, src.height))
            val res = BufferedImage(out.w, out.h, BufferedImage.TYPE_INT_ARGB)
            res.setRGB(0, 0, out.w, out.h, out.px, 0, out.w)
            val maxW = cw * 0.72
            val maxH = ch * 0.6
            val s = minOf(maxW / out.w, maxH / out.h)
            val dw = (out.w * s).toInt()
            val dh = (out.h * s).toInt()
            g.drawImage(res, x + (cw - dw) / 2, y + (ch - dh) / 2, dw, dh, null)
            g.color = Color(255, 255, 255, 120)
            g.drawString(o.getString("id"), x + 8, y + ch - 6)
        }
        val dir = File("build/logo-preview").apply { mkdirs() }
        ImageIO.write(sheet, "png", File(dir, "logos.png"))
    }
}
