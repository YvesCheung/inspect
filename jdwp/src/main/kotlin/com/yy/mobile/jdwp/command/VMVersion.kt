package com.yy.mobile.jdwp.command

import com.yy.mobile.jdwp.transport.JdwpPacket
import com.yy.mobile.jdwp.transport.JdwpReader
import com.yy.mobile.jdwp.transport.JdwpWriter

/**
 * @author YvesCheung
 * 2019/11/8
 */
class VMVersion : JdwpPacket(1, 1) {

    override fun to(writer: JdwpWriter) {
        //Nothing to write
    }

    override fun from(errorCode: Short, reader: JdwpReader) {
        println("errorCode = $errorCode")
    }
}