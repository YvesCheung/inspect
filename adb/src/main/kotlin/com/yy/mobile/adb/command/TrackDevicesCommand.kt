package com.yy.mobile.adb.command

import com.yy.mobile.adb.transport.AdbReader
import com.yy.mobile.adb.transport.AdbWriter
import java.io.IOException
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.SocketChannel
import java.util.concurrent.*

/**
 * 监听设备的状态变化
 *
 * host:track-devices
 *
 * This is a variant of host:devices which doesn't close the
 * connection. Instead, a new device list description is sent
 * each time a device is added/removed or the state of a given
 * device changes (hex4 + content). This allows tools like DDMS
 * to track the state of connected devices in real-time without
 * polling the server repeatedly.
 *
 * @author: YvesCheung
 * 2019/11/7
 */
@Suppress("MemberVisibilityCanBePrivate")
open class TrackDevicesCommand(private val listener: UpdateListener) : AdbCommand {

    interface UpdateListener {

        fun connectionError(error: Exception)

        fun deviceListUpdate(deviceList: Map<String, DeviceState>)
    }

    enum class DeviceState constructor(val state: String) {
        BOOTLOADER("bootloader"),
        OFFLINE("offline"),
        ONLINE("device"),
        RECOVERY("recovery"),
        SIDELOAD("sideload"),
        UNAUTHORIZED("unauthorized"),
        DISCONNECTED("disconnected");

        companion object {
            fun getState(state: String): DeviceState =
                values().find { it.state == state } ?: DISCONNECTED
        }
    }

    var monitoring = false
        private set

    var initialDeviceListDone = false
        private set

    private var connect: SocketChannel? = null

    private var quit = false

    override fun execute(channel: SocketChannel, writer: AdbWriter, reader: AdbReader) {

        channel.configureBlocking(true)

        connect = channel

        val lenBuffer = ByteArray(4)

        while (!quit) {
            try {
                if (!monitoring) {
                    writer.write("host:track-devices")
                    monitoring = reader.read(false).okay
                }

                if (monitoring) {
                    val length = reader.readString(lenBuffer).toInt(16)
                    if (length >= 0) {
                        processIncomingDeviceData(reader, length)
                        initialDeviceListDone = true
                    }
                }
            } catch (var2: AsynchronousCloseException) {
            } catch (var3: TimeoutException) {
                handleExceptionInMonitorLoop(var3)
            } catch (var4: IOException) {
                handleExceptionInMonitorLoop(var4)
            }
        }
    }

    @Throws(IOException::class)
    private fun processIncomingDeviceData(reader: AdbReader, length: Int) {
        val result: Map<String, DeviceState> =
            if (length <= 0) {
                mapOf()
            } else {
                val response = reader.readString(ByteArray(length))
                parseDeviceListResponse(response)
            }

        listener.deviceListUpdate(result)
    }

    private fun parseDeviceListResponse(result: String): Map<String, DeviceState> {
        val deviceStateMap = mutableMapOf<String, DeviceState>()
        val devices = result.split("\n")
        val size = devices.size

        for (idx in 0 until size) {
            val d = devices[idx]
            val param = d.split("\t")
            if (param.size == 2) {
                deviceStateMap[param[0]] = DeviceState.getState(param[1])
            }
        }

        return deviceStateMap
    }

    private fun handleExceptionInMonitorLoop(e: Exception) {
        if (!quit) {
            monitoring = false

            try {
                connect?.close()
                connect = null
            } catch (ignore: IOException) {
            }

            listener.connectionError(e)
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
        Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "DeviceTrackerThread") }
}