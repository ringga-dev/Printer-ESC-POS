package ngga.ring.printer

import ngga.ring.printer.manager.PrinterConnector
import ngga.ring.printer.manager.PrinterConnectorFactory
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.ReceiptData
import ngga.ring.printer.model.BusinessInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ngga.ring.printer.model.PrintStatus

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
    fun printReceipt(
        config: PrinterConfig,
        data: ByteArray,
    ): Flow<PrintStatus> = flow {
        emit(PrintStatus.Processing)
        
        // Delegate to printRaw and collect its emissions
        printRaw(config, data).collect { status ->
            emit(status)
        }
    }

    /**
     * Sends raw ESC/POS bytes to the printer.
     * This is the lowest level call for custom printing logic.
     * 
     * @param config The target printer configuration.
     * @param data The raw byte array to send.
     */
    fun printRaw(config: PrinterConfig, data: ByteArray): Flow<PrintStatus> = flow {
        val connector = activeConnector ?: connectorFactory.create(config)
        
        try {
            if (!connector.isConnected()) {
                emit(PrintStatus.Connecting)
                val success = connector.connect(config)
                if (!success) {
                    emit(PrintStatus.Error("Failed to connect to printer"))
                    return@flow
                }
            }
            
            emit(PrintStatus.Sending)
            val sent = connector.sendData(data)
            
            if (sent) {
                emit(PrintStatus.Success)
            } else {
                emit(PrintStatus.Error("Failed to send data to printer"))
            }

        } catch (e: Exception) {
            emit(PrintStatus.Error(e.message ?: "Unknown print error"))
        }
    }

    /**
     * Prints a professional hardware test page containing styles, barcodes, and QR codes.
     */
    fun printTestPage(config: PrinterConfig, cpl: Int = 32): Flow<PrintStatus> = flow {
        emit(PrintStatus.Processing)
        val bytes = receiptService.generateTestPrint(cpl)
        
        printRaw(config, bytes).collect { status ->
            emit(status)
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
