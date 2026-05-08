package ngga.ring.printer.util.escpos

/**
 * Pure Kotlin image scaler for ESC/POS thermal printing.
 * Works across all KMP platforms without platform-specific dependencies.
 */
object ImageScaler {

    /**
     * Scales a grayscale image to fit within a target width while maintaining aspect ratio.
     * Returns the scaled grayscale IntArray and the new dimensions.
     *
     * @param grayscale Source grayscale pixels (0-255).
     * @param srcWidth Source image width.
     * @param srcHeight Source image height.
     * @param targetWidth Target width in dots (e.g. 384 for 58mm, 576 for 80mm).
     * @param algorithm Scaling algorithm: "NEAREST" or "BILINEAR".
     * @return Triple of (scaledPixels, newWidth, newHeight).
     */
    fun scaleToFit(
        grayscale: IntArray,
        srcWidth: Int,
        srcHeight: Int,
        targetWidth: Int,
        algorithm: String = "BILINEAR"
    ): Triple<IntArray, Int, Int> {
        if (srcWidth <= 0 || srcHeight <= 0 || targetWidth <= 0) {
            return Triple(grayscale, srcWidth, srcHeight)
        }
        if (srcWidth == targetWidth) {
            return Triple(grayscale, srcWidth, srcHeight)
        }

        val scale = targetWidth.toDouble() / srcWidth
        val newWidth = targetWidth
        val newHeight = (srcHeight * scale).toInt().coerceAtLeast(1)

        return when (algorithm.uppercase()) {
            "NEAREST" -> Triple(scaleNearestNeighbor(grayscale, srcWidth, srcHeight, newWidth, newHeight), newWidth, newHeight)
            else -> Triple(scaleBilinear(grayscale, srcWidth, srcHeight, newWidth, newHeight), newWidth, newHeight)
        }
    }

    /**
     * Forces the image to a specific width and height.
     */
    fun scaleExact(
        grayscale: IntArray,
        srcWidth: Int,
        srcHeight: Int,
        dstWidth: Int,
        dstHeight: Int,
        algorithm: String = "BILINEAR"
    ): IntArray {
        if (srcWidth == dstWidth && srcHeight == dstHeight) return grayscale
        return when (algorithm.uppercase()) {
            "NEAREST" -> scaleNearestNeighbor(grayscale, srcWidth, srcHeight, dstWidth, dstHeight)
            else -> scaleBilinear(grayscale, srcWidth, srcHeight, dstWidth, dstHeight)
        }
    }

    /**
     * Nearest-neighbor scaling — fast, good for simple logos and text.
     */
    private fun scaleNearestNeighbor(
        src: IntArray,
        srcW: Int, srcH: Int,
        dstW: Int, dstH: Int
    ): IntArray {
        val dst = IntArray(dstW * dstH)
        val xRatio = srcW.toDouble() / dstW
        val yRatio = srcH.toDouble() / dstH

        for (y in 0 until dstH) {
            val srcY = (y * yRatio).toInt().coerceIn(0, srcH - 1)
            val srcRowOffset = srcY * srcW
            val dstRowOffset = y * dstW
            for (x in 0 until dstW) {
                val srcX = (x * xRatio).toInt().coerceIn(0, srcW - 1)
                dst[dstRowOffset + x] = src[srcRowOffset + srcX]
            }
        }
        return dst
    }

    /**
     * Bilinear interpolation scaling — smoother results for photos and gradients.
     */
    private fun scaleBilinear(
        src: IntArray,
        srcW: Int, srcH: Int,
        dstW: Int, dstH: Int
    ): IntArray {
        val dst = IntArray(dstW * dstH)
        val xRatio = (srcW - 1).toDouble() / dstW.coerceAtLeast(1)
        val yRatio = (srcH - 1).toDouble() / dstH.coerceAtLeast(1)

        for (y in 0 until dstH) {
            val yFloor = (y * yRatio).toInt()
            val yFrac = (y * yRatio) - yFloor
            val y0 = yFloor.coerceIn(0, srcH - 1)
            val y1 = (yFloor + 1).coerceIn(0, srcH - 1)

            for (x in 0 until dstW) {
                val xFloor = (x * xRatio).toInt()
                val xFrac = (x * xRatio) - xFloor
                val x0 = xFloor.coerceIn(0, srcW - 1)
                val x1 = (xFloor + 1).coerceIn(0, srcW - 1)

                // Four neighbors
                val topLeft = src[y0 * srcW + x0]
                val topRight = src[y0 * srcW + x1]
                val bottomLeft = src[y1 * srcW + x0]
                val bottomRight = src[y1 * srcW + x1]

                // Bilinear interpolation
                val top = topLeft + (xFrac * (topRight - topLeft))
                val bottom = bottomLeft + (xFrac * (bottomRight - bottomLeft))
                val value = top + (yFrac * (bottom - top))

                dst[y * dstW + x] = value.toInt().coerceIn(0, 255)
            }
        }
        return dst
    }

    /**
     * Crops a grayscale image to remove white borders (auto-trim).
     * Useful for cleaning up scanned documents before printing.
     *
     * @param threshold Pixels brighter than this are considered "white" (0-255).
     */
    fun autoTrim(
        grayscale: IntArray,
        width: Int,
        height: Int,
        threshold: Int = 240
    ): Triple<IntArray, Int, Int> {
        var top = 0
        var bottom = height - 1
        var left = width - 1
        var right = 0

        // Find bounds
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (grayscale[y * width + x] < threshold) {
                    if (y < top || top == 0) top = y
                    if (y > bottom || bottom == height - 1) bottom = y
                    if (x < left) left = x
                    if (x > right) right = x
                }
            }
        }

        // If entirely white
        if (right < left) return Triple(grayscale, width, height)

        val newW = right - left + 1
        val newH = bottom - top + 1
        val cropped = IntArray(newW * newH)

        for (y in 0 until newH) {
            for (x in 0 until newW) {
                cropped[y * newW + x] = grayscale[(y + top) * width + (x + left)]
            }
        }

        return Triple(cropped, newW, newH)
    }
}
