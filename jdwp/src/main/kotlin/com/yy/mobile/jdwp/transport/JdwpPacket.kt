package com.yy.mobile.jdwp.transport

import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger

/**
 * JDWP规范的一种实现
 *
 * https://docs.oracle.com/javase/7/docs/technotes/guides/jpda/jdwp-spec.html
 *
 * @author YvesCheung
 * 2019/11/8
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class JdwpPacket(
    /**
     * # command set
     *
     * This field is useful as a means for grouping commands in a meaningful way.
     * The Sun defined command sets are used to group commands by the interfaces they support in the JDI.
     * For example, all commands that support the JDI VirtualMachine interface are
     * grouped in a VirtualMachine command set.
     *
     * The command set space is roughly divided as follows:
     *
     * - 0 - 63
     * Sets of commands sent to the target VM
     * - 64 - 127
     * Sets of commands sent to the debugger
     * - 128 - 256
     * Vendor-defined commands and extensions.
     */
    protected val cmdSet: Byte,
    /**
     * # command
     *
     * This field identifies a particular command in a command set.
     * This field, together with the command set field, is used to indicate how the command
     * packet should be processed. More succinctly, they tell the receiver what to do.
     * Specific commands are presented later in this document.
     */
    protected val cmd: Byte
) {

    companion object {

        /**
         * # id
         *
         * The id field is used to uniquely identify each packet command/reply pair.
         * A reply packet has the same id as the command packet to which it replies.
         * This allows asynchronous commands and replies to be matched.
         * The id field must be unique among all outstanding commands sent from one source.
         * (Outstanding commands originating from the debugger may use the same id as
         * outstanding commands originating from the target VM.)
         * Other than that, there are no requirements on the allocation of id's.
         * A simple monotonic counter should be adequate for most implementations.
         * It will allow 2^32 unique outstanding packets and is the simplest implementation alternative.
         */
        private val idGenerator = AtomicInteger(1)

        /**
         * # flags
         *
         * Flags are used to alter how any command is queued and processed and to tag command packets that
         * originate from the target VM. There is currently one flag bits defined; future versions of the
         * protocol may define additional flags.
         *
         * - 0x80
         * Reply packet
         * The reply bit, when set, indicates that this packet is a reply.
         */
        private const val NO_FLAG: Byte = 0
        private const val REPLAY_FLAG: Byte = 0x80.toByte()

        /**
         * # length
         *
         * The length field is the size, in bytes, of the entire packet,
         * including the length field. The header size is 11 bytes, so a packet with no data
         * would set this field to 11.
         */
        private const val PACKET_HEAD_SIZE = 11
    }

    protected open val id: Int = idGenerator.getAndIncrement()

    abstract fun to(writer: JdwpWriter)

    abstract fun from(reader: JdwpReader)

    fun toByteArray(): ByteArray {
        val packetWriter = WriterImpl(ByteArrayOutputStream())
        val contentWriter = WriterImpl(ByteArrayOutputStream())

        val content = contentWriter.apply(::to).getData()
        val length = content.size + PACKET_HEAD_SIZE

        //4 byte for length
        packetWriter.writeInt(length)

        //4 byte for id
        packetWriter.writeInt(id)

        //1 byte for flag
        packetWriter.writeByte(NO_FLAG)

        //1 byte for cmd set
        packetWriter.writeByte(cmdSet)

        //1 byte for cmd
        packetWriter.writeByte(cmd)

        //other for packet content
        packetWriter.writeByteArray(content)

        return packetWriter.getData()
    }

    fun fromByteArray(byteArray: ByteArray) {

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
}