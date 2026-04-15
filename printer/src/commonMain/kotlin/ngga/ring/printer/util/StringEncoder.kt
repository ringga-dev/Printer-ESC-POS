package ngga.ring.printer.util

/**
 * Encodes a string into a byte array using the specified charset.
 * Platform-specific implementations handle the actual decoding logic.
 */
expect fun encodeString(text: String, charsetName: String): ByteArray
