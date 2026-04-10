package ngga.ring.printer.util.escpos

/**
 * Utility for processing images into ESC/POS compatible byte streams.
 * Focuses on KMP compatibility by avoiding platform-specific image classes.
 */
object ESCPosImageHelper {

    /**
     * Converts a bitonal (black/white) pixel array into ESC/POS raster bytes.
     * Pixels are packed 8 per byte, most significant bit first.
     * 
     * @param pixels Array of pixels where true = black, false = white.
     * @param width Width of the image in pixels.
     * @param height Height of the image in pixels.
     * @return ByteArray formatted for 'GS v 0' command.
     */
    fun packPixelsToRaster(pixels: BooleanArray, width: Int, height: Int): ByteArray {
        val widthBytes = (width + 7) / 8
        val output = ByteArray(widthBytes * height)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (pixels[y * width + x]) {
                    val byteIndex = y * widthBytes + (x / 8)
                    val bitIndex = 7 - (x % 8)
                    output[byteIndex] = (output[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                }
            }
        }
        return output
    }

    /**
     * Alternative version using ByteArray where values >= 128 (threshold) are black.
     */
    fun packPixelsToRaster(pixels: ByteArray, width: Int, height: Int, threshold: Int = 128): ByteArray {
        val widthBytes = (width + 7) / 8
        val output = ByteArray(widthBytes * height)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x].toInt() and 0xFF
                if (pixel < threshold) { // Assuming 0 is black in grayscale, but ESC/POS bit 1 is black
                    // Wait, usually in thermal printing 1 = Black, 0 = White.
                    // If we use threshold: pixel < threshold means it's "dark" enough to be black.
                    val byteIndex = y * widthBytes + (x / 8)
                    val bitIndex = 7 - (x % 8)
                    output[byteIndex] = (output[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                }
            }
        }
        return output
    }
}
