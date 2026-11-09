package om.yousef.shashati.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import om.yousef.shashati.R
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Channel(
    val id: String,
    val name: String,
    val category: String,
    val color: Long,
    val urls: List<String>,
    val headers: Map<String, String>,
    val logo: String? = null,
)

data class Category(val id: String, val name: String)

data class ChannelList(val categories: List<Category>, val channels: List<Channel>)

object ChannelRepository {
    /** The list lives in the GitHub repo so it can be fixed without reinstalling the app. */
    const val REMOTE_URL = "https://raw.githubusercontent.com/youbalushi/tv-launcher/main/channels.json"

    @Volatile
    private var cached: ChannelList? = null

    fun load(context: Context): ChannelList {
        cached?.let { return it }
        val fromPrefs = Prefs.channelsJson?.let { runCatching { parse(it) }.getOrNull() }
        val list = fromPrefs ?: parse(
            context.resources.openRawResource(R.raw.channels).bufferedReader().use { it.readText() }
        )
        cached = list
        return list
    }

    /** Downloads the latest list. Returns true when it changed. */
    suspend fun refresh(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(REMOTE_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.useCaches = false
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val parsed = parse(text)
            if (parsed.channels.isEmpty()) return@runCatching false
            val changed = text != Prefs.channelsJson
            Prefs.channelsJson = text
            cached = parsed
            changed
        }.getOrDefault(false)
    }

    fun favorites(): Set<String> =
        Prefs.channelFavs?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()

    fun toggleFavorite(id: String) {
        val f = favorites().toMutableSet()
        if (!f.add(id)) f.remove(id)
        Prefs.channelFavs = f.joinToString(",")
    }

    private fun parse(text: String): ChannelList {
        val root = JSONObject(text)
        val cats = root.getJSONArray("categories")
        val categories = (0 until cats.length()).map {
            val c = cats.getJSONObject(it)
            Category(c.getString("id"), c.getString("name"))
        }
        val arr = root.getJSONArray("channels")
        val channels = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val urls = o.getJSONArray("urls").let { u -> (0 until u.length()).map { u.getString(it) } }
            val headers = mutableMapOf<String, String>()
            o.optJSONObject("headers")?.let { h ->
                h.keys().forEach { k -> headers[k] = h.getString(k) }
            }
            val color = runCatching {
                java.lang.Long.parseLong(o.optString("color", "#2A4F7A").removePrefix("#"), 16) or 0xFF000000
            }.getOrDefault(0xFF2A4F7A)
            Channel(o.getString("id"), o.getString("name"), o.optString("category"), color, urls, headers, o.optString("logo").ifBlank { null })
        }
        return ChannelList(categories, channels)
    }
}
