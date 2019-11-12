package com.yy.mobile.jdwp.transport

import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.ClosedChannelException
import java.nio.channels.SocketChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author YvesCheung
 * 2019/11/12
 */
@Suppress("MemberVisibilityCanBePrivate")
open class JavaDebugTarget(private val channel: SocketChannel) {

    @Volatile
    private var monitorServer: ExecutorService? = null

    @Volatile
    private var replyServer: ExecutorService? = null

    protected open fun newMonitorExecutor(): ExecutorService =
        Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "JavaDebugMonitorThread") }

    protected open fun newReplyExecutor(): ExecutorService =
        Executors.newFixedThreadPool(2, object : ThreadFactory {
            private val poolNumber = AtomicInteger(1)

            override fun newThread(runnable: Runnable) =
                Thread(runnable, "JavaDebugReply${poolNumber.getAndIncrement()}Thread")
        })

    //guard by "this"
    protected var debugging = false

    @Synchronized
    open fun startDebug() {
        if (debugging) {
            return
        }

        monitorServer = newMonitorExecutor().also { it.execute { run() } }
        replyServer = newReplyExecutor()
        debugging = true

        JdwpHandShake.sendHandshake(channel)
    }

    @Synchronized
    open fun stopDebug() {
        if (debugging) {
            monitorServer?.shutdown()
            monitorServer = null
            replyServer?.shutdown()
            replyServer = null
            debugging = false
        }
    }

    protected open fun run() {
        channel.configureBlocking(true)
        channel.socket().tcpNoDelay = true
        val readBuffer = ByteBuffer.allocate(2048)

        var bos = ByteArrayOutputStream()

        try {
            while (channel.read(readBuffer) != 0) {
                try {
                    readBuffer.flip()
                    val byteArray = ByteArray(readBuffer.limit())
                    readBuffer.get(byteArray)
                    bos.write(byteArray)

                    if (readBuffer.limit() < readBuffer.capacity()) {
                        val bytes = bos.toByteArray()
                        bos = ByteArrayOutputStream()
                        replyServer?.execute {
                            handle(bytes)
                        }
                    }
                } catch (e: Exception) {
                    if (!debugging) {
                        break
                    }
                } finally {
                    readBuffer.clear()
                }
            }
        } catch (e: ClosedChannelException) {
            stopDebug()
        }
    }

    protected open fun handle(bytes: ByteArray) {
        if (JdwpHandShake.consumeHandshake(bytes)) {
            return
        }

        var session: JdwpSession? = null

        val packet = JdwpPacketTransmitter.fromByteArray(bytes) { id ->

            synchronized(sessionHolder) {
                session = sessionHolder.remove(id)
            }

            session?.packet
        }

        val replySession = session
        if (replySession != null && packet != null) {
            replySession.reply(packet)
        }
    }

    protected val sessionHolder: MutableMap<Int, JdwpSession> = HashMap()

    open fun newSession(command: JdwpPacket, reply: JdwpPacket.() -> Unit) {
        val id = command.id

        synchronized(sessionHolder) {
            if (sessionHolder.containsKey(id)) {
                throw RuntimeException("Already has a session with id $id")
            }

            sessionHolder[id] = JdwpSession(command, reply)
        }

        val bytes = JdwpPacketTransmitter.toByteArray(command)
        channel.write(ByteBuffer.wrap(bytes))
    }

    protected data class JdwpSession(val packet: JdwpPacket, val reply: JdwpPacket.() -> Unit)
}