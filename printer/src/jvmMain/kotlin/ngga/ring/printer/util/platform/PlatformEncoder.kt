package ngga.ring.printer.util.platform

import java.nio.charset.Charset

/**
 * JVM Implementation of Platform Encoder.
 */
actual fun encodeString(text: String, charsetName: String): ByteArray {
    return try {
        text.toByteArray(Charset.forName(charsetName))
    } catch (e: Exception) {
        // Fallback to ISO-8859-1 if charset not found
        text.toByteArray(Charset.forName("ISO-8859-1"))
    }
}
