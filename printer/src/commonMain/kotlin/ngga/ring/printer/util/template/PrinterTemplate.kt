package ngga.ring.printer.util.template

import ngga.ring.printer.util.escpos.ESCPosCommandBuilder
import ngga.ring.printer.util.escpos.TextAlignment
import ngga.ring.printer.model.BarcodeType
import ngga.ring.printer.model.QRCodeLevel

/**
 * A declarative representation of a receipt element.
 */
sealed class TemplateElement {
    data class Text(
        val content: String,
        val alignment: TextAlignment = TextAlignment.LEFT,
        val isBold: Boolean = false,
        val height: Int = 1,
        val width: Int = 1
    ) : TemplateElement()

    data class KeyValue(
        val label: String,
        val value: String,
        val isBold: Boolean = false
    ) : TemplateElement()

    data class Divider(val char: Char = '-') : TemplateElement()
    
    data class Space(val lines: Int = 1) : TemplateElement()

    data class Image(
        val bytes: ByteArray,
        val width: Int,
        val height: Int,
        val alignment: TextAlignment = TextAlignment.CENTER
    ) : TemplateElement()

    data class Barcode(
        val data: String,
        val type: BarcodeType = BarcodeType.CODE128,
        val alignment: TextAlignment = TextAlignment.CENTER
    ) : TemplateElement()

    data class QRCode(
        val data: String,
        val size: Int = 8,
        val level: QRCodeLevel = QRCodeLevel.L,
        val alignment: TextAlignment = TextAlignment.CENTER
    ) : TemplateElement()
}

/**
 * A full receipt template.
 */
data class ReceiptTemplate(
    val elements: List<TemplateElement>
) {
    /**
     * Renders the template into ESC/POS commands using the builder.
     * @param variables Map of placeholder to real value (e.g., "total" -> "$100")
     */
    fun render(builder: ESCPosCommandBuilder, variables: Map<String, String> = emptyMap()) {
        builder.initialize()
        
        elements.forEach { element ->
            when (element) {
                is TemplateElement.Text -> {
                    val finalContent = injectVariables(element.content, variables)
                    builder.withAlignment(element.alignment) {
                        withTextSize(element.width, element.height) {
                            bold(element.isBold)
                            line(finalContent)
                        }
                    }
                }
                is TemplateElement.KeyValue -> {
                    val label = injectVariables(element.label, variables)
                    val value = injectVariables(element.value, variables)
                    builder.bold(element.isBold)
                    builder.segmentedLine(label, value)
                    builder.bold(false)
                }
                is TemplateElement.Divider -> {
                    builder.divider(element.char)
                }
                is TemplateElement.Space -> {
                    builder.feed(element.lines)
                }
                is TemplateElement.Image -> {
                    builder.image(element.bytes, element.width, element.height, element.alignment == TextAlignment.CENTER)
                }
                is TemplateElement.Barcode -> {
                    val data = injectVariables(element.data, variables)
                    builder.barcode(data, type = element.type, center = element.alignment == TextAlignment.CENTER)
                }
                is TemplateElement.QRCode -> {
                    val data = injectVariables(element.data, variables)
                    builder.qrCode(data, size = element.size, level = element.level, center = element.alignment == TextAlignment.CENTER)
                }
            }
        }
        
        builder.feed(3)
        builder.cut()
    }

    private fun injectVariables(text: String, variables: Map<String, String>): String {
        var result = text
        variables.forEach { (key, value) ->
            result = result.replace("{{$key}}", value)
        }
        return result
    }
}
