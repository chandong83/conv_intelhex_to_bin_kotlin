import java.io.File

class Memory(val address: Int, val data: ByteArray)

var globalOffset = 0
val mem = ArrayList<Memory>()
var array = ByteArray(0)

val RECORD_TYPE_DATA                     = 0
val RECORD_TYPE_END_OF_FILE              = 1
val RECORD_TYPE_EXTENDED_SEGMENT_ADDRESS = 2 
val RECORD_TYPE_EXTENDED_LINEAR_ADDRESS  = 4

fun hex(bytes: ByteArray): String {
    var hexString = ""
    for (i in bytes.indices) {
        hexString += String.format("%02X", (bytes[i].toInt() and 0xFF).toInt())
    }
    return hexString
}

fun readComma(line: String): String {
    return line.substring(0,1)
}

fun readByteLen(line: String): Int {
    return line.substring(1,3).toInt(radix = 16)
}

fun readAddress(line: String): Int {
    return line.substring(3,7).toInt(radix = 16)
}

fun readRecordType(line: String): Int {
    return line.substring(7,9).toInt(radix = 16)
}

fun readData(line: String): ByteArray {
    val len = (line.length-(2+9))
    var dat = ByteArray(len/2)
    var idx=0

    for (i in 1..len step 2) {        
        dat[idx] = line.substring(i+8, i+8+2).toInt(radix = 16).toByte() 
        idx += 1
    }
    //println("dat: ${hex(dat)}")
    return dat
}

fun readChecksum(line: String): Int {
    return line.substring(line.length-2, line.length).toInt(radix = 16)
}

fun hasComma(x: Any) = when(x) {
    is String -> x.startsWith(":")
    else -> false
}

fun calculateChecksum(line: String): Int {
    var len = line.length-3
    var checksum = 0
    for (i in 1..len step 2) {
        val num = line.substring(i, i+2).toInt(radix = 16)
        checksum += num        
    }
    checksum = ((0xFF-(checksum and 0xFF))+1) and 0xFF    
    return checksum
}


fun parsingHex(recordType: Int, address: Int, dat: ByteArray) {
    when (recordType) {
        RECORD_TYPE_DATA -> {
            //println("DATA")
            mem.add(Memory(globalOffset+address, dat))
        }
        RECORD_TYPE_END_OF_FILE -> {
            println("END_OF_FILE")
            var sortedList = mem.sortedWith(compareBy({ it.address }))            
            var curAddress = sortedList[0].address
            println("curAddress : ${Integer.toHexString(curAddress)}")
            for (obj in sortedList) {
                val size = obj.data.size
                if(curAddress != obj.address) {
                    //pad
                    println(" - pad start ${Integer.toHexString(curAddress)}, size ${Integer.toHexString(obj.address-curAddress)}")                        
                    val padBuf = ByteArray(obj.address-curAddress, { 0xFF.toByte() } ) 
                    array += padBuf
                }
                array += obj.data
                curAddress = obj.address + size
                //println("${Integer.toHexString(obj.address)}, ${hex(obj.data)}")
            }
            println("done - binary total size : ${array.size}")
        }
        RECORD_TYPE_EXTENDED_SEGMENT_ADDRESS -> {
            globalOffset = ((((dat[0].toInt() and 0xFF) shl 8) or (dat[1].toInt() and 0xFF)) shl 4)
            println("EXTENDED_SEGMENT_ADDRESS : ${Integer.toHexString(globalOffset)}")
        }
        RECORD_TYPE_EXTENDED_LINEAR_ADDRESS -> {
            globalOffset = ((((dat[0].toInt() and 0xFF) shl 8) or (dat[1].toInt() and 0xFF)) shl 16)
            println("EXTENDED_LINEAR_ADDRESS : ${Integer.toHexString(globalOffset)}")
        }
    }
}

fun decodeLine(line: String) {
    var ln = line.trim()
    
    var byteLen      = readByteLen(ln)
    var address      = readAddress(ln)
    var recordType   = readRecordType(ln)
    var dat          = readData(ln)
    var checksum     = readChecksum(ln)
    var calcChecksum = calculateChecksum(ln)
    
    if(!hasComma(ln)) {
        println("error: the format is invalid.")
        return
    }
    if(calcChecksum != checksum) {
        println("error: the Checksum is mismatched.")
        return
    }
    
    if(byteLen != dat.size){        
        println("error: the length is mismatched.")
        return
    }
    parsingHex(recordType, address, dat)   
}


fun fileReadReadLines(file: String) {
    File(file).readLines().forEach {
        decodeLine(it)
    }    
}

fun fileWriteOutputStrteam(file: String, bytes: ByteArray){
    val stream = File(file).outputStream()
    stream.write(bytes)
    stream.close()
}

fun main(args: Array<String>) {  
    fileReadReadLines("../b.hex")
    fileWriteOutputStrteam("../b.bin", array)
}