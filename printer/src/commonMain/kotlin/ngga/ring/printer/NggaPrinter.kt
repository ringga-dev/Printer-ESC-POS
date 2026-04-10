package ngga.ring.printer

import ngga.ring.printer.manager.PrinterConnector
import ngga.ring.printer.manager.PrinterConnectorFactory
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.ReceiptData
import ngga.ring.printer.model.BusinessInfo

/**
 * The "Satu Pintu" (Single Entry Point) for the printer library.
 * This class handles all printer operations using a unified Connector architecture.
 */
class NggaPrinter {

    /**
     * Platform-aware factory for creating printer connectors.
     * This handles Bluetooth (BLE/Classic), USB, and Network discovery and creation.
     */
    val connectorFactory = PrinterConnectorFactory()
    
    /**
     * Managed connector instance. Can be used for persistent connections.
     */
    private var activeConnector: PrinterConnector? = null

    val receiptService = ReceiptService()

    /**
     * Prints a professionally styled receipt using the specified configuration and data.
     * This method automatically handles:
     * 1. Multi-platform connection management.
     * 2. Receipt layout generation (Precision ESC/POS).
     * 3. Data transmission and cleanup.
     *
     * @param config The target printer configuration.
     * @param business Information about the merchant/store.
     * @param data The transaction details (Items, Tax, QR Code, etc).
     * @param role The template role (Default: "KASIR").
     * @return true if the print job was successfully transmitted.
     */
    suspend fun printReceipt(
        config: PrinterConfig,
        business: BusinessInfo?,
        data: ReceiptData,
        role: String = "KASIR"
    ): Boolean {
        val bytes = receiptService.generateReceipt(business, data, role)
        return printRaw(config, bytes)
    }

    /**
     * Sends raw ESC/POS bytes to the printer.
     * This is the lowest level call for custom printing logic.
     * 
     * @param config The target printer configuration.
     * @param data The raw byte array to send.
     */
    suspend fun printRaw(config: PrinterConfig, data: ByteArray): Boolean {
        val connector = activeConnector ?: connectorFactory.create(config)
        
        try {
            if (!connector.isConnected()) {
                val success = connector.connect(config)
                if (!success) return false
            }
            
            val sent = connector.sendData(data)
            
            // For one-off print jobs, we might want to disconnect. 
            // In a professional POS app, users usually keep the connection alive.
            // connector.disconnect() 
            
            return sent
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Manually disconnects the current active connector.
     */
    suspend fun disconnect() {
        activeConnector?.disconnect()
        activeConnector = null
    }
}
