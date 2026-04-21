package ngga.ring.printer.util.platform

/**
 * WASM Image Helper Stub.
 */
actual object ESCPosImageHelper {
    actual fun processToRaster(image: Any, maxWidth: Int): Triple<ByteArray, Int, Int> {
        throw UnsupportedOperationException("Image processing is not supported on WASM yet.")
    }
}
