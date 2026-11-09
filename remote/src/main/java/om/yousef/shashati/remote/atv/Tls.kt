package om.yousef.shashati.remote.atv

import java.net.InetSocketAddress
import java.net.Socket
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object Tls {
    /** The TV uses its own self-signed certificate, so any server certificate is accepted. */
    private val trustAll = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    fun connect(host: String, port: Int, id: ClientIdentity, timeoutMs: Int = 5000): SSLSocket {
        val ks = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setKeyEntry("me", id.key, CharArray(0), arrayOf(id.cert))
        }
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply { init(ks, CharArray(0)) }
        val ctx = SSLContext.getInstance("TLS").apply { init(kmf.keyManagers, arrayOf<TrustManager>(trustAll), null) }
        val raw = Socket()
        raw.connect(InetSocketAddress(host, port), timeoutMs)
        val s = ctx.socketFactory.createSocket(raw, host, port, true) as SSLSocket
        s.soTimeout = timeoutMs
        s.startHandshake()
        return s
    }
}
