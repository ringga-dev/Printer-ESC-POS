package ngga.ring.printer.util.platform

/**
 * Common image processing interface for converting various platform image formats 
 * (Bitmap, UIImage, BufferedImage) to bitonal raster data.
 */
expect object ESCPosImageHelper {
    
    /**
     * Resizes and converts a platform-specific image to 1-bit raster data.
     * 
     * @param image The source image (Type: android.graphics.Bitmap, UIImage, or BufferedImage)
     * @param maxWidth Max width in dots (usually 384 or 576)
     * @return Triple of (bytes, width, height)
     */
    fun processToRaster(image: Any, maxWidth: Int = 384): Triple<ByteArray, Int, Int>
}
