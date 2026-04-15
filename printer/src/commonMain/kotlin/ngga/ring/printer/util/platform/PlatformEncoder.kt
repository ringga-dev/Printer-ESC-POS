package ngga.ring.printer.util.platform

/**
 * Universal encoder for KMP.
 * Bypasses platform-specific charset limitations.
 */
expect fun encodeString(text: String, charsetName: String): ByteArray
