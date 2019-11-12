package com.yy.mobile.jdwp.transport

import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * @author YvesCheung
 * 2019/11/11
 */
class JdwpPacketTransmitter {

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

    fun fromByteArray(byteArray: ByteArray) {
        if (byteArray.size < 11) {
            throw IOException("packet is insufficient size")
        } else {
            val packetReader = ReaderImpl(byteArray)
            val length = packetReader.readInt()
            if (length != byteArray.size) {
                throw IOException("length size mis-match")
            } else {

            }
        }
    }

    private class WriterImpl(val dataStream: ByteArrayOutputStream) : JdwpWriter {

        override fun writeString(str: String) {
            val var2 = str.toByteArray(charset("UTF8"))
            writeInt(var2.size)
            writeByteArray(var2)
        }

        override fun writeInt(int: Int) {
            dataStream.write(int)
        }

        override fun writeByte(byte: Byte) {
            writeInt(byte.toInt())
        }

        override fun writeByteArray(byteArray: ByteArray) {
            dataStream.write(byteArray, 0, byteArray.size)
        }

        fun getData(): ByteArray = dataStream.toByteArray()
    }

    private class ReaderImpl(val data: ByteArray) : JdwpReader {

        private var inCursor = 0

        override fun readInt(): Int {
            val var1 = data[inCursor++].toInt()
            val var2 = data[inCursor++].toInt()
            val var3 = data[inCursor++].toInt()
            val var4 = data[inCursor++].toInt()
            return (var1 shl 24) + (var2 shl 16) + (var3 shl 8) + var4
        }
    }
}