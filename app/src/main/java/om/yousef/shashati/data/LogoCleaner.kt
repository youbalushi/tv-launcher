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

    private fun lighten(px: IntArray) {
        var dark = 0
        var visible = 0
        for (c in px) {
            if (a(c) < 128) continue
            visible++
            val mx = max(r(c), max(g(c), b(c)))
            val mn = min(r(c), min(g(c), b(c)))
            if (mx < 90 && mx - mn < 40) dark++
        }
        if (visible == 0 || dark < visible * 0.15f) return
        for (i in px.indices) {
            val c = px[i]
            if (a(c) == 0) continue
            val mx = max(r(c), max(g(c), b(c)))
            val mn = min(r(c), min(g(c), b(c)))
            val sat = if (mx == 0) 0f else (mx - mn).toFloat() / mx
            if (sat < 0.25f && mx < 140) {
                val v = 255 - abs(mx - 20) / 2
                px[i] = (a(c) shl 24) or (v shl 16) or (v shl 8) or v
            }
        }
    }
}
