package ngga.ring.printer.util.escpos

/**
 * Web Renderer Stub. 
 */
class WebESCPosRenderer : ESCPosRenderer {
    override suspend fun renderPdfPage(data: ByteArray, pageIndex: Int, targetWidth: Int): BooleanArray? = null
    override suspend fun renderSvg(svgString: String, targetWidth: Int): BooleanArray? = null
}

actual fun getPlatformRenderer(): ESCPosRenderer = WebESCPosRenderer()
