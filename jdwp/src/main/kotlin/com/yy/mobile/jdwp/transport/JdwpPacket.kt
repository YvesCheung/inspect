package com.yy.mobile.jdwp.transport

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
    val cmdSet: Byte,
    /**
     * # command
     *
     * This field identifies a particular command in a command set.
     * This field, together with the command set field, is used to indicate how the command
     * packet should be processed. More succinctly, they tell the receiver what to do.
     * Specific commands are presented later in this document.
     */
    val cmd: Byte
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
        const val NO_FLAG: Byte = 0
        const val REPLAY_FLAG: Byte = 0x80.toByte()

        /**
         * # length
         *
         * The length field is the size, in bytes, of the entire packet,
         * including the length field. The header size is 11 bytes, so a packet with no data
         * would set this field to 11.
         */
        const val PACKET_HEAD_SIZE = 11
    }

    open val id: Int = idGenerator.getAndIncrement()

    abstract fun to(writer: JdwpWriter)

    abstract fun from(errorCode: Short, reader: JdwpReader)
}