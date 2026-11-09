package om.yousef.shashati.remote.atv

import java.io.ByteArrayOutputStream
import java.io.File
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The TV remembers a phone by its TLS client certificate. We make one self-signed
 * RSA certificate per phone and keep it in the app's private folder.
 */
class ClientIdentity(val key: PrivateKey, val cert: X509Certificate) {
    companion object {
        fun loadOrCreate(dir: File, commonName: String): ClientIdentity {
            val keyFile = File(dir, "remote_key.der")
            val certFile = File(dir, "remote_cert.der")
            if (keyFile.exists() && certFile.exists()) {
                runCatching {
                    val key = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(keyFile.readBytes()))
                    val cert = CertificateFactory.getInstance("X.509")
                        .generateCertificate(certFile.inputStream()) as X509Certificate
                    return ClientIdentity(key, cert)
                }
            }
            val id = create(commonName)
            dir.mkdirs()
            keyFile.writeBytes(id.key.encoded)
            certFile.writeBytes(id.cert.encoded)
            return id
        }

        fun create(commonName: String): ClientIdentity {
            val kp = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
            val sha256Rsa = seq(oid(byteArrayOf(0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x0B)), byteArrayOf(0x05, 0x00))
            val name = seq(set(seq(oid(byteArrayOf(0x55, 0x04, 0x03)), tlv(0x0C, commonName.toByteArray()))))
            val now = System.currentTimeMillis()
            val validity = seq(utc(Date(now - 86_400_000L)), utc(Date(now + 20L * 365 * 86_400_000L)))
            val serial = tlv(0x02, BigInteger.valueOf(now).toByteArray())
            val version = tlv(0xA0, tlv(0x02, byteArrayOf(2)))
            val tbs = seq(version, serial, sha256Rsa, name, validity, name, kp.public.encoded)
            val sig = Signature.getInstance("SHA256withRSA").run {
                initSign(kp.private); update(tbs); sign()
            }
            val der = seq(tbs, sha256Rsa, tlv(0x03, byteArrayOf(0) + sig))
            val cert = CertificateFactory.getInstance("X.509").generateCertificate(der.inputStream()) as X509Certificate
            return ClientIdentity(kp.private, cert)
        }

        private fun len(n: Int): ByteArray = when {
            n < 0x80 -> byteArrayOf(n.toByte())
            n < 0x100 -> byteArrayOf(0x81.toByte(), n.toByte())
            n < 0x10000 -> byteArrayOf(0x82.toByte(), (n shr 8).toByte(), n.toByte())
            else -> byteArrayOf(0x83.toByte(), (n shr 16).toByte(), (n shr 8).toByte(), n.toByte())
        }

        private fun tlv(tag: Int, body: ByteArray): ByteArray {
            val o = ByteArrayOutputStream()
            o.write(tag); o.write(len(body.size)); o.write(body)
            return o.toByteArray()
        }

        private fun concat(parts: Array<out ByteArray>): ByteArray {
            val o = ByteArrayOutputStream(); parts.forEach { o.write(it) }; return o.toByteArray()
        }

        private fun seq(vararg parts: ByteArray) = tlv(0x30, concat(parts))
        private fun set(vararg parts: ByteArray) = tlv(0x31, concat(parts))
        private fun oid(body: ByteArray) = tlv(0x06, body)
        private fun utc(d: Date): ByteArray {
            val f = SimpleDateFormat("yyMMddHHmmss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            return tlv(0x17, f.format(d).toByteArray())
        }
    }
}
