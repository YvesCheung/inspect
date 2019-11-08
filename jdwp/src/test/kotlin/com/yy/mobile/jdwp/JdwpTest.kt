package com.yy.mobile.jdwp

import com.sun.jdi.Bootstrap
import com.sun.jdi.connect.spi.ClosedConnectionException
import com.sun.jdi.connect.spi.Connection
import com.yy.mobile.adb.client.AndroidDebugBridge
import com.yy.mobile.adb.command.ConnectJdwpCommand
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
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

        val connect = ConnectJdwpCommand("Y2J7N17729001308", 22239,
            object : ConnectJdwpCommand.ReadyListener {
                override fun whenChannelReady(channel: SocketChannel) {

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
                            channel.read(tempBuffer)

                            println("finish handshake, rsp = " + String(tempBuffer.array()))


                            val vm = Bootstrap.virtualMachineManager()
                                .createVirtualMachine(SocketChannelConnection(channel))
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

        private val receiveLock = Any()

        init {
            channel.configureBlocking(true)
        }

        @Throws(IOException::class)
        override fun readPacket(): ByteArray {
            if (!this.isOpen) {
                throw ClosedConnectionException("connection is closed")
            } else {
                synchronized(receiveLock) {
                    val lenBuffer = ByteBuffer.allocate(4)

                    //第一个4字节是长度
                    try {
                        channel.read(lenBuffer)
                    } catch (e: IOException) {
                        if (!isOpen) {
                            throw ClosedConnectionException("connection is closed")
                        }
                        throw e
                    }

                    lenBuffer.flip()
                    val b1 = lenBuffer[0].toInt()
                    val b2 = lenBuffer[1].toInt()
                    val b3 = lenBuffer[2].toInt()
                    val b4 = lenBuffer[3].toInt()
                    if (b1 == 74 && b2 == 68 && b3 == 87 && b4 == 80) { //handshake
                        val other = ByteBuffer.allocate(10)
                        channel.read(other)
                        return ByteBuffer.allocate(14).put(lenBuffer).put(other).array()
                    }
                    val length = (b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4
                    if (length <= 0) {
                        throw IOException("protocol error - invalid length")
                    } else {
                        val contentBuffer = ByteBuffer.allocate(length - 4)
                        channel.read(contentBuffer)

                        val packetBuffer = ByteBuffer.allocate(length)
                        packetBuffer.put(lenBuffer)
                        packetBuffer.put(contentBuffer)

                        return packetBuffer.array()
                    }
                }
            }
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