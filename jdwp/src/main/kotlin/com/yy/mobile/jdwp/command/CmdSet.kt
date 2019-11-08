package com.yy.mobile.jdwp.command

/**
 * @author YvesCheung
 * 2019/11/8
 */
sealed class CmdSet(toByte: Byte)

sealed class CmdId(toByte: Byte)

//ReferenceType(2),
//ClassType(3),
//ArrayType(4),
//InterfaceType(5),
//Method(6),
//Field(8),
//ObjectReference(9),
//StringReference(10),
//ThreadReference(11),
//ThreadGroupReference(12),
//ArrayReference(13),
//ClassLoaderReference(14),
//EventRequest(15),
//StackFrame(16),
//ClassObjectReference(17),
//Event(64)


class VirtualMachine : CmdSet(1)

class Version : CmdId(1)

