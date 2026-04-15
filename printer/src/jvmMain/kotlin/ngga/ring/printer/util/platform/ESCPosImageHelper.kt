package ngga.ring.printer.util.platform

import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.Color
import ngga.ring.printer.util.escpos.ESCPosImageHelper as CommonHelper

/**
 * JVM/Desktop implementation of image processing using AWT.
 */
actual object ESCPosImageHelper {
    
    actual fun processToRaster(image: Any, maxWidth: Int): Triple<ByteArray, Int, Int> {
        val original = if (image is ByteArray) {
            javax.imageio.ImageIO.read(image.inputStream())
        } else {
            image as BufferedImage
        }
        
        // 1. Scaling
        val scale = maxWidth.toDouble() / original.width.toDouble()
        val targetWidth = maxWidth
        val targetHeight = (original.height * scale).toInt()
        
        val scaled = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB)
        val g = scaled.createGraphics()
        g.drawImage(original.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH), 0, 0, null)
        g.dispose()
        
        // 2. Pixel extraction & Grayscale
        val width = scaled.width
        val height = scaled.height
        val gray = DoubleArray(width * height)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = Color(scaled.getRGB(x, y), true)
                // Use a standard grayscale conversion formula
                gray[y * width + x] = color.red * 0.299 + color.green * 0.587 + color.blue * 0.114
            }
        }
        
        val bitonal = BooleanArray(width * height)
        
        // Floyd-Steinberg Dithering
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                val oldPixel = gray[index]
                val newPixel = if (oldPixel < 128) 0.0 else 255.0
                bitonal[index] = newPixel == 0.0 
                
                val error = oldPixel - newPixel
                
                if (x + 1 < width) gray[index + 1] += error * 7.0 / 16.0
                if (y + 1 < height) {
                    if (x - 1 >= 0) gray[(y + 1) * width + x - 1] += error * 3.0 / 16.0
                    gray[(y + 1) * width + x] += error * 5.0 / 16.0
                    if (x + 1 < width) gray[(y + 1) * width + x + 1] += error * 1.0 / 16.0
                }
            }
        }
        
        val bytes = CommonHelper.packPixelsToRaster(bitonal, width, height)
        return Triple(bytes, width, height)
    }
}
