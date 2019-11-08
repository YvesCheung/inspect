package com.yy.mobile.adb.command

import com.yy.mobile.adb.client.AdbResponse
import com.yy.mobile.adb.transport.AdbReader
import com.yy.mobile.adb.transport.AdbWriter
import java.lang.RuntimeException
import java.nio.channels.SocketChannel

/**
 * 直接执行adb命令,
 * 支持的命令参考：https://github.com/aosp-mirror/platform_system_core/blob/master/adb/SERVICES.TXT
 *
 * @param command adb命令
 * @param detailMessage 是否读取详细信息。决定[AdbResponse.message]是否有值。
 *
 * @author: YvesCheung
 * 2019/11/7
 */
open class SimpleCommand(
    private val command: String,
    private val detailMessage: Boolean = true
) : AdbCommand {

    private var result: AdbResponse? = null

    override fun execute(channel: SocketChannel, writer: AdbWriter, reader: AdbReader) {
        channel.configureBlocking(false)
        writer.write(command)
        result = reader.read(detailMessage)
    }

    open fun getResult(): AdbResponse {
        return result ?: throw RuntimeException("Can't getResult before execution.")
    }
}