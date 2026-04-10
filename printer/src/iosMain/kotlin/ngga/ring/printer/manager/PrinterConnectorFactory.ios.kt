package ngga.ring.printer.manager

import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.DiscoveredPrinter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual class PrinterConnectorFactory {
    actual constructor()

    actual fun create(config: PrinterConfig): PrinterConnector {
        return object : PrinterConnector {
            override suspend fun connect(config: PrinterConfig): Boolean = false
            override suspend fun sendData(data: ByteArray): Boolean = false
            override suspend fun disconnect() {}
            override fun isConnected(): Boolean = false
        }
    }

    actual suspend fun discovery(
        type: String, 
        onLog: (String) -> Unit
    ): Flow<List<DiscoveredPrinter>> {
        return flow {
            onLog("Discovery not yet implemented for iOS")
            emit(emptyList<DiscoveredPrinter>())
        }
    }
}
