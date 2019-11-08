package com.yy.mobile.adb.transport

import com.yy.mobile.adb.client.AdbResponse
import java.io.IOException
import java.util.concurrent.TimeoutException

/**
 * @author: YvesCheung
 * 2019/11/7
 */
interface AdbReader {

    @Throws(TimeoutException::class, IOException::class)
    fun read(detail: Boolean): AdbResponse

    @Throws(TimeoutException::class, IOException::class)
    fun readString(buffer: ByteArray): String
}