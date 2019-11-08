package com.yy.mobile.adb.transport

import java.io.IOException
import java.util.concurrent.TimeoutException

/**
 * @author YvesCheung
 * 2019/11/7
 */
interface AdbWriter {

    @Throws(TimeoutException::class, IOException::class)
    fun write(command: String)
}

