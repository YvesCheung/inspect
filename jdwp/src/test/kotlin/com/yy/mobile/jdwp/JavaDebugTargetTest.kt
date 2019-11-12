package com.yy.mobile.jdwp

import com.yy.mobile.adb.client.AndroidDebugBridge
import com.yy.mobile.adb.command.ConnectJdwpCommand
import com.yy.mobile.adb.command.TrackDevicesCommand
import com.yy.mobile.adb.command.TrackJdwpCommand
import com.yy.mobile.jdwp.command.VMVersion
import com.yy.mobile.jdwp.transport.JavaDebugTarget
import org.junit.jupiter.api.Test
import java.nio.channels.SocketChannel
import java.util.concurrent.CountDownLatch

/**
 * @author YvesCheung
 * 2019/11/12
 */

class JavaDebugTargetTest {

    private val adb = AndroidDebugBridge()

    @Test
    fun testTarget() {

        val lock = CountDownLatch(1)

        findDevice { device ->
            findProcess(device) { pid ->
                connectProccess(device, pid) { socket ->

                    val debugTarget = JavaDebugTarget(socket)
                    debugTarget.startDebug()

                    Thread.sleep(2000L)

                    debugTarget.newSession(VMVersion()) {
                        println(this)

                        debugTarget.stopDebug()
                        socket.close()
                        lock.countDown()
                    }
                }
            }
        }

        lock.await()
    }

    private fun findDevice(ok: (serialNumber: String) -> Unit) {
        val findDevice = TrackDevicesCommand(object : TrackDevicesCommand.UpdateListener {

            private var currentDevice: String? = null

            override fun connectionError(error: Exception) {
                println(error)
            }

            override fun deviceListUpdate(deviceList: Map<String, TrackDevicesCommand.DeviceState>) {
                if (currentDevice != null) {
                    return
                }
                val onLineDevice =
                    deviceList.entries.find { it.value == TrackDevicesCommand.DeviceState.ONLINE }
                if (onLineDevice != null) {
                    currentDevice = onLineDevice.key
                    ok(onLineDevice.key)
                }
            }
        })

        adb.sendCommand(findDevice)
    }

    private fun findProcess(serialNumber: String, ok: (pid: Int) -> Unit) {
        val findProcess = TrackJdwpCommand(serialNumber, object : TrackJdwpCommand.UpdateListener {

            private var currentPid = 0

            override fun processListUpdate(pids: List<Int>) {
                if (currentPid > 0) {
                    return
                }
                val findOne = pids.firstOrNull()
                if (findOne != null) {
                    currentPid = findOne
                    ok(findOne)
                }
            }
        })

        adb.sendCommand(findProcess)
    }

    private fun connectProccess(serialNumber: String, pid: Int, ok: (SocketChannel) -> Unit) {
        val connect = ConnectJdwpCommand(
            serialNumber, pid,
            object : ConnectJdwpCommand.ReadyListener {
                override fun whenChannelReady(channel: SocketChannel) {
                    ok(channel)
                }
            })

        adb.sendCommand(connect)
    }
}