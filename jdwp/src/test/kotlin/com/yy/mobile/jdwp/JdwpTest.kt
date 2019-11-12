package com.yy.mobile.jdwp

import com.sun.jdi.Bootstrap
import com.sun.jdi.connect.spi.ClosedConnectionException
import com.sun.jdi.connect.spi.Connection
import com.yy.mobile.adb.client.AndroidDebugBridge
import com.yy.mobile.adb.command.ConnectJdwpCommand
import com.yy.mobile.jdwp.command.VMVersion
import com.yy.mobile.jdwp.transport.JdwpPacketTransmitter
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.CountDownLatch

/**
 * @author YvesCheung
 * 2019/11/8
 */
class JdwpTest {

    private val adb = AndroidDebugBridge()

    @Test
    fun testJdwpConnect() {
        val lock = CountDownLatch(1)

        val connect = ConnectJdwpCommand("cc764b31", 3272,
            object : ConnectJdwpCommand.ReadyListener {
                override fun whenChannelReady(channel: SocketChannel) {

                    channel.configureBlocking(true)

                    //ASCII for "JDWP-Handshake"
                    val handshake =
                        byteArrayOf(74, 68, 87, 80, 45, 72, 97, 110, 100, 115, 104, 97, 107, 101)

                    val tempBuffer = ByteBuffer.allocate(handshake.size)

                    try {
                        tempBuffer.put(handshake)
                        val expectedLen = tempBuffer.position()
                        tempBuffer.flip()
                        if (channel.write(tempBuffer) != expectedLen) {
                            println("partial handshake write")
                        } else {
                            val c = SocketChannelConnection(channel)

                            val byteArray = c.readPacket()
                            println("rsp = ${String(byteArray)}")

                            val vm = Bootstrap.virtualMachineManager()
                                .createVirtualMachine(c)
                            println("vm version = ${vm.version()}")
                        }
                    } catch (e: IOException) {
                        println("IO error during handshake: $e")
                    } finally {
                        channel.close()
                        lock.countDown()
                    }
                }
            })

        adb.sendCommand(connect)

        lock.await()
    }

    private class SocketChannelConnection(private val channel: SocketChannel) : Connection() {

        init {
            channel.configureBlocking(true)
        }

        private val readBuffer = ByteBuffer.allocate(10240)

        @Throws(IOException::class)
        override fun readPacket(): ByteArray {
            try {
                if (!this.isOpen) {
                    throw ClosedConnectionException("connection is closed")
                } else {
                    try {
                        var bo: ByteArrayOutputStream? = null
                        while (channel.read(readBuffer) != 0 && readBuffer.position() != 0) {
                            readBuffer.flip()

                            if (bo != null) {
                                if (readBuffer.limit() == readBuffer.capacity()) {
                                    bo.write(readBuffer.array())
                                } else {
                                    val byteArray = ByteArray(readBuffer.limit())
                                    readBuffer.get(byteArray)
                                    bo.write(byteArray)
                                    return bo.toByteArray()
                                }
                            } else {
                                val b1 = readBuffer[0].toInt() and 255
                                val b2 = readBuffer[1].toInt() and 255
                                val b3 = readBuffer[2].toInt() and 255
                                val b4 = readBuffer[3].toInt() and 255
                                if (b1 == 74 && b2 == 68 && b3 == 87 && b4 == 80) { //handshake
                                    return ByteArray(14).also { readBuffer.get(it) }
                                }
                                val length = (b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4
                                when {
                                    length <= 0 -> {
                                        throw IOException("protocol error - invalid length")
                                    }
                                    length > readBuffer.limit() -> {
                                        println("message length is $length")
                                        bo = ByteArrayOutputStream(length)
                                        bo.write(readBuffer.array())
                                    }
                                    else -> {
                                        return ByteArray(length).also { readBuffer.get(it) }
                                    }
                                }
                            }
                            readBuffer.clear()
                        }
                        return bo?.toByteArray() ?: ByteArray(0)
                    } catch (e: IOException) {
                        if (!isOpen) {
                            throw ClosedConnectionException("connection is closed")
                        }
                        throw e
                    }
                }
            } catch (e: IOException) {
                println(e)
            } finally {
                readBuffer.clear()
            }

            return ByteArray(0)
        }

        @Throws(IOException::class)
        override fun writePacket(bytes: ByteArray) {
            val packet = ByteBuffer.wrap(bytes)
            channel.write(packet)
        }

        @Throws(IOException::class)
        override fun close() {
            channel.close()
        }

        override fun isOpen(): Boolean {
            return channel.isOpen
        }
    }
}