package ngga.ring.printer.util.platform

/**
 * JS Implementation of Platform Encoder.
 */
actual fun encodeString(text: String, charsetName: String): ByteArray {
    // Standard JS TextEncoder only supports UTF-8.
    // For ISO-8859-1 or ASCII, we do a bit-masking fallback.
    return try {
        if (charsetName.uppercase() == "ISO-8859-1" || charsetName.uppercase() == "US-ASCII") {
            ByteArray(text.length) { text[it].code.toByte() }
        } else {
            text.encodeToByteArray()
        }
    } catch (e: Exception) {
        text.encodeToByteArray()
    }
}
