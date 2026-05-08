package ngga.ring.printer.util.escpos

/**
 * Utility for processing images into ESC/POS compatible byte streams.
 * Focuses on KMP compatibility by avoiding platform-specific image classes.
 */
object ESCPosImageHelper {

    /**
     * Converts a bitonal (black/white) pixel array into ESC/POS raster bytes.
     * Pixels are packed 8 per byte, most significant bit first.
     */
    fun packPixelsToRaster(pixels: BooleanArray, width: Int, height: Int): ByteArray {
        val widthBytes = (width + 7) / 8
        val output = ByteArray(widthBytes * height)
        
        for (y in 0 until height) {
            val yOffset = y * width
            val outputOffset = y * widthBytes
            for (x in 0 until width) {
                if (pixels[yOffset + x]) {
                    val byteIndex = outputOffset + (x shr 3)
                    val bitIndex = 7 - (x and 0x07)
                    output[byteIndex] = (output[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                }
            }
        }
        return output
    }

    /**
     * Highly optimized pack from bitonal ShortArray/IntArray to ByteArray.
     */
    fun packGrayscaleToRaster(pixels: IntArray, width: Int, height: Int, threshold: Int = 128): ByteArray {
        val widthBytes = (width + 7) / 8
        val output = ByteArray(widthBytes * height)
        
        for (y in 0 until height) {
            val yOffset = y * width
            val outputOffset = y * widthBytes
            for (x in 0 until width) {
                if (pixels[yOffset + x] < threshold) { // Black
                    val byteIndex = outputOffset + (x shr 3)
                    val bitIndex = 7 - (x and 0x07)
                    output[byteIndex] = (output[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                }
            }
        }
        return output
    }

    /**
     * Adjusts contrast and brightness of a grayscale image.
     * @param grayscale Intensity values (0-255).
     * @param contrast -100 to 100.
     * @param brightness -100 to 100.
     */
    fun adjustLevels(grayscale: IntArray, contrast: Int, brightness: Int): IntArray {
        val factor = (259.0 * (contrast + 255.0)) / (255.0 * (259.0 - contrast))
        return IntArray(grayscale.size) { i ->
            val p = grayscale[i]
            val adjusted = (factor * (p + brightness - 128.0) + 128.0).toInt()
            adjusted.coerceIn(0, 255)
        }
    }

    /**
     * Applies Floyd-Steinberg dithering to convert grayscale to bitonal.
     * Uses fixed-point integer arithmetic for high performance.
     */
    fun applyFloydSteinberg(grayscale: IntArray, width: Int, height: Int): BooleanArray {
        // Scaled by 16 for fixed-point math
        val pixels = IntArray(grayscale.size) { grayscale[it] shl 4 }
        val result = BooleanArray(width * height)

        for (y in 0 until height) {
            val yOffset = y * width
            for (x in 0 until width) {
                val index = yOffset + x
                val oldPixel = pixels[index]
                val newPixel = if (oldPixel < 2048) 0 else 4080 // 128 << 4 = 2048, 255 << 4 = 4080
                
                result[index] = newPixel == 0
                val error = oldPixel - newPixel

                // Distribute error: 7, 3, 5, 1
                if (x + 1 < width) pixels[index + 1] += (error * 7) shr 4
                if (y + 1 < height) {
                    val nextRow = index + width
                    if (x > 0) pixels[nextRow - 1] += (error * 3) shr 4
                    pixels[nextRow] += (error * 5) shr 4
                    if (x + 1 < width) pixels[nextRow + 1] += (error * 1) shr 4
                }
            }
        }
        return result
    }

    /**
     * Applies Atkinson dithering using fixed-point integer arithmetic.
     */
    fun applyAtkinson(grayscale: IntArray, width: Int, height: Int): BooleanArray {
        // Scaled by 8 for fixed-point math
        val pixels = IntArray(grayscale.size) { grayscale[it] shl 3 }
        val result = BooleanArray(width * height)

        for (y in 0 until height) {
            val yOffset = y * width
            for (x in 0 until width) {
                val index = yOffset + x
                val oldPixel = pixels[index]
                val newPixel = if (oldPixel < 1024) 0 else 2040 // 128 << 3 = 1024
                
                result[index] = newPixel == 0
                val error = (oldPixel - newPixel) shr 3 // 1/8th of error

                if (x + 1 < width) pixels[index + 1] += error
                if (x + 2 < width) pixels[index + 2] += error
                if (y + 1 < height) {
                    val nextRow = index + width
                    if (x > 0) pixels[nextRow - 1] += error
                    pixels[nextRow] += error
                    if (x + 1 < width) pixels[nextRow + 1] += error
                }
                if (y + 2 < height) {
                    pixels[index + (width shl 1)] += error
                }
            }
        }
        return result
    }

    /**
     * Rotates a BooleanArray (bitonal image) by 90, 180, or 270 degrees.
     */
    fun rotate(pixels: BooleanArray, width: Int, height: Int, degrees: Int): Triple<BooleanArray, Int, Int> {
        val normalized = (degrees % 360 + 360) % 360
        if (normalized == 0) return Triple(pixels, width, height)

        val newWidth = if (normalized == 90 || normalized == 270) height else width
        val newHeight = if (normalized == 90 || normalized == 270) width else height
        val result = BooleanArray(pixels.size)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val oldIdx = y * width + x
                val newX: Int
                val newY: Int

                when (normalized) {
                    90 -> {
                        newX = height - 1 - y
                        newY = x
                    }
                    180 -> {
                        newX = width - 1 - x
                        newY = height - 1 - y
                    }
                    270 -> {
                        newX = y
                        newY = width - 1 - x
                    }
                    else -> {
                        newX = x
                        newY = y
                    }
                }
                result[newY * newWidth + newX] = pixels[oldIdx]
            }
        }
        return Triple(result, newWidth, newHeight)
    }

    /**
     * Alternative version using simple threshold.
     */
    fun packPixelsToRaster(pixels: ByteArray, width: Int, height: Int, threshold: Int = 128): ByteArray {
        val bitonal = BooleanArray(pixels.size) { (pixels[it].toInt() and 0xFF) < threshold }
        return packPixelsToRaster(bitonal, width, height)
    }
}
