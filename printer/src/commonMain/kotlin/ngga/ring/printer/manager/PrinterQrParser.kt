package ngga.ring.printer.manager

import ngga.ring.data.model.DiscoveredPrinter

/**
 * Utility to parse printer information from a QR code.
 * Expected format: "Name|ConnectionType|Address|Port"
 * Example: "Kitchen Printer|NETWORK|192.168.1.100|9100"
 */
object PrinterQrParser {

    fun parse(data: String): DiscoveredPrinter? {
        return try {
            val parts = data.split("|")
            if (parts.size >= 3) {
                DiscoveredPrinter(
                    name = parts[0],
                    connectionType = parts[1].uppercase(),
                    address = parts[2],
                    port = parts.getOrNull(3)?.toIntOrNull() ?: 9100
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
