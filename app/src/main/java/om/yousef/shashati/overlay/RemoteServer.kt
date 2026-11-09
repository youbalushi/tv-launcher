package om.yousef.shashati.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import om.yousef.shashati.MainActivity
import om.yousef.shashati.PlayerActivity
import om.yousef.shashati.R
import om.yousef.shashati.data.AppsRepository
import om.yousef.shashati.data.ChannelRepository
import om.yousef.shashati.data.Prefs
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.security.SecureRandom
import java.util.concurrent.Executors

/**
 * Small local web service that lets the "ريموت شاشتي" phone app list channels and apps,
 * start them, and type into the TV. A phone must first pair with a 4-digit code shown on the TV.
 */
class RemoteServer(private val service: AccessibilityService) {
    private val main = Handler(Looper.getMainLooper())
    private val pool = Executors.newCachedThreadPool()
    private var server: ServerSocket? = null
    private var nsd: NsdManager? = null
    private var nsdListener: NsdManager.RegistrationListener? = null
    private var codeView: View? = null

    @Volatile private var pairCode: String? = null
    @Volatile private var pairCodeUntil = 0L
    @Volatile private var pairFails = 0
    private val iconCache = HashMap<String, ByteArray>()

    fun start() {
        if (server != null) return
        pool.execute {
            try {
                val ss = ServerSocket(PORT)
                server = ss
                main.post { advertise() }
                while (!ss.isClosed) {
                    val c = ss.accept()
                    pool.execute { handle(c) }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun stop() {
        runCatching { server?.close() }
        server = null
        runCatching { nsdListener?.let { nsd?.unregisterService(it) } }
        hideCode()
        pool.shutdownNow()
    }

    private fun tvName(): String =
        Settings.Global.getString(service.contentResolver, "device_name")
            ?: Settings.Secure.getString(service.contentResolver, "bluetooth_name")
            ?: Build.MODEL

    private fun advertise() {
        val info = NsdServiceInfo().apply {
            serviceName = tvName()
            serviceType = "_shashati._tcp."
            port = PORT
        }
        val l = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(i: NsdServiceInfo) {}
            override fun onRegistrationFailed(i: NsdServiceInfo, e: Int) {}
            override fun onServiceUnregistered(i: NsdServiceInfo) {}
            override fun onUnregistrationFailed(i: NsdServiceInfo, e: Int) {}
        }
        runCatching {
            nsd = service.getSystemService(Context.NSD_SERVICE) as NsdManager
            nsd!!.registerService(info, NsdManager.PROTOCOL_DNS_SD, l)
            nsdListener = l
        }
    }

    // ---------- HTTP ----------

    private class Req(val method: String, val path: String, val query: Map<String, String>, val headers: Map<String, String>, val body: String)
    private class Res(val code: Int, val type: String, val body: ByteArray) {
        constructor(json: JSONObject, code: Int = 200) : this(code, "application/json; charset=utf-8", json.toString().toByteArray())
    }

    private fun handle(c: Socket) {
        c.use { sock ->
            runCatching {
                sock.soTimeout = 10_000
                val input = BufferedInputStream(sock.getInputStream())
                val req = parse(input) ?: return
                val res = runCatching { route(req) }.getOrElse { Res(JSONObject().put("error", it.toString()), 500) }
                val out = sock.getOutputStream()
                val head = "HTTP/1.1 ${res.code} OK\r\nContent-Type: ${res.type}\r\nContent-Length: ${res.body.size}\r\nConnection: close\r\n\r\n"
                out.write(head.toByteArray())
                out.write(res.body)
                out.flush()
            }
        }
    }

    private fun readLine(input: InputStream): String? {
        val b = ByteArrayOutputStream()
        while (true) {
            val ch = input.read()
            if (ch < 0) return if (b.size() == 0) null else b.toString("UTF-8")
            if (ch == '\n'.code) return b.toString("UTF-8").trimEnd('\r')
            b.write(ch)
            if (b.size() > 8192) return null
        }
    }

    private fun parse(input: InputStream): Req? {
        val first = readLine(input) ?: return null
        val parts = first.split(" ")
        if (parts.size < 2) return null
        val headers = HashMap<String, String>()
        while (true) {
            val l = readLine(input) ?: break
            if (l.isEmpty()) break
            val i = l.indexOf(':')
            if (i > 0) headers[l.substring(0, i).trim().lowercase()] = l.substring(i + 1).trim()
        }
        val len = headers["content-length"]?.toIntOrNull()?.coerceIn(0, 100_000) ?: 0
        val body = ByteArray(len)
        var off = 0
        while (off < len) { val n = input.read(body, off, len - off); if (n < 0) break; off += n }
        val full = parts[1]
        val q = full.substringAfter('?', "")
        val query = q.split("&").filter { it.contains("=") }.associate {
            URLDecoder.decode(it.substringBefore("="), "UTF-8") to URLDecoder.decode(it.substringAfter("="), "UTF-8")
        }
        return Req(parts[0], full.substringBefore('?'), query, headers, String(body, Charsets.UTF_8))
    }

    private fun ok(extra: JSONObject.() -> Unit = {}) = Res(JSONObject().put("ok", true).apply(extra))
    private fun fail(reason: String, code: Int = 200) = Res(JSONObject().put("ok", false).put("reason", reason), code)

    private fun tokens(): Map<String, String> =
        Prefs.remoteTokensRaw?.lines()?.filter { it.contains("|") }?.associate { it.substringBefore("|") to it.substringAfter("|") } ?: emptyMap()

    private fun route(r: Req): Res {
        when (r.path) {
            "/info" -> return ok { put("name", tvName()); put("app", "shashati"); put("version", 1); put("kids", Prefs.kidsMode) }
            "/pair/start" -> {
                val code = (1000 + SecureRandom().nextInt(9000)).toString()
                pairCode = code
                pairCodeUntil = System.currentTimeMillis() + 90_000
                pairFails = 0
                main.post { showCode(code) }
                return ok()
            }
            "/pair/finish" -> {
                val code = pairCode
                if (code == null || System.currentTimeMillis() > pairCodeUntil) return fail("expired")
                if (r.query["code"] != code) {
                    if (++pairFails >= 5) { pairCode = null; main.post { hideCode() } }
                    return fail("bad_code")
                }
                pairCode = null
                main.post { hideCode() }
                val token = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
                val name = (r.query["name"] ?: "جوال").replace("\n", " ").replace("|", " ").take(40)
                Prefs.remoteTokensRaw = (tokens() + (token to name)).entries.joinToString("\n") { "${it.key}|${it.value}" }
                return ok { put("token", token); put("name", tvName()) }
            }
        }

        if (r.headers["x-token"].let { it == null || it !in tokens() }) return fail("unauthorized", 401)

        return when (r.path) {
            "/channels" -> channels()
            "/play" -> play(r.query["id"] ?: "")
            "/apps" -> apps()
            "/icon" -> icon(r.query["pkg"] ?: "")
            "/launch" -> launch(r.query["pkg"] ?: "")
            "/home" -> { openHome(); ok() }
            "/text" -> text(r.body)
            "/volume" -> volume(r.query["d"] ?: "")
            "/back" -> { service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK); ok() }
            else -> fail("not_found", 404)
        }
    }

    // ---------- actions ----------

    private fun channels(): Res {
        val list = ChannelRepository.load(service)
        val kids = Prefs.kidsMode
        val cats = JSONArray()
        list.categories.filter { !kids || it.id == "kids" }.forEach { cats.put(JSONObject().put("id", it.id).put("name", it.name)) }
        val arr = JSONArray()
        list.channels.filter { !kids || it.category == "kids" }.forEach {
            arr.put(JSONObject().put("id", it.id).put("name", it.name).put("category", it.category).put("color", it.color))
        }
        return ok {
            put("categories", cats); put("channels", arr)
            put("favorites", JSONArray(ChannelRepository.favorites().toList()))
        }
    }

    private fun play(id: String): Res {
        val all = ChannelRepository.load(service).channels.filter { !Prefs.kidsMode || it.category == "kids" }
        val ch = all.firstOrNull { it.id == id } ?: return fail("no_channel")
        // Up/down on the TV remote then moves within the same category.
        val group = all.filter { it.category == ch.category }
        val intent = PlayerActivity.intent(service, group, group.indexOf(ch))
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
        main.post { runCatching { service.startActivity(intent) } }
        return ok()
    }

    private fun apps(): Res {
        val kids = Prefs.kidsMode
        val allowed = Prefs.kidsApps()
        val arr = JSONArray()
        AppsRepository.installed(service).filter { !kids || it.packageName in allowed }.forEach {
            arr.put(JSONObject().put("pkg", it.packageName).put("label", it.label).put("locked", it.packageName in Prefs.lockedApps()))
        }
        return ok { put("apps", arr) }
    }

    private fun icon(pkg: String): Res {
        synchronized(iconCache) { iconCache[pkg] }?.let { return Res(200, "image/png", it) }
        val app = AppsRepository.installed(service).firstOrNull { it.packageName == pkg } ?: return fail("no_app", 404)
        val d = app.banner ?: app.icon ?: return fail("no_icon", 404)
        val banner = app.banner != null
        val w = if (banner) 320 else 160
        val h = if (banner) 180 else 160
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        d.setBounds(0, 0, w, h)
        d.draw(c)
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        val bytes = out.toByteArray()
        synchronized(iconCache) { iconCache[pkg] = bytes }
        return Res(200, "image/png", bytes)
    }

    private fun launch(pkg: String): Res {
        if (Prefs.kidsMode && pkg !in Prefs.kidsApps()) return fail("kids")
        if (pkg == "shashati.channels") {
            main.post { openHome() }
            return ok()
        }
        var launched = false
        val done = java.util.concurrent.CountDownLatch(1)
        main.post {
            launched = AppsRepository.launch(service, pkg)
            done.countDown()
        }
        done.await(3, java.util.concurrent.TimeUnit.SECONDS)
        return if (launched) ok() else fail("not_launched")
    }

    private fun openHome() {
        Prefs.originalAllowed = false
        val i = android.content.Intent(service, MainActivity::class.java)
            .setAction(android.content.Intent.ACTION_MAIN)
            .addCategory(android.content.Intent.CATEGORY_HOME)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { service.startActivity(i) }
    }

    /** Puts the phone's text into the text box that is selected on the TV (e.g. YouTube search). */
    private fun text(body: String): Res {
        val text = runCatching { JSONObject(body).optString("text") }.getOrDefault(body)
        var result = false
        val done = java.util.concurrent.CountDownLatch(1)
        main.post {
            val node = service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.takeIf { it.isEditable }
                ?: findEditable(service.rootInActiveWindow)
            if (node != null) {
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                result = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            }
            done.countDown()
        }
        done.await(3, java.util.concurrent.TimeUnit.SECONDS)
        return if (result) ok() else fail("no_field")
    }

    private fun findEditable(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        root ?: return null
        if (root.isEditable && root.isVisibleToUser) return root
        for (i in 0 until root.childCount) findEditable(root.getChild(i))?.let { return it }
        return null
    }

    private fun volume(d: String): Res {
        val am = service.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val dir = when (d) {
            "up" -> AudioManager.ADJUST_RAISE
            "down" -> AudioManager.ADJUST_LOWER
            "mute" -> AudioManager.ADJUST_TOGGLE_MUTE
            else -> return fail("bad")
        }
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, AudioManager.FLAG_SHOW_UI)
        return ok()
    }

    // ---------- pairing code on screen ----------

    private fun showCode(code: String) {
        hideCode()
        val font = runCatching { ResourcesCompat.getFont(service, R.font.plex_medium) }.getOrNull() ?: Typeface.DEFAULT
        val box = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(70, 44, 70, 44)
            background = GradientDrawable().apply {
                cornerRadius = 40f
                setColor(Color.parseColor("#E60A0F1C"))
                setStroke(3, Color.parseColor("#E2B45A"))
            }
            addView(TextView(service).apply {
                text = "رمز ربط ريموت شاشتي"
                typeface = font
                textSize = 22f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            })
            addView(TextView(service).apply {
                text = code.toCharArray().joinToString("  ")
                typeface = font
                textSize = 64f
                setTextColor(Color.parseColor("#E2B45A"))
                gravity = Gravity.CENTER
                textDirection = View.TEXT_DIRECTION_LTR
            })
            addView(TextView(service).apply {
                text = "اكتب هذا الرمز في تطبيق الريموت على الجوال"
                typeface = font
                textSize = 16f
                setTextColor(Color.parseColor("#B3FFFFFF"))
                gravity = Gravity.CENTER
            })
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
        runCatching {
            (service.getSystemService(Context.WINDOW_SERVICE) as WindowManager).addView(box, lp)
            codeView = box
        }
        main.postDelayed({ if (codeView === box) hideCode() }, 90_000)
    }

    private fun hideCode() {
        val v = codeView ?: return
        codeView = null
        runCatching { (service.getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(v) }
    }

    companion object {
        const val PORT = 8765
    }
}
