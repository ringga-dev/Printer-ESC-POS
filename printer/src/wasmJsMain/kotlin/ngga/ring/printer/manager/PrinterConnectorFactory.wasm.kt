package ngga.ring.printer.manager

import ngga.ring.printer.model.*
import kotlinx.coroutines.flow.*

/**
 * WASM Implementation of PrinterConnectorFactory.
 */
actual class PrinterConnectorFactory actual constructor() {
    actual fun create(config: PrinterConfig): PrinterConnector {
        return object : PrinterConnector {
            override suspend fun connect(config: PrinterConfig) = false
            override suspend fun sendData(data: ByteArray) = false
            override suspend fun readData(count: Int, timeout: Long) = null
            override suspend fun disconnect() {}
            override fun isConnected() = false
        }
    }

    actual fun discovery(
        type: String, 
        config: DiscoveryConfig,
        onLog: (String) -> Unit
    ): Flow<List<DiscoveredPrinter>> = flow {
        emit(emptyList())
    }
}
