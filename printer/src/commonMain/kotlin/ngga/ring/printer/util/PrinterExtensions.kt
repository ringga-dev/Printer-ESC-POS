package ngga.ring.printer.util

import ngga.ring.printer.manager.PrinterConnector
import ngga.ring.printer.util.escpos.ESCPosCommandBuilder
import ngga.ring.printer.util.template.ReceiptTemplate

/**
 * Conveniently prints a template using a specific connector.
 */
suspend fun PrinterConnector.printTemplate(
    template: ReceiptTemplate,
    data: Map<String, String>
): Boolean {
    if (!isConnected()) return false
    
    val builder = ESCPosCommandBuilder()
    template.render(builder, data)
    
    return sendData(builder.build())
}

/**
 * Conveniently prints a raw ESC/POS built sequence.
 */
suspend fun PrinterConnector.print(
    block: ESCPosCommandBuilder.() -> Unit
): Boolean {
    if (!isConnected()) return false
    
    val builder = ESCPosCommandBuilder()
    builder.block()
    
    return sendData(builder.build())
}
