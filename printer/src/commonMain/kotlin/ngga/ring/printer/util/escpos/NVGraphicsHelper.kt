package ngga.ring.printer.util.escpos

/**
 * Helper for NV (Non-Volatile) Graphics operations.
 * Allows storing images permanently in the printer's flash memory
 * for faster repeated printing (e.g. company logos).
 *
 * ESC/POS Commands Used:
 * - FS q n [xL xH yL yH d1...dk]... — Define NV bit images
 * - FS p n m — Print NV bit image
 * - FS ( L — Define/print download graphics (newer printers)
 */
object NVGraphicsHelper {

    /**
     * Generates the byte sequence to define one or more NV bit images.
     * This writes the images to the printer's non-volatile memory.
     *
     * FS q n [xL xH yL yH d1...dk]1 ... [xL xH yL yH d1...dk]n
     *
     * @param images List of image data. Each item is a Triple of (grayscale IntArray, width, height).
     * @param threshold Binarization threshold (0-255).
     * @return ByteArray containing the full FS q command.
     */
    fun defineNVBitImages(
        images: List<Triple<IntArray, Int, Int>>,
        threshold: Int = 128
    ): ByteArray {
        val buffer = mutableListOf<Byte>()

        // FS q n
        buffer.add(0x1C.toByte()) // FS
        buffer.add(0x71.toByte()) // q
        buffer.add(images.size.coerceIn(1, 255).toByte()) // n

        for ((grayscale, width, height) in images) {
            // xL xH — width in bytes (width / 8)
            val widthBytes = (width + 7) / 8
            buffer.add((widthBytes % 256).toByte())
            buffer.add((widthBytes / 256).toByte())

            // yL yH — height in dots
            buffer.add((height % 256).toByte())
            buffer.add((height / 256).toByte())

            // Pack pixel data
            val packed = ESCPosImageHelper.packGrayscaleToRaster(grayscale, width, height, threshold)
            packed.forEach { buffer.add(it) }
        }

        return buffer.toByteArray()
    }

    /**
     * Generates the byte sequence to define a single NV bit image at a specific index.
     *
     * @param index Image index (1-based, typically 1-255).
     * @param grayscale Grayscale pixel values (0-255).
     * @param width Image width in pixels.
     * @param height Image height in pixels.
     * @param threshold Binarization threshold.
     * @return ByteArray containing the command to define a single NV image.
     */
    fun defineNVBitImage(
        index: Int,
        grayscale: IntArray,
        width: Int,
        height: Int,
        threshold: Int = 128
    ): ByteArray {
        return defineNVBitImages(listOf(Triple(grayscale, width, height)), threshold)
    }

    /**
     * Generates the byte sequence to print a previously stored NV bit image.
     *
     * FS p n m
     * @param index Image index (1-based).
     * @param mode Print mode:
     *   0 = Normal
     *   1 = Double-width
     *   2 = Double-height
     *   3 = Quadruple (double-width + double-height)
     */
    fun printNVBitImage(index: Int, mode: Int = 0): ByteArray {
        return byteArrayOf(
            0x1C.toByte(), // FS
            0x70.toByte(), // p
            index.coerceIn(1, 255).toByte(),
            mode.coerceIn(0, 3).toByte()
        )
    }

    /**
     * Generates the byte sequence to delete all NV bit images from printer memory.
     *
     * This is done by defining 0 images (FS q 0), which some printers interpret
     * as a clear command. For printers that don't support this, use the
     * manufacturer-specific clear command.
     */
    fun deleteAllNVBitImages(): ByteArray {
        return byteArrayOf(
            0x1C.toByte(), // FS
            0x71.toByte(), // q
            0x00.toByte()  // n = 0
        )
    }

    /**
     * Generates bytes for the newer "Download Graphics" approach (GS ( L).
     * This is supported by newer Epson-compatible printers.
     *
     * Steps:
     * 1. Define download data (GS ( L pL pH 30 43 30 ...)
     * 2. Print download data (GS ( L pL pH 30 45 ...)
     *
     * @param grayscale Grayscale pixel values.
     * @param width Image width.
     * @param height Image height.
     * @param keyCode1 Key code 1 (identifier, default 0x20).
     * @param keyCode2 Key code 2 (identifier, default 0x20).
     */
    fun defineDownloadGraphics(
        grayscale: IntArray,
        width: Int,
        height: Int,
        keyCode1: Int = 0x20,
        keyCode2: Int = 0x20,
        threshold: Int = 128
    ): ByteArray {
        val buffer = mutableListOf<Byte>()
        val widthBytes = (width + 7) / 8
        val packed = ESCPosImageHelper.packGrayscaleToRaster(grayscale, width, height, threshold)

        // Data length: fn(30) + kc1 + kc2 + b + xL + xH + yL + yH + data
        val dataLen = 10 + packed.size
        val pL = dataLen % 256
        val pH = dataLen / 256

        // GS ( L pL pH 30 43 30 kc1 kc2 b xL xH yL yH d1...dk
        buffer.add(0x1D.toByte()) // GS
        buffer.add(0x28.toByte()) // (
        buffer.add(0x4C.toByte()) // L
        buffer.add(pL.toByte())
        buffer.add(pH.toByte())
        buffer.add(0x30.toByte()) // fn = 48
        buffer.add(0x43.toByte()) // fn2 = 67 (Define raster format)
        buffer.add(0x30.toByte()) // tone = 48
        buffer.add(keyCode1.toByte())
        buffer.add(keyCode2.toByte())
        buffer.add(0x01.toByte()) // b = 1 (monochrome)

        // xL xH — width in dots
        buffer.add((width % 256).toByte())
        buffer.add((width / 256).toByte())

        // yL yH — height in dots
        buffer.add((height % 256).toByte())
        buffer.add((height / 256).toByte())

        packed.forEach { buffer.add(it) }

        return buffer.toByteArray()
    }

    /**
     * Generates the print command for download graphics.
     *
     * GS ( L pL pH 30 45 kc1 kc2
     */
    fun printDownloadGraphics(
        keyCode1: Int = 0x20,
        keyCode2: Int = 0x20
    ): ByteArray {
        return byteArrayOf(
            0x1D.toByte(), // GS
            0x28.toByte(), // (
            0x4C.toByte(), // L
            0x06.toByte(), // pL
            0x00.toByte(), // pH
            0x30.toByte(), // fn = 48
            0x45.toByte(), // fn2 = 69 (Print)
            keyCode1.toByte(),
            keyCode2.toByte(),
            0x01.toByte(), // x (scale x)
            0x01.toByte()  // y (scale y)
        )
    }
}
