package ngga.ring.printer.util.platform

actual fun encodeString(text: String, charsetName: String): ByteArray {
    return try {
        text.toByteArray(charset(charsetName))
    } catch (e: Exception) {
        // Fallback to UTF-8 or platform default
        text.encodeToByteArray()
    }
}
