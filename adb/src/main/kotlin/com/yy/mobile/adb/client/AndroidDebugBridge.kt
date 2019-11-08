package com.yy.mobile.adb.client

import com.yy.mobile.adb.command.AdbCommand
import com.yy.mobile.adb.transport.AdbReader
import com.yy.mobile.adb.transport.AdbWriter
import com.yy.mobile.adb.command.SimpleCommand
import com.yy.mobile.adb.transport.SocketHelper.read
import com.yy.mobile.adb.transport.SocketHelper.write
import java.io.IOException
import java.lang.Exception
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executor
import java.util.concurrent.TimeoutException

/**
 * 一种ADB的代码实现。
 *
 * ADB传输原理参考：https://github.com/aosp-mirror/platform_system_core/blob/master/adb/OVERVIEW.TXT
 *
 * @author: YvesCheung
 * 2019/11/7
 */
class AndroidDebugBridge {

    companion object {
        const val DEFAULT_ADB_PORT = 5037

        private val DEFAULT_CHARSET = StandardCharsets.UTF_8
    }

    private val hostAddress = InetAddress.getLoopbackAddress()

    private val socketAddress = InetSocketAddress(hostAddress, DEFAULT_ADB_PORT)

    private val currentThreadExecutor = Executor { task -> task?.run() }

    @Throws(Exception::class)
    fun sendCommand(command: String, readDetailMessage: Boolean = true): AdbResponse {
        val commandLine = SimpleCommand(command, readDetailMessage)
        sendCommand(commandLine)
        return commandLine.getResult()
    }

    @Throws(Exception::class)
    fun sendCommand(command: AdbCommand) {

        AdbCommandLine.startServer()

        val executor = command.executor() ?: currentThreadExecutor
        executor.execute {

            val runnable = { channel: SocketChannel ->
                channel.socket().tcpNoDelay = true
                command.execute(channel, WriterImpl(channel), ReaderImpl(channel))
            }

            val channel = SocketChannel.open(socketAddress)
            if (command.needCloseSocket()) {
                channel.use(runnable)
            } else {
                channel.let(runnable)
            }
        }
    }

    private class WriterImpl(val channel: SocketChannel) : AdbWriter {

        override fun write(command: String) = writeAdbRequest(channel, command)

        @Throws(TimeoutException::class, IOException::class)
        private fun writeAdbRequest(channel: SocketChannel, req: String) {
            val resultStr = String.format("%04X%s", req.length, req)
            val byteReq = resultStr.toByteArray(DEFAULT_CHARSET)
            write(channel, byteReq)
        }
    }

    private class ReaderImpl(val channel: SocketChannel) : AdbReader {

        override fun read(detail: Boolean): AdbResponse = readAdbResponse(channel, detail)

        override fun readString(buffer: ByteArray): String {
            read(channel, buffer)
            return String(buffer, DEFAULT_CHARSET)
        }

        @Throws(TimeoutException::class, IOException::class)
        private fun readAdbResponse(chan: SocketChannel, readDetailMessage: Boolean): AdbResponse {

            fun isOkay(reply: ByteArray): Boolean =
                reply[0].toInt() == 79 &&
                        reply[1].toInt() == 75 &&
                        reply[2].toInt() == 65 &&
                        reply[3].toInt() == 89

            val reply = ByteArray(4)
            read(chan, reply)

            val isOk = isOkay(reply)

            if (readDetailMessage || !isOk) {
                try {
                    val lenBuf = ByteArray(4)
                    val lenStr = readString(lenBuf)

                    val len = Integer.parseInt(lenStr, 16)
                    val msg = ByteArray(len)
                    val message = readString(msg)

                    return AdbResponse(isOk, message)
                } catch (ignore: Exception) {
                }
            }

            return AdbResponse(isOk)
        }
    }
}