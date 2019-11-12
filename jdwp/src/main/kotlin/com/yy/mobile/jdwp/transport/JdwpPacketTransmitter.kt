package com.yy.mobile.jdwp.transport

import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * @author YvesCheung
 * 2019/11/11
 */
object JdwpPacketTransmitter {

    fun toByteArray(packet: JdwpPacket): ByteArray {
        val packetWriter = WriterImpl(ByteArrayOutputStream())
        val contentWriter = WriterImpl(ByteArrayOutputStream())

        val content = contentWriter.apply(packet::to).getData()
        val length = content.size + JdwpPacket.PACKET_HEAD_SIZE

        //4 byte for length
        packetWriter.writeInt(length)

        //4 byte for id
        packetWriter.writeInt(packet.id)

        //1 byte for flag
        packetWriter.writeByte(JdwpPacket.NO_FLAG)

        //1 byte for cmd set
        packetWriter.writeByte(packet.cmdSet)

        //1 byte for cmd
        packetWriter.writeByte(packet.cmd)

        //other for packet content
        packetWriter.writeByteArray(content)

        return packetWriter.getData()
    }

    fun fromByteArray(byteArray: ByteArray, findPacket: (id: Int) -> JdwpPacket?): JdwpPacket? {
        if (byteArray.size < 11) {
            throw IOException("packet is insufficient size")
        } else {
            val packetReader = ReaderImpl(byteArray)
            val length = packetReader.readInt()
            if (length != byteArray.size) {
                throw IOException(
                    "length size mis-match, " +
                            "expect = $length but actual = ${byteArray.size}"
                )
            }

            val id = packetReader.readInt()
            val packet = findPacket(id)
            if (packet != null) {
                val flag = packetReader.readByte()
                if (flag == JdwpPacket.REPLAY_FLAG) {
                    val errorCode = packetReader.readShort()
                    packet.from(errorCode, packetReader)
                    return packet
                }
            }
        }
        return null
    }

    private class WriterImpl(val dataStream: ByteArrayOutputStream) : JdwpWriter {

        override fun writeString(str: String) {
            val var2 = str.toByteArray(charset("UTF8"))
            writeInt(var2.size)
            writeByteArray(var2)
        }

        override fun writeInt(int: Int) {
            dataStream.write(int.ushr(24) and 255)
            dataStream.write(int.ushr(16) and 255)
            dataStream.write(int.ushr(8) and 255)
            dataStream.write(int.ushr(0) and 255)
        }

        override fun writeByte(byte: Byte) {
            dataStream.write(byte.toInt() and 255)
        }

        override fun writeByteArray(byteArray: ByteArray) {
            dataStream.write(byteArray, 0, byteArray.size)
        }

        fun getData(): ByteArray = dataStream.toByteArray()
    }

    private class ReaderImpl(val data: ByteArray) : JdwpReader {

        private var inCursor = 0

        override fun readInt(): Int {
            val var1 = data[inCursor++].toInt() and 255
            val var2 = data[inCursor++].toInt() and 255
            val var3 = data[inCursor++].toInt() and 255
            val var4 = data[inCursor++].toInt() and 255
            return (var1 shl 24) + (var2 shl 16) + (var3 shl 8) + var4
        }

        override fun readByte(): Byte {
            return data[inCursor++]
        }

        override fun readShort(): Short {
            val var1 = data[inCursor++].toInt() and 255
            val var2 = data[inCursor++].toInt() and 255
            return ((var1 shl 8) + var2).toShort()
        }
    }
}