package com.yy.mobile.adb.command

import com.yy.mobile.adb.transport.AdbReader
import com.yy.mobile.adb.transport.AdbWriter
import java.io.IOException
import java.nio.channels.SocketChannel
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * 对指定手机进行监听jdwp进程，获取可以被debug的进程号
 *
 *
 * track-jdwp
 *
 * This is used to send the list of JDWP pids periodically to the client.
 * The format of the returned data is the following:
 * <hex4>:    the length of all content as a 4-char hexadecimal string
 * <content>: a series of ASCII lines of the following format:<pid> "\n"
 *
 * This service is used by DDMS to know which debuggable processes are running
 * on the device/emulator.
 *
 * Note that there is no single-shot service to retrieve the list only once.
 *
 * @author YvesCheung
 * 2019/11/7
 */
open class TrackJdwpCommand(
    private val deviceSerialNumber: String,
    private val listener: UpdateListener
) : AdbCommand {

    interface UpdateListener {

        fun processListUpdate(pids: List<Int>)
    }

    private var quit = false

    private var connect: SocketChannel? = null

    private val selectTarget = SelectDeviceCommand(deviceSerialNumber)

    override fun execute(channel: SocketChannel, writer: AdbWriter, reader: AdbReader) {

        connect = channel

        val lenBuffer = ByteArray(4)

        selectTarget.execute(channel, writer, reader)
        val selectResult = selectTarget.getResult()
        if (!selectResult.okay) {
            throw IOException("Can't transport to $deviceSerialNumber: ${selectResult.message}")
        }

        writer.write("track-jdwp")
        val rsp = reader.read(false)
        if (rsp.okay) {
            while (!quit) {
                try {
                    val len = reader.readString(lenBuffer).toInt(16)
                    if (len > 0) {
                        val result = reader.readString(ByteArray(len))
                        val pids = result.split("\n").mapNotNull { it.toIntOrNull() }
                        listener.processListUpdate(pids)
                    } else {
                        listener.processListUpdate(listOf())
                    }
                } catch (e: IOException) {
                }
            }
        } else {
            throw IOException("Can't track-jdwp to device $deviceSerialNumber: ${rsp.message}")
        }
    }

    open fun stop() {
        quit = true

        try {
            connect?.close()
            connect = null
        } catch (ignore: IOException) {
        }
    }

    override fun executor(): Executor =
        Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "TrackJdwpThread") }
}