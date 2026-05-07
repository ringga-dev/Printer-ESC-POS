package ngga.ring.printer.manager

import ngga.ring.printer.model.*
import kotlinx.coroutines.flow.*

/**
 * WASM Implementation of PrinterConnectorFactory.
 * Using stubs for Web APIs for now to ensure stable cross-platform compilation.
 */
actual class PrinterConnectorFactory actual constructor() {
    actual fun create(config: PrinterConfig): PrinterConnector {
        return when (config.connectionType) {
            "VIRTUAL" -> VirtualPrinterConnector()
            // Web APIs (Bluetooth, USB, Serial) require complex Wasm-JS interop.
            // Returning a placeholder that returns false to ensure successful compilation.
            else -> object : BasePrinterConnector() {
                override suspend fun connect(config: PrinterConfig) = false
                override suspend fun sendRawData(data: ByteArray) = false
                override suspend fun readData(count: Int, timeout: Long) = null
                override suspend fun disconnect() {}
                override fun isConnected() = false
            }
        }
    }

    actual fun discovery(
        type: String, 
        config: DiscoveryConfig,
        onLog: (String) -> Unit
    ): Flow<List<DiscoveredPrinter>> = flow {
        val devices = mutableListOf<DiscoveredPrinter>()
        if (config.showVirtualDevices) {
            devices.add(DiscoveredPrinter("[VIRTUAL] Wasm $type Printer", "VIRTUAL", "WASM-VIRTUAL-001"))
        }
        emit(devices)
    }
}
