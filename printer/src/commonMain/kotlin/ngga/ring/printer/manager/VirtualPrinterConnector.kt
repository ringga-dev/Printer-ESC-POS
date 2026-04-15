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

        Napier.d("┌──────────────────────────────────────────┐")
        Napier.d("│             VIRTUAL RECEIPT              │")
        Napier.d("├──────────────────────────────────────────┤")

        var line = StringBuilder("│ ")
        data.forEach { byte ->
            val b = byte.toInt() and 0xFF
            when (b) {
                0x0A -> {
                    // Line Feed: Close current line and print ENTER
                    val currentText = line.toString()
                    val padding = 43 - currentText.length
                    if (padding > 0) line.append(" ".repeat(padding))
                    line.append("│")
                    Napier.d(line.toString())
                    
                    Napier.d("│ [ENTER]                                 │")
                    line = StringBuilder("│ ")
                }
                in 0x20..0x7E -> {
                    line.append(b.toChar())
                    if (line.length >= 42) {
                        line.append("│")
                        Napier.d(line.toString())
                        line = StringBuilder("│ ")
                    }
                }
                else -> {
                    val hex = String.format("\\x%02X", b)
                    line.append(hex)
                    if (line.length >= 42) {
                        line.append("│")
                        Napier.d(line.toString())
                        line = StringBuilder("│ ")
                    }
                }
            }
        }
        
        if (line.length > 2) {
            val currentText = line.toString()
            val padding = 43 - currentText.length
            if (padding > 0) line.append(" ".repeat(padding))
            line.append("│")
            Napier.d(line.toString())
        }
        
        Napier.d("└──────────────────────────────────────────┘")
        return true
    }

    override suspend fun disconnect() {
        connected = false
        Napier.i("VirtualPrinter: Disconnected")
    }

    override fun isConnected(): Boolean = connected
}
