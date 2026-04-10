package ngga.ring.printer.util

import android.graphics.Bitmap
import android.graphics.Color
import ngga.ring.printer.util.escpos.ESCPosImageHelper

/**
 * Android-specific utility to convert Bitmaps to ESC/POS raster bytes.
 */
object BitmapToEscPos {

    /**
     * Converts a Bitmap to bitonal bytes using optionally Floyd-Steinberg dithering.
     * 
     * @param bitmap The source image.
     * @param dither Whether to use Floyd-Steinberg dithering for better gradients.
     * @param threshold Grayscale threshold (used if dither=false).
     * @return A Triple containing (packedBytes, width, height).
     */
    fun convert(bitmap: Bitmap, dither: Boolean = true, threshold: Int = 120): RasterResult {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Convert to grayscale first (double precision for dithering)
        val gray = DoubleArray(width * height) { i ->
            val color = pixels[i]
            (Color.red(color) * 0.299 + Color.green(color) * 0.587 + Color.blue(color) * 0.114)
        }

        val bitonal = BooleanArray(width * height)

        if (dither) {
            // Floyd-Steinberg Dithering
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val index = y * width + x
                    val oldPixel = gray[index]
                    val newPixel = if (oldPixel < 128) 0.0 else 255.0
                    bitonal[index] = newPixel == 0.0
                    
                    val error = oldPixel - newPixel
                    
                    // Distribute error to neighbors
                    if (x + 1 < width) gray[index + 1] += error * 7.0 / 16.0
                    if (y + 1 < height) {
                        if (x - 1 >= 0) gray[(y + 1) * width + x - 1] += error * 3.0 / 16.0
                        gray[(y + 1) * width + x] += error * 5.0 / 16.0
                        if (x + 1 < width) gray[(y + 1) * width + x + 1] += error * 1.0 / 16.0
                    }
                }
            }
        } else {
            // Simple thresholding
            for (i in gray.indices) {
                bitonal[i] = gray[i] < threshold
            }
        }

        return RasterResult(
            bytes = ESCPosImageHelper.packPixelsToRaster(bitonal, width, height),
            width = width,
            height = height
        )
    }

    data class RasterResult(
        val bytes: ByteArray,
        val width: Int,
        val height: Int
    )
}
