package om.yousef.shashati.remote.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** A TV this phone has been set up with. */
data class SavedTv(
    val host: String,
    val name: String,
    val token: String? = null,      // شاشتي pairing (channels, apps, typing)
    val keysPaired: Boolean = false, // Google remote pairing (arrows, OK, power…)
)

object TvStore {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.getSharedPreferences("remote", Context.MODE_PRIVATE)
    }

    fun all(): List<SavedTv> {
        val raw = sp.getString("tvs", null) ?: return emptyList()
        return runCatching {
            val a = JSONArray(raw)
            (0 until a.length()).map {
                val o = a.getJSONObject(it)
                SavedTv(o.getString("host"), o.getString("name"), o.optString("token").ifBlank { null }, o.optBoolean("keys"))
            }
        }.getOrDefault(emptyList())
    }

    fun save(tv: SavedTv) {
        val list = all().filter { it.host != tv.host } + tv
        write(list)
    }

    fun remove(host: String) = write(all().filter { it.host != host })

    fun get(host: String) = all().firstOrNull { it.host == host }

    private fun write(list: List<SavedTv>) {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().put("host", it.host).put("name", it.name).put("token", it.token ?: "").put("keys", it.keysPaired))
        }
        sp.edit().putString("tvs", a.toString()).apply()
    }

    var lastHost: String?
        get() = sp.getString("last", null)
        set(v) { sp.edit().putString("last", v).apply() }
}
