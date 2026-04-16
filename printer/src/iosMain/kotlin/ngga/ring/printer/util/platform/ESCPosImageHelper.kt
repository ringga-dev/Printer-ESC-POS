package ngga.ring.printer.util.platform

import platform.UIKit.*
import platform.CoreGraphics.*
import platform.Foundation.*
import kotlinx.cinterop.*
import ngga.ring.printer.util.escpos.ESCPosImageHelper as CommonHelper

@OptIn(ExperimentalForeignApi::class)
actual object ESCPosImageHelper {
    
    actual fun processToRaster(image: Any, maxWidth: Int): Triple<ByteArray, Int, Int> {
        val uiImage = if (image is ByteArray) {
            val data = image.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = image.size.toULong())
            }
            UIImage.imageWithData(data) ?: throw IllegalArgumentException("Invalid image data")
        } else {
            image as UIImage
        }

        // 1. Scaling
        val width = uiImage.size.useContents { width }
        val height = uiImage.size.useContents { height }
        val scale = maxWidth.toDouble() / width
        val targetWidth = maxWidth.toDouble()
        val targetHeight = height * scale

        val size = CGSizeMake(targetWidth, targetHeight)
        UIGraphicsBeginImageContextWithOptions(size, false, 1.0)
        uiImage.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
        val scaledImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        if (scaledImage == null) throw IllegalStateException("Failed to scale image")

        // 2. Pixel extraction & Grayscale
        val w = targetWidth.toInt()
        val h = targetHeight.toInt()
        val totalPixels = w * h
        val gray = DoubleArray(totalPixels)

        val colorSpace = CGColorSpaceCreateDeviceRGB()
        val context = CGBitmapContextCreate(
            null, 
            w.toULong(), 
            h.toULong(), 
            8u, 
            (4 * w).toULong(), 
            colorSpace, 
            CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
        ) ?: throw IllegalStateException("Failed to create bitmap context")
        
        UIGraphicsPushContext(context)
        scaledImage.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
        UIGraphicsPopContext()
        
        val dataPtr = CGBitmapContextGetData(context) ?: throw IllegalStateException("Failed to get context data")
        val pixels = dataPtr.reinterpret<ByteVar>()
        
        for (i in 0 until totalPixels) {
            val r = pixels[i * 4].toInt() and 0xFF
            val g = pixels[i * 4 + 1].toInt() and 0xFF
            val b = pixels[i * 4 + 2].toInt() and 0xFF
            // Grayscale conversion
            gray[i] = r * 0.299 + g * 0.587 + b * 0.114
        }
        
        // Floyd-Steinberg Dithering
        val bitonal = BooleanArray(totalPixels)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val index = y * w + x
                val oldPixel = gray[index]
                val newPixel = if (oldPixel < 128) 0.0 else 255.0
                bitonal[index] = newPixel == 0.0 
                
                val error = oldPixel - newPixel
                
                if (x + 1 < w) gray[index + 1] += error * 7.0 / 16.0
                if (y + 1 < h) {
                    if (x - 1 >= 0) gray[(y + 1) * w + x - 1] += error * 3.0 / 16.0
                    gray[(y + 1) * w + x] += error * 5.0 / 16.0
                    if (x + 1 < w) gray[(y + 1) * w + x + 1] += error * 1.0 / 16.0
                }
            }
        }

        val rasterBytes = CommonHelper.packPixelsToRaster(bitonal, w, h)
        return Triple(rasterBytes, w, h)
    }
}
