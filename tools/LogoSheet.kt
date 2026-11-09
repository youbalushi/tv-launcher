// Review sheet: downloads every channel logo, cleans it with LogoCleaner, draws it on glass cards.
// Run in CI:  kotlinc tools/LogoSheet.kt app/src/main/java/om/yousef/shashati/data/LogoCleaner.kt -include-runtime -d sheet.jar
import om.yousef.shashati.data.LogoCleaner
import java.awt.Color
import java.awt.GradientPaint
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.imageio.ImageIO

fun main(args: Array<String>) {
    val text = File(args[0]).readText()
    val re = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"[\\s\\S]*?\"logo\"\\s*:\\s*\"([^\"]+)\"")
    val blocks = text.split(Regex("\\n\\s*\\{")).drop(1)
    val items = blocks.mapNotNull { b -> re.find("{$b")?.let { it.groupValues[1] to it.groupValues[2].replace("\\u0026", "&") } }
        .filter { (_, logo) -> logo.startsWith("http") }
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
    items.forEachIndexed { i, (id, logo) ->
        val x = 20 + (i % cols) * (cw + 20)
        val y = 20 + (i / cols) * (ch + 20)
        g.color = Color(255, 255, 255, 34)
        g.fill(RoundRectangle2D.Float(x.toFloat(), y.toFloat(), cw.toFloat(), ch.toFloat(), 30f, 30f))
        g.color = Color(255, 255, 255, 70)
        g.draw(RoundRectangle2D.Float(x.toFloat(), y.toFloat(), cw.toFloat(), ch.toFloat(), 30f, 30f))
        val img = runCatching {
            val c = URL(logo).openConnection() as HttpURLConnection
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android TV) Shashati")
            c.setRequestProperty("Accept", "image/png,image/jpeg,image/*;q=0.8")
            c.connectTimeout = 10000; c.readTimeout = 15000
            ImageIO.read(c.inputStream)
        }.getOrNull()
        g.color = Color(255, 255, 255, 120)
        g.drawString(id, x + 8, y + ch - 6)
        if (img == null) { g.color = Color.RED; g.drawString("FAILED", x + 10, y + 20); return@forEachIndexed }
        val src = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB).also { it.createGraphics().drawImage(img, 0, 0, null) }
        val scale = if (src.width > 480) 480.0 / src.width else 1.0
        val sw = (src.width * scale).toInt().coerceAtLeast(1)
        val sh = (src.height * scale).toInt().coerceAtLeast(1)
        val small = BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB).also {
            val gg = it.createGraphics(); gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); gg.drawImage(src, 0, 0, sw, sh, null)
        }
        val px = small.getRGB(0, 0, sw, sh, null, 0, sw)
        val out = LogoCleaner.clean(LogoCleaner.Img(px, sw, sh))
        val res = BufferedImage(out.w, out.h, BufferedImage.TYPE_INT_ARGB)
        res.setRGB(0, 0, out.w, out.h, out.px, 0, out.w)
        val s = minOf(cw * 0.72 / out.w, ch * 0.6 / out.h)
        val dw = (out.w * s).toInt()
        val dh = (out.h * s).toInt()
        g.drawImage(res, x + (cw - dw) / 2, y + (ch - dh) / 2, dw, dh, null)
    }
    ImageIO.write(sheet, "png", File(args[1]))
}
