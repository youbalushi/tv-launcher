package om.yousef.shashati.remote.net

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Talks to the small service inside شاشتي on the TV (port 8765). */
class ShashatiApi(private val host: String, private val token: String?) {

    private fun url(path: String, query: Map<String, String> = emptyMap()): URL {
        val q = query.entries.joinToString("&") { "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" }
        return URL("http://$host:8765$path" + if (q.isEmpty()) "" else "?$q")
    }

    private fun open(path: String, query: Map<String, String>, body: String?): HttpURLConnection {
        val c = url(path, query).openConnection() as HttpURLConnection
        c.connectTimeout = 4000
        c.readTimeout = 8000
        token?.let { c.setRequestProperty("X-Token", it) }
        if (body != null) {
            c.requestMethod = "POST"
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            c.outputStream.use { it.write(body.toByteArray()) }
        }
        return c
    }

    suspend fun call(path: String, query: Map<String, String> = emptyMap(), body: String? = null): JSONObject? =
        withContext(Dispatchers.IO) {
            runCatching {
                val c = open(path, query, body)
                val stream = if (c.responseCode in 200..299) c.inputStream else c.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
                c.disconnect()
                JSONObject(text).put("_http", c.responseCode)
            }.getOrNull()
        }

    suspend fun icon(pkg: String): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val c = open("/icon", mapOf("pkg" to pkg), null)
            if (c.responseCode != 200) return@runCatching null
            c.inputStream.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }

    suspend fun info() = call("/info")
    suspend fun pairStart() = call("/pair/start")
    suspend fun pairFinish(code: String, phoneName: String) = call("/pair/finish", mapOf("code" to code, "name" to phoneName))
    suspend fun channels() = call("/channels")
    suspend fun play(id: String) = call("/play", mapOf("id" to id))
    suspend fun apps() = call("/apps")
    suspend fun launch(pkg: String) = call("/launch", mapOf("pkg" to pkg))
    suspend fun text(t: String) = call("/text", body = JSONObject().put("text", t).toString())
    suspend fun volume(d: String) = call("/volume", mapOf("d" to d))
    suspend fun home() = call("/home")
    suspend fun back() = call("/back")
}
