package ngga.ring.printer.util

import platform.Foundation.*
import kotlinx.cinterop.*
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual fun encodeString(text: String, charsetName: String): ByteArray {
    // Standard NSString encodings
    val encoding = when (charsetName.uppercase()) {
        "UTF-8" -> NSUTF8StringEncoding
        "US-ASCII" -> NSASCIIStringEncoding
        "ISO-8859-1" -> NSISOLatin1StringEncoding
        "SHIFT-JIS" -> NSShiftJISStringEncoding
        "GBK", "GB18030" -> 0x80000632UL // kCFStringEncodingGB_18030_2000 mapping
        else -> NSUTF8StringEncoding
    }

    val nsString = text as NSString
    val data = nsString.dataUsingEncoding(encoding) ?: return text.encodeToByteArray()
    
    val length = data.length.toInt()
    val bytes = ByteArray(length)
    if (length > 0) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
    }
    return bytes
}
