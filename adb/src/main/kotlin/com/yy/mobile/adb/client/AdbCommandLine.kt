package com.yy.mobile.adb.client

import java.io.IOException
import java.lang.Exception
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author YvesCheung
 * 2019/11/8
 */
object AdbCommandLine {

    private val startAdb = AtomicBoolean(false)

    fun startServer() {
        if (startAdb.compareAndSet(false, true)) {
            try {
                execCommand("adb start-server")
            } catch (e: Exception) {
                startAdb.set(false)
                throw e
            }
        }
    }

    fun killServer() {
        execCommand("adb kill-server")
        startAdb.set(false)
    }

    @Throws(Exception::class)
    private fun execCommand(commandLine: String) {
        val proc = Runtime.getRuntime().exec(commandLine)
        val status = proc.waitFor()

        if (status != 0) {
            throw IOException("Can't exec command: $commandLine, return $status")
        }
    }
}