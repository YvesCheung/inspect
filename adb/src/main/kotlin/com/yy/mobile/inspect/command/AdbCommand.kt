package com.yy.mobile.inspect.command

import com.yy.mobile.inspect.transport.AdbReader
import com.yy.mobile.inspect.transport.AdbWriter
import java.nio.channels.SocketChannel
import java.util.concurrent.Executor

/**
 * 与adb server通讯的命令
 *
 * @author: YvesCheung
 * 2019/11/7
 */
interface AdbCommand {

    fun execute(channel: SocketChannel, writer: AdbWriter, reader: AdbReader)

    fun executor(): Executor? = null
}