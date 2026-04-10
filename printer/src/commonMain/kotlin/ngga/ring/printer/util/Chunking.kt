package ngga.ring.printer.util

/**
 * Splits a byte array into smaller chunks for sequential writing to printer buffers.
 */
fun ByteArray.chunkedForWrite(size: Int): List<ByteArray> {
    if (size <= 0) return listOf(this)
    val chunks = mutableListOf<ByteArray>()
    var start = 0
    while (start < this.size) {
        val end = (start + size).coerceAtMost(this.size)
        chunks.add(this.copyOfRange(start, end))
        start = end
    }
    return chunks
}
