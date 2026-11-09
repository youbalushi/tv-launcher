package om.yousef.shashati.remote.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import java.util.ArrayDeque

data class FoundTv(val host: String, val name: String, val hasKeys: Boolean, val hasShashati: Boolean)

/** Finds TVs on the home Wi-Fi: Google's remote service and the شاشتي service. */
class Discovery(context: Context, private val onChange: (List<FoundTv>) -> Unit) {
    private val app = context.applicationContext
    private val nsd = app.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val main = Handler(Looper.getMainLooper())
    private val found = LinkedHashMap<String, FoundTv>()
    private val listeners = mutableListOf<NsdManager.DiscoveryListener>()
    private val pending = ArrayDeque<NsdServiceInfo>()
    private var resolving = false
    private var lock: WifiManager.MulticastLock? = null

    fun start() {
        lock = runCatching {
            (app.getSystemService(Context.WIFI_SERVICE) as WifiManager).createMulticastLock("remote").apply {
                setReferenceCounted(false); acquire()
            }
        }.getOrNull()
        listOf(KEYS, SHASHATI).forEach { type ->
            val l = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(t: String) {}
                override fun onDiscoveryStopped(t: String) {}
                override fun onStartDiscoveryFailed(t: String, e: Int) {}
                override fun onStopDiscoveryFailed(t: String, e: Int) {}
                override fun onServiceLost(info: NsdServiceInfo) {}
                override fun onServiceFound(info: NsdServiceInfo) {
                    main.post { pending.add(info); next() }
                }
            }
            runCatching { nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, l); listeners.add(l) }
        }
    }

    fun stop() {
        listeners.forEach { runCatching { nsd.stopServiceDiscovery(it) } }
        listeners.clear()
        runCatching { lock?.release() }
    }

    // Older Android versions can only resolve one service at a time.
    private fun next() {
        if (resolving) return
        val info = pending.pollFirst() ?: return
        resolving = true
        @Suppress("DEPRECATION")
        nsd.resolveService(info, object : NsdManager.ResolveListener {
            override fun onResolveFailed(i: NsdServiceInfo, e: Int) {
                main.post { resolving = false; next() }
            }

            override fun onServiceResolved(i: NsdServiceInfo) {
                @Suppress("DEPRECATION")
                val host = i.host?.hostAddress
                main.post {
                    resolving = false
                    if (host != null && !host.contains(":")) {
                        val isKeys = i.serviceType.contains("androidtvremote2")
                        val old = found[host]
                        val name = if (!isKeys || old == null) i.serviceName else old.name
                        found[host] = FoundTv(
                            host, name,
                            hasKeys = isKeys || old?.hasKeys == true,
                            hasShashati = !isKeys || old?.hasShashati == true,
                        )
                        onChange(found.values.toList())
                    }
                    next()
                }
            }
        })
    }

    companion object {
        const val KEYS = "_androidtvremote2._tcp."
        const val SHASHATI = "_shashati._tcp."
    }
}
