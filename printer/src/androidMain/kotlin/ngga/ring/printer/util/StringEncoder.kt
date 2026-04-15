package ngga.ring.printer.util

import java.nio.charset.Charset

actual fun encodeString(text: String, charsetName: String): ByteArray {
    return try {
        text.toByteArray(Charset.forName(charsetName))
    } catch (e: Exception) {
        text.encodeToByteArray() // Fallback to UTF-8
    }
}
