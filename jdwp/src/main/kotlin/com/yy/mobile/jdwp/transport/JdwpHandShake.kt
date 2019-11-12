package com.yy.mobile.jdwp.transport

import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

/**
 * @author YvesCheung
 * 2019/11/12
 */
object JdwpHandShake {

    private val handshake = byteArrayOf(74, 68, 87, 80, 45, 72, 97, 110, 100, 115, 104, 97, 107, 101)

    fun sendHandshake(channel: SocketChannel) {
        val buffer = ByteBuffer.wrap(handshake)
        channel.write(buffer)
    }

    fun consumeHandshake(bytes: ByteArray): Boolean {
        if (bytes.size != handshake.size) {
            return false
        }
        return handshake.contentEquals(bytes)
    }
}