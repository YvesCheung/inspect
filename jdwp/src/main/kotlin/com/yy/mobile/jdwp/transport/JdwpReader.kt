package com.yy.mobile.jdwp.transport

/**
 * @author YvesCheung
 * 2019/11/8
 */
interface JdwpReader {

    fun readInt(): Int

    fun readByte(): Byte

    fun readShort(): Short
}