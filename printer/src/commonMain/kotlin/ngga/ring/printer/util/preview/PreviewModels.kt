package ngga.ring.printer.util.preview

import ngga.ring.printer.util.escpos.TextAlignment

/**
 * Logical representation of a receipt element for UI preview.
 */
sealed class PreviewBlock {
    data class Text(
        val text: String,
        val alignment: TextAlignment = TextAlignment.LEFT,
        val isBold: Boolean = false,
        val isBig: Boolean = false,
        val isUnderline: Boolean = false
    ) : PreviewBlock()

    data class KeyValue(
        val key: String,
        val value: String,
        val isBold: Boolean = false
    ) : PreviewBlock()

    data class Divider(val char: Char = '-') : PreviewBlock()
    
    data class Barcode(
        val content: String,
        val alignment: TextAlignment = TextAlignment.LEFT
    ) : PreviewBlock()
    
    data class Image(val alt: String = "[Image]") : PreviewBlock()
    
    data class QRCode(
        val content: String,
        val alignment: TextAlignment = TextAlignment.LEFT
    ) : PreviewBlock()
    
    object Space : PreviewBlock()
}
