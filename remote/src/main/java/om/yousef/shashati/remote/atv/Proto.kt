package om.yousef.shashati.remote.atv

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

/** Just enough protobuf to talk to the Android TV remote service (no generated code needed). */
class Pb {
    private val out = ByteArrayOutputStream()

    fun varint(field: Int, value: Long): Pb { tag(field, 0); writeVarint(out, value); return this }
    fun int(field: Int, value: Int) = varint(field, value.toLong())
    fun bool(field: Int, value: Boolean) = varint(field, if (value) 1 else 0)
    fun bytes(field: Int, value: ByteArray): Pb { tag(field, 2); writeVarint(out, value.size.toLong()); out.write(value); return this }
    fun string(field: Int, value: String) = bytes(field, value.toByteArray(Charsets.UTF_8))
    fun msg(field: Int, build: Pb.() -> Unit) = bytes(field, Pb().apply(build).toByteArray())

    fun toByteArray(): ByteArray = out.toByteArray()

    private fun tag(field: Int, wire: Int) = writeVarint(out, ((field shl 3) or wire).toLong())

    companion object {
        fun writeVarint(out: OutputStream, v: Long) {
            var x = v
            while (true) {
                if (x and 0x7FL.inv() == 0L) { out.write(x.toInt()); return }
                out.write(((x and 0x7F) or 0x80).toInt())
                x = x ushr 7
            }
        }

        fun readVarint(input: InputStream): Long {
            var shift = 0
            var result = 0L
            while (true) {
                val b = input.read()
                if (b < 0) throw EOFException()
                result = result or ((b and 0x7F).toLong() shl shift)
                if (b and 0x80 == 0) return result
                shift += 7
            }
        }

        /** Messages on both ports are sent as <varint length><bytes>. */
        fun writeFrame(out: OutputStream, body: ByteArray) {
            val b = ByteArrayOutputStream()
            writeVarint(b, body.size.toLong())
            b.write(body)
            out.write(b.toByteArray())
            out.flush()
        }

        fun readFrame(input: InputStream): PbMsg {
            val len = readVarint(input).toInt()
            require(len in 0..1_000_000) { "bad frame" }
            val buf = ByteArray(len)
            var off = 0
            while (off < len) {
                val n = input.read(buf, off, len - off)
                if (n < 0) throw EOFException()
                off += n
            }
            return PbMsg.parse(buf)
        }
    }
}

/** A parsed message: field number -> values (Long for numbers, ByteArray for everything else). */
class PbMsg(private val fields: Map<Int, List<Any>>) {
    fun has(field: Int) = fields.containsKey(field)
    fun long(field: Int, default: Long = 0) = (fields[field]?.firstOrNull() as? Long) ?: default
    fun int(field: Int, default: Int = 0) = long(field, default.toLong()).toInt()
    fun bytes(field: Int) = fields[field]?.firstOrNull() as? ByteArray
    fun string(field: Int) = bytes(field)?.toString(Charsets.UTF_8)
    fun msg(field: Int) = bytes(field)?.let { parse(it) }
    val fieldNumbers: Set<Int> get() = fields.keys

    companion object {
        fun parse(data: ByteArray): PbMsg {
            val input = data.inputStream()
            val map = LinkedHashMap<Int, MutableList<Any>>()
            while (input.available() > 0) {
                val key = Pb.readVarint(input)
                val field = (key ushr 3).toInt()
                val value: Any = when ((key and 7).toInt()) {
                    0 -> Pb.readVarint(input)
                    2 -> {
                        val n = Pb.readVarint(input).toInt()
                        ByteArray(n).also { input.read(it) }
                    }
                    1 -> ByteArray(8).also { input.read(it) }
                    5 -> ByteArray(4).also { input.read(it) }
                    else -> throw IllegalStateException("wire type")
                }
                map.getOrPut(field) { mutableListOf() }.add(value)
            }
            return PbMsg(map)
        }
    }
}
