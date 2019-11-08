package com.yy.mobile.inspect.command

import com.yy.mobile.inspect.transport.AdbReader
import com.yy.mobile.inspect.transport.AdbWriter
import java.io.IOException
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
    private val pid: Int
) : AdbCommand {

    private val selectTarget = SelectDeviceCommand(deviceSerialNumber)

    override fun execute(channel: SocketChannel, writer: AdbWriter, reader: AdbReader) {
        channel.configureBlocking(false)

        selectTarget.execute(channel, writer, reader)
        val result = selectTarget.getResult()
        if (!result.okay) {
            throw IOException("Can't select device $deviceSerialNumber: ${result.message}")
        }

        writer.write("jdwp:$pid")
    }
}