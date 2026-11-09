package om.yousef.shashati.data

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Turns a logo picture into "just the logo": removes a solid background colour around it,
 * trims the empty edges, and lightens black lettering so it reads on the dark glass cards.
 * Pure Kotlin on ARGB pixels so it can be tested off the TV.
 */
object LogoCleaner {
    class Img(val px: IntArray, val w: Int, val h: Int)

    private fun a(c: Int) = c ushr 24
    private fun r(c: Int) = (c shr 16) and 255
    private fun g(c: Int) = (c shr 8) and 255
    private fun b(c: Int) = c and 255
    private fun dist(c1: Int, c2: Int): Float {
        val dr = r(c1) - r(c2); val dg = g(c1) - g(c2); val db = b(c1) - b(c2)
        return sqrt((dr * dr + dg * dg + db * db).toFloat())
    }

    fun clean(src: Img): Img {
        val w = src.w
        val h = src.h
        val px = src.px.copyOf()

        // 1. Background colour = the most common opaque colour on the border.
        val border = ArrayList<Int>()
        for (x in 0 until w) { border += px[x]; border += px[(h - 1) * w + x] }
        for (y in 0 until h) { border += px[y * w]; border += px[y * w + w - 1] }
        val opaque = border.filter { a(it) > 200 }
        if (opaque.size > border.size * 0.6) {
            val bg = opaque.groupingBy { (r(it) / 24) * 10000 + (g(it) / 24) * 100 + (b(it) / 24) }.eachCount().maxBy { it.value }.key
            val sample = opaque.filter { (r(it) / 24) * 10000 + (g(it) / 24) * 100 + (b(it) / 24) == bg }
            val bgc = sample[sample.size / 2]
            val share = sample.size.toFloat() / border.size
            if (share > 0.45f) removeBackground(px, w, h, bgc)
        }

        // 2. Trim transparent edges.
        var x0 = w; var y0 = h; var x1 = -1; var y1 = -1
        for (y in 0 until h) for (x in 0 until w) if (a(px[y * w + x]) > 24) {
            x0 = min(x0, x); x1 = max(x1, x); y0 = min(y0, y); y1 = max(y1, y)
        }
        if (x1 < 0) return src
        val nw = x1 - x0 + 1
        val nh = y1 - y0 + 1
        val out = IntArray(nw * nh)
        for (y in 0 until nh) System.arraycopy(px, (y + y0) * w + x0, out, y * nw, nw)

        // 3. Dark grey / black lettering becomes white (coloured parts stay as they are).
        lighten(out)
        return Img(out, nw, nh)
    }

    private fun removeBackground(px: IntArray, w: Int, h: Int, bg: Int) {
        val hard = 42f
        val soft = 95f
        val seen = BooleanArray(w * h)
        val stack = IntArray(w * h)
        var sp = 0
        fun push(i: Int) { if (!seen[i]) { seen[i] = true; stack[sp++] = i } }
        for (x in 0 until w) { push(x); push((h - 1) * w + x) }
        for (y in 0 until h) { push(y * w); push(y * w + w - 1) }
        while (sp > 0) {
            val i = stack[--sp]
            val c = px[i]
            val d = dist(c, bg)
            if (a(c) > 0 && d > soft) continue
            if (d <= hard || a(c) == 0) {
                px[i] = 0
            } else {
                // soft edge: partly transparent and with the background colour taken out
                val t = (d - hard) / (soft - hard)
                val alpha = (a(c) * t).toInt().coerceIn(0, 255)
                fun un(v: Int, bv: Int) = ((v - bv * (1 - t)) / t).toInt().coerceIn(0, 255)
                px[i] = (alpha shl 24) or (un(r(c), r(bg)) shl 16) or (un(g(c), g(bg)) shl 8) or un(b(c), b(bg))
                continue
            }
            val x = i % w
            val y = i / w
            if (x > 0) push(i - 1)
            if (x < w - 1) push(i + 1)
            if (y > 0) push(i - w)
            if (y < h - 1) push(i + w)
        }
    }

    private fun luma(c: Int) = (0.299f * r(c) + 0.587f * g(c) + 0.114f * b(c)) / 255f

    /** Very dark parts (black or navy lettering) become light, keeping their hue. */
    private fun lighten(px: IntArray) {
        var dark = 0
        var visible = 0
        for (c in px) {
            if (a(c) < 128) continue
            visible++
            if (luma(c) < 0.27f) dark++
        }
        if (visible == 0 || dark < visible * 0.12f) return
        for (i in px.indices) {
            val c = px[i]
            if (a(c) == 0) continue
            val y = luma(c)
            if (y >= 0.33f) continue
            val hsl = toHsl(r(c), g(c), b(c))
            // smooth: the darker the pixel, the lighter it becomes
            val t = ((0.33f - y) / 0.33f).coerceIn(0f, 1f)
            val l = hsl[2] + (0.9f - hsl[2]) * t
            val sat = hsl[1] * (1f - 0.35f * t)
            val rgb = fromHsl(hsl[0], sat, l)
            px[i] = (a(c) shl 24) or (rgb[0] shl 16) or (rgb[1] shl 8) or rgb[2]
        }
    }

    private fun toHsl(r: Int, g: Int, b: Int): FloatArray {
        val rf = r / 255f; val gf = g / 255f; val bf = b / 255f
        val mx = max(rf, max(gf, bf)); val mn = min(rf, min(gf, bf))
        val l = (mx + mn) / 2f
        if (mx == mn) return floatArrayOf(0f, 0f, l)
        val d = mx - mn
        val s = if (l > 0.5f) d / (2f - mx - mn) else d / (mx + mn)
        val h = when (mx) {
            rf -> ((gf - bf) / d + (if (gf < bf) 6 else 0)) / 6f
            gf -> ((bf - rf) / d + 2) / 6f
            else -> ((rf - gf) / d + 4) / 6f
        }
        return floatArrayOf(h, s, l)
    }

    private fun fromHsl(h: Float, s: Float, l: Float): IntArray {
        if (s == 0f) { val v = (l * 255).toInt().coerceIn(0, 255); return intArrayOf(v, v, v) }
        val q = if (l < 0.5f) l * (1 + s) else l + s - l * s
        val p = 2 * l - q
        fun hue(t0: Float): Int {
            var t = t0
            if (t < 0) t += 1f
            if (t > 1) t -= 1f
            val v = when {
                t < 1f / 6 -> p + (q - p) * 6 * t
                t < 1f / 2 -> q
                t < 2f / 3 -> p + (q - p) * (2f / 3 - t) * 6
                else -> p
            }
            return (v * 255).toInt().coerceIn(0, 255)
        }
        return intArrayOf(hue(h + 1f / 3), hue(h), hue(h - 1f / 3))
    }
}
