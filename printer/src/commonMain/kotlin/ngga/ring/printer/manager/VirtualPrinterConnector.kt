package ngga.ring.printer.manager

import io.github.aakira.napier.Napier
import ngga.ring.printer.model.PrinterConfig

/**
 * Virtual Printer Emulator for Unit Testing and Debugging.
 * Redirects ESC/POS commands to console log.
 */
class VirtualPrinterConnector : PrinterConnector {
    private var connected = false

    override suspend fun connect(config: PrinterConfig): Boolean {
        connected = true
        Napier.i("VirtualPrinter: Connected to virtual emulator")
        return true
    }

    override suspend fun sendData(data: ByteArray): Boolean {
        if (!connected) return false

        val sb = StringBuilder()
        sb.append("\n┌────────────────────────────────────────────────┐\n")
        sb.append("│             VIRTUAL PRINTER OUTPUT             │\n")
        sb.append("├────────────────────────────────────────────────┤\n")
        sb.append("│ ")

        data.forEach { byte ->
            when (byte) {
                0x0A.toByte() -> { // Line Feed
                    sb.append(" <LF>\n│ ")
                }
                0x1B.toByte() -> sb.append("<ESC>")
                0x1D.toByte() -> sb.append("<GS>")
                in 32..126 -> sb.append(byte.toInt().toChar()) // Readable ASCII
                else -> {
                    // Show hex for non-readable bytes
                    val hex = byte.toInt().and(0xFF).toString(16).uppercase().padStart(2, '0')
                    sb.append("[%${hex}]")
                }
            }
        }

        sb.append("\n└────────────────────────────────────────────────┘")
        
        Napier.d(sb.toString())
        return true
    }

    override suspend fun disconnect() {
        connected = false
        Napier.i("VirtualPrinter: Disconnected")
    }

    override fun isConnected(): Boolean = connected
}
