package om.yousef.shashati.remote.atv

import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocket

/**
 * Live connection to the TV's remote service (port 6466) after pairing.
 * Keys are queued and sent on a background thread so the UI never waits.
 */
class AtvRemote(
    private val host: String,
    private val id: ClientIdentity,
    private val onState: (State) -> Unit,
) {
    enum class State { Connecting, Connected, NeedsPairing, Offline }

    @Volatile private var socket: SSLSocket? = null
    @Volatile private var running = false
    @Volatile var powerOn: Boolean? = null; private set
    private val queue = LinkedBlockingQueue<ByteArray>()
    private val pool = Executors.newFixedThreadPool(2)

    fun start() {
        if (running) return
        running = true
        pool.execute { loop() }
    }

    fun stop() {
        running = false
        runCatching { socket?.close() }
        pool.shutdownNow()
    }

    fun key(code: Int, direction: Int = SHORT) {
        queue.offer(Pb().msg(10) { int(1, code); int(2, direction) }.toByteArray())
    }

    fun openLink(link: String) {
        queue.offer(Pb().msg(90) { string(1, link) }.toByteArray())
    }

    private fun loop() {
        var attempt = 0
        while (running) {
            onState(State.Connecting)
            try {
                val s = Tls.connect(host, 6466, id, timeoutMs = 6000)
                s.soTimeout = 0
                socket = s
                attempt = 0
                queue.clear()
                val writer = pool.submit {
                    try {
                        while (running && !s.isClosed) {
                            val m = queue.poll(1, TimeUnit.SECONDS) ?: continue
                            synchronized(s) { Pb.writeFrame(s.outputStream, m) }
                        }
                    } catch (_: Exception) { runCatching { s.close() } }
                }
                read(s)
                writer.cancel(true)
            } catch (e: Exception) {
                val msg = e.toString()
                // A TV that doesn't know our certificate closes the handshake.
                if (msg.contains("SSLHandshake") || msg.contains("certificate", true) || msg.contains("bad_certificate")) {
                    onState(State.NeedsPairing)
                    running = false
                    return
                }
            } finally {
                runCatching { socket?.close() }
                socket = null
            }
            if (!running) break
            onState(State.Offline)
            attempt++
            Thread.sleep((1000L * attempt).coerceAtMost(5000))
        }
    }

    private fun read(s: SSLSocket) {
        val input = s.inputStream
        while (running) {
            val m = Pb.readFrame(input)
            fun reply(body: ByteArray) = synchronized(s) { Pb.writeFrame(s.outputStream, body) }
            when {
                m.has(1) -> reply(Pb().msg(1) {
                    int(1, FEATURES)
                    msg(2) {
                        string(1, android.os.Build.MODEL ?: "phone")
                        string(2, android.os.Build.MANUFACTURER ?: "android")
                        int(3, 1); string(4, "1"); string(5, "atvremote"); string(6, "1.0.0")
                    }
                }.toByteArray())
                m.has(2) -> { reply(Pb().msg(2) { int(1, FEATURES) }.toByteArray()); onState(State.Connected) }
                m.has(8) -> reply(Pb().msg(9) { int(1, m.msg(8)!!.int(1)) }.toByteArray())
                m.has(40) -> powerOn = m.msg(40)!!.long(1) != 0L
            }
        }
    }

    companion object {
        const val SHORT = 3
        const val START_LONG = 1
        const val END_LONG = 2
        // ping | key | power | volume | app link
        const val FEATURES = 1 or 2 or 32 or 64 or 512

        const val UP = 19; const val DOWN = 20; const val LEFT = 21; const val RIGHT = 22; const val OK = 23
        const val BACK = 4; const val HOME = 3; const val MENU = 82; const val POWER = 26
        const val VOL_UP = 24; const val VOL_DOWN = 25; const val MUTE = 164
        const val PLAY_PAUSE = 85; const val REWIND = 89; const val FORWARD = 90
        const val CH_UP = 166; const val CH_DOWN = 167; const val ENTER = 66; const val DEL = 67
    }
}
