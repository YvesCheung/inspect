package com.yy.mobile.jdwp.transport

/**
 * @author YvesCheung
 * 2019/11/8
 */
interface JdwpWriter {

    fun writeString(str: String)

    fun writeInt(int: Int)

    fun writeByte(byte: Byte)

    fun writeByteArray(byteArray: ByteArray)
}