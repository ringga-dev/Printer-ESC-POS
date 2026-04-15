package ngga.ring.printer.manager

import io.github.aakira.napier.Napier
import ngga.ring.printer.model.PrinterConfig

/**
 * Virtual Printer Emulator for Unit Testing and Debugging.
 * Redirects ESC/POS commands to console log.
 */
class VirtualPrinterConnector : PrinterConnector {
    private var connected = false
    private var isBold = false
    private var isUnderline = false
    private var isInverted = false
    private var alignment = "LEFT"

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
        var i = 0
        while (i < data.size) {
            val b = data[i].toInt() and 0xFF
            
            // Look for commands
            if (b == 0x1B) {
                if (i + 1 < data.size) {
                    val next = data[i + 1].toInt() and 0xFF
                    when (next) {
                        0x45 -> { // Bold
                            isBold = (data[i + 2].toInt() == 1)
                            i += 3; continue
                        }
                        0x2D -> { // Underline
                            isUnderline = (data[i + 2].toInt() == 1)
                            i += 3; continue
                        }
                        0x61 -> { // Alignment
                            alignment = when (data[i + 2].toInt()) {
                                1 -> "CENTER"
                                2 -> "RIGHT"
                                else -> "LEFT"
                            }
                            i += 3; continue
                        }
                        0x40 -> { // Initialize
                            isBold = false; isUnderline = false; isInverted = false; alignment = "LEFT"
                            i += 2; continue
                        }
                    }
                }
            } else if (b == 0x1D) {
                if (i + 1 < data.size) {
                    val next = data[i + 1].toInt() and 0xFF
                    when (next) {
                        0x42 -> { // Invert
                            isInverted = (data[i + 2].toInt() == 1)
                            i += 3; continue
                        }
                    }
                }
            }

            when (b) {
                0x0A -> {
                    val currentText = line.toString()
                    val padding = 43 - currentText.length
                    if (padding > 0) line.append(" ".repeat(padding))
                    line.append("│")
                    Napier.d(line.toString())
                    line = StringBuilder("│ ")
                }
                in 0x20..0x7E -> {
                    // Prefix with style markers if state changed
                    val char = b.toChar()
                    line.append(char)
                    if (line.length >= 40) {
                        line.append(" │")
                        Napier.d(line.toString())
                        line = StringBuilder("│ ")
                    }
                }
            }
            i++
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
