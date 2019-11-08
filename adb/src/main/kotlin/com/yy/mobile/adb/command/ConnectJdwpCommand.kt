package com.yy.mobile.adb.command

import com.yy.mobile.adb.transport.AdbReader
import com.yy.mobile.adb.transport.AdbWriter
import java.io.IOException
import java.lang.Exception
import java.nio.channels.SocketChannel

/**
 * 连接到指定的设备进程
 *
 * jdwp:<pid>
 * Connects to the JDWP thread running in the VM of process <pid>.
 *
 * @author: YvesCheung
 * 2019/11/7
 */
open class ConnectJdwpCommand(
    private val deviceSerialNumber: String,
    private val pid: Int,
    private val listener: ReadyListener
) : AdbCommand {

    interface ReadyListener {

        fun whenChannelReady(channel: SocketChannel)
    }

    private val selectTarget = SelectDeviceCommand(deviceSerialNumber)

    override fun execute(channel: SocketChannel, writer: AdbWriter, reader: AdbReader) {
        channel.configureBlocking(false)

        selectTarget.execute(channel, writer, reader)
        val result = selectTarget.getResult()
        if (!result.okay) {
            throw IOException("Can't select device $deviceSerialNumber: ${result.message}")
        }

        try {
            writer.write("jdwp:$pid")
            val response = reader.read(false)
            if (!response.okay) {
                throw IOException("Can't connect jdwp:$pid: ${response.message}")
            }

            listener.whenChannelReady(channel)
        } catch (e: Exception) {
            channel.close()
            throw e
        }
    }

    override fun needCloseSocket(): Boolean = false
}