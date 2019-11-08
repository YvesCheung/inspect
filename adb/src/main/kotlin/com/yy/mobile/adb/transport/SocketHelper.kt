package com.yy.mobile.adb.transport

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.concurrent.TimeoutException

/**
 * @author: YvesCheung
 * 2019/11/7
 */
object SocketHelper {

    private const val DEFAULT_TIMEOUT = 5000

    @Throws(TimeoutException::class, IOException::class)
    fun write(
        chan: SocketChannel,
        data: ByteArray,
        length: Int = data.size,
        timeout: Int = DEFAULT_TIMEOUT
    ) {
        val buf = ByteBuffer.wrap(data, 0, if (length != -1) length else data.size)
        var numWaits = 0

        while (buf.position() != buf.limit()) {
            val count = chan.write(buf)
            if (count < 0) {
                throw IOException("channel EOF")
            }

            if (count == 0) {
                if (timeout != 0 && numWaits * 5 > timeout) {
                    throw TimeoutException()
                }

                try {
                    Thread.sleep(5L)
                } catch (var8: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw TimeoutException("Write interrupted with immediate timeout via interruption.")
                }

                ++numWaits
            } else {
                numWaits = 0
            }
        }
    }

    @Throws(TimeoutException::class, IOException::class)
    fun read(
        chan: SocketChannel,
        data: ByteArray,
        length: Int = data.size,
        timeout: Int = DEFAULT_TIMEOUT
    ) {
        val buf = ByteBuffer.wrap(data, 0, if (length != -1) length else data.size)
        var numWaits = 0

        while (buf.position() != buf.limit()) {
            val count = chan.read(buf)
            if (count < 0) {
                throw IOException("EOF")
            }

            if (count == 0) {
                if (timeout != 0 && numWaits * 5 > timeout) {
                    throw TimeoutException()
                }

                try {
                    Thread.sleep(5L)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw TimeoutException("Read interrupted with immediate timeout via interruption.")
                }

                ++numWaits
            } else {
                numWaits = 0
            }
        }
    }
}