package com.yy.mobile.inspect

import com.yy.mobile.inspect.client.AndroidDebugBridge
import com.yy.mobile.inspect.command.SelectDeviceCommand
import com.yy.mobile.inspect.command.TrackDevicesCommand
import com.yy.mobile.inspect.command.SimpleCommand
import com.yy.mobile.inspect.command.TrackJdwpCommand
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch

/**
 * @author: YvesCheung
 * 2019/11/7
 */
class AndroidDebugBridgeTest {

    private val adb = AndroidDebugBridge()

    @Test
    fun testVersion() = testCommand("host:version")

    @Test
    fun testDevices() = testCommand("host:devices-l")

    private fun testCommand(command: String) {
        val wrapCommand = SimpleCommand(command)

        val result1 = adb.sendCommand(command)

        adb.sendCommand(wrapCommand)
        val result2 = wrapCommand.getResult()

        println(result1)

        Assertions.assertEquals(result1, result2)
    }

    @Test
    fun testDeviceTracker() {

        val lock = CountDownLatch(1)

        val tracker = TrackDevicesCommand(object : TrackDevicesCommand.UpdateListener {
            override fun connectionError(error: Exception) {
                println(error)
                lock.countDown()
            }

            override fun deviceListUpdate(deviceList: Map<String, TrackDevicesCommand.DeviceState>) {
                println(deviceList)

                if (deviceList.isEmpty()) {
                    lock.countDown()
                } else if (deviceList.any { it.value == TrackDevicesCommand.DeviceState.ONLINE }) {
                    lock.countDown()
                }
            }
        })
        adb.sendCommand(tracker)

        lock.await()
    }

    @Test
    fun testSetDevice() {
        val lock = CountDownLatch(1)

        val tracker = TrackDevicesCommand(object : TrackDevicesCommand.UpdateListener {
            override fun connectionError(error: Exception) {
                println(error)
                lock.countDown()
            }

            override fun deviceListUpdate(deviceList: Map<String, TrackDevicesCommand.DeviceState>) {
                println(deviceList)

                val device =
                    deviceList.entries.find { it.value == TrackDevicesCommand.DeviceState.ONLINE }
                if (device != null) {
                    val select = SelectDeviceCommand(device.key)
                    adb.sendCommand(select)
                    println(select.getResult())
                    lock.countDown()
                }
            }
        })
        adb.sendCommand(tracker)

        lock.await()
    }

    @Test
    fun testJdwpTracker() {
        val lock = CountDownLatch(1)

        val tracker = TrackJdwpCommand("cc764b31", object : TrackJdwpCommand.UpdateListener {
            override fun processListUpdate(pids: List<Int>) {
                println("pids = $pids")
                lock.countDown()
            }
        })
        adb.sendCommand(tracker)

        lock.await()
    }
}