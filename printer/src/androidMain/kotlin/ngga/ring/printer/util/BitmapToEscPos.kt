package ngga.ring.printer.util

import android.graphics.Bitmap
import android.graphics.Color
import ngga.ring.printer.util.escpos.ESCPosImageHelper

/**
 * Android-specific utility to convert Bitmaps to ESC/POS raster bytes.
 */
object BitmapToEscPos {

    /**
     * Converts a Bitmap to bitonal bytes.
     * 
     * @param bitmap The source image.
     * @param threshold Grayscale threshold (0-255). Below this is black.
     * @return A Triple containing (packedBytes, width, height).
     */
    fun convert(bitmap: Bitmap, threshold: Int = 120): RasterResult {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val bitonal = BooleanArray(width * height)
        for (i in pixels.indices) {
            val color = pixels[i]
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
            bitonal[i] = gray < threshold 
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
