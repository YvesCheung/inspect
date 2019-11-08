package com.yy.mobile.inspect.command

import com.sun.org.apache.xpath.internal.operations.Bool
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

    /**
     * 执行命令的具体实现
     *
     * @param channel 与 adb server 通讯的通道
     * @param writer 往 adb server 发消息
     * @param reader 接收 adb server 的消息
     */
    fun execute(channel: SocketChannel, writer: AdbWriter, reader: AdbReader)

    /**
     * 命令执行的线程调度器
     *
     * @return 命令执行所在的 [Executor]，null 表示在当前线程执行
     */
    fun executor(): Executor? = null

    /**
     * 执行完命令或者执行抛出异常后，是否关闭通讯通道
     *
     * 对于短暂通讯的命令需要用完就关闭，比如 adb devices/ adb version
     * 对于要保持长连接通讯的不需要关闭，比如 adb forward
     *
     * @return 是否关闭通讯通道
     */
    fun needCloseSocket(): Boolean = true
}