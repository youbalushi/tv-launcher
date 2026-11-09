package om.yousef.shashati.remote.atv

import java.math.BigInteger
import java.security.MessageDigest
import java.security.interfaces.RSAPublicKey
import javax.net.ssl.SSLSocket

/**
 * Pairing with Google's "Android TV Remote Service" (port 6467).
 * start() makes the TV show a 6-character code; finish(code) completes pairing.
 */
class AtvPairing(private val host: String, private val id: ClientIdentity, private val clientName: String) {
    private var socket: SSLSocket? = null

    class PairingException(message: String) : Exception(message)

    private fun send(build: Pb.() -> Unit) {
        val body = Pb().int(1, 2).int(2, 200).apply(build).toByteArray()
        Pb.writeFrame(socket!!.outputStream, body)
    }

    private fun receive(): PbMsg {
        val m = Pb.readFrame(socket!!.inputStream)
        val status = m.int(2, 200)
        if (status != 200) throw PairingException(if (status == 402) "bad_code" else "status_$status")
        return m
    }

    /** Opens the connection and asks the TV to display the code. */
    fun start() {
        close()
        socket = Tls.connect(host, 6467, id, timeoutMs = 10_000)
        send { msg(10) { string(1, "atvremote"); string(2, clientName) } }
        receive() // PairingRequestAck
        send { msg(20) { msg(1) { int(1, 3); int(2, 6) }; int(3, 1) } }
        receive() // Options
        send { msg(30) { msg(1) { int(1, 3); int(2, 6) }; int(2, 1) } }
        receive() // ConfigurationAck – the code is now on the TV screen
        socket!!.soTimeout = 120_000
    }

    /** Returns false when the code is wrong (the TV keeps showing it, so the user can retry). */
    fun finish(code: String): Boolean {
        val s = socket ?: throw PairingException("not_started")
        val clean = code.trim().uppercase()
        if (!clean.matches(Regex("[0-9A-F]{6}"))) return false
        val secret = secret(id.cert.publicKey as RSAPublicKey, s.session.peerCertificates[0].publicKey as RSAPublicKey, clean)
        if (secret[0] != clean.substring(0, 2).toInt(16).toByte()) return false
        send { msg(40) { bytes(1, secret) } }
        receive() // SecretAck
        close()
        return true
    }

    fun close() {
        runCatching { socket?.close() }
        socket = null
    }

    companion object {
        private fun unsigned(b: BigInteger): ByteArray {
            val a = b.toByteArray()
            var i = 0
            while (i < a.size - 1 && a[i] == 0.toByte()) i++
            return a.copyOfRange(i, a.size)
        }

        fun secret(client: RSAPublicKey, server: RSAPublicKey, code: String): ByteArray {
            val d = MessageDigest.getInstance("SHA-256")
            d.update(unsigned(client.modulus))
            d.update(unsigned(client.publicExponent))
            d.update(unsigned(server.modulus))
            d.update(unsigned(server.publicExponent))
            d.update(code.substring(2).chunked(2).map { it.toInt(16).toByte() }.toByteArray())
            return d.digest()
        }
    }
}
