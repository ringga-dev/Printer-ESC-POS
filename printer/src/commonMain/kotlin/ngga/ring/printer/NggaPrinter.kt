package ngga.ring.printer

import ngga.ring.printer.model.*
import ngga.ring.printer.util.ConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import ngga.ring.printer.manager.PrinterConnector
import ngga.ring.printer.manager.PrinterConnectorFactory
import ngga.ring.printer.util.escpos.ESCPosCommandBuilder

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
     * Managed connector instance.
     */
    private var activeConnector: PrinterConnector? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    
    /**
     * Observe the current connection status of the printer.
     */
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    val receiptService = ReceiptService()

    /**
     * Creates a new CommandBuilder pre-configured for the specific printer.
     */
    fun newCommandBuilder(config: PrinterConfig): ESCPosCommandBuilder {
        return ESCPosCommandBuilder.fromPrinterConfig(config)
    }

    /**
     * Discovers printers based on the specified type.
     */
    fun discovery(
        type: String, 
        config: DiscoveryConfig = DiscoveryConfig(),
        onLog: (String) -> Unit = {}
    ): Flow<List<DiscoveredPrinter>> {
        return connectorFactory.discovery(type, config, onLog)
    }

    /**
     * Prints a professionally styled receipt using the specified configuration and data.
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
        val connector = activeConnector ?: connectorFactory.create(config).also { activeConnector = it }
        
        try {
            if (!connector.isConnected()) {
                emit(PrintStatus.Connecting)
                _connectionState.value = ConnectionState.Connecting
                
                val success = connector.connect(config)
                if (!success) {
                    emit(PrintStatus.Error("Failed to connect to printer"))
                    _connectionState.value = ConnectionState.Error("Failed to connect to printer")
                    return@flow
                }
            }
            
            _connectionState.value = ConnectionState.Connected(config.name, config.address)
            emit(PrintStatus.Sending)
            val sent = connector.sendData(data)
            
            if (sent) {
                emit(PrintStatus.Success)
            } else {
                emit(PrintStatus.Error("Failed to send data to printer"))
            }

        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown print error")
            emit(PrintStatus.Error(e.message ?: "Unknown print error"))
        }
    }

    /**
     * Prints a professional hardware test page containing styles, barcodes, and QR codes.
     */
    fun printTestPage(config: PrinterConfig): Flow<PrintStatus> = flow {
        emit(PrintStatus.Processing)
        val bytes = receiptService.generateTestPrint(config)
        
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
        _connectionState.value = ConnectionState.Disconnected
    }
}
