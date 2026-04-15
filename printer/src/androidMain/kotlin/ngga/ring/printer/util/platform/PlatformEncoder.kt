package ngga.ring.printer.util.platform

actual fun encodeString(text: String, charsetName: String): ByteArray {
    return try {
        text.toByteArray(java.nio.charset.Charset.forName(charsetName))
    } catch (e: Exception) {
        text.encodeToByteArray() // Fallback to UTF-8
    }
}
