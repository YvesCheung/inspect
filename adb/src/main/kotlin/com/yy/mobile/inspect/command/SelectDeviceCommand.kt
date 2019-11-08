package com.yy.mobile.inspect.command

/**
 * 用于选定adb通讯的设备，相当于 adb -s <serial-number> <other-command>
 *
 * host:transport:<serial-number>
 *
 * Ask to switch the connection to the device/emulator identified by
 * <serial-number>. After the OKAY response, every client request will
 * be sent directly to the adbd daemon running on the device.
 * (Used to implement the -s option)
 *
 * @param deviceSerialNumber the device/emulator identified
 *
 * @author: YvesCheung
 * 2019/11/7
 */
class SelectDeviceCommand(deviceSerialNumber: String) :
    SimpleCommand("host:transport:$deviceSerialNumber", false)