package ngga.ring.printer

import ngga.ring.printer.util.escpos.*
import ngga.ring.printer.util.preview.*
import ngga.ring.printer.manager.*
import kotlin.test.*

class PrinterLibraryV3Test {

    @Test
    fun testImageScalerNearest() {
        val src = intArrayOf(0, 255, 128, 64)
        val (scaled, newW, newH) = ImageScaler.scaleToFit(src, 2, 2, 4, "NEAREST")
        assertEquals(4, newW)
        assertEquals(4, newH)
        assertEquals(16, scaled.size)
        assertEquals(0, scaled[0])
    }

    @Test
    fun testVirtualRenderer() {
        val data = byteArrayOf(
            0x1B, 0x40, // Init
            0x48, 0x65, 0x6C, 0x6C, 0x6F, // Hello
            0x0A // LF
        )
        val lines = ESCPosVirtualRenderer.render(data)
        assertTrue(lines.isNotEmpty())
        assertEquals("Hello", lines[0].content)
        assertEquals(LineType.TEXT, lines[0].type)
    }

    @Test
    fun testVirtualRendererCut() {
        val data = byteArrayOf(0x1D, 0x56, 0x01)
        val lines = ESCPosVirtualRenderer.render(data)
        assertTrue(lines.any { it.type == LineType.CUT })
    }

    @Test
    fun testNVGraphicsGeneration() {
        val src = intArrayOf(0, 255, 0, 255)
        val bytes = NVGraphicsHelper.defineNVBitImage(1, src, 4, 1)
        assertTrue(bytes.size > 3)
        assertEquals(0x1C.toByte(), bytes[0])
        assertEquals(0x71.toByte(), bytes[1])
    }

    @Test
    fun testImageScalerAutoTrim() {
        // 4x4 image with white borders
        val src = intArrayOf(
            255, 255, 255, 255,
            255, 0,   0,   255,
            255, 0,   0,   255,
            255, 255, 255, 255
        )
        val (trimmed, w, h) = ImageScaler.autoTrim(src, 4, 4, 200)
        assertEquals(2, w)
        assertEquals(2, h)
        assertEquals(4, trimmed.size)
        assertEquals(0, trimmed[0])
    }
}
