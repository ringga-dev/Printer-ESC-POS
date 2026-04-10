package ngga.ring.printer.manager

import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.DiscoveredPrinter
import kotlinx.coroutines.flow.Flow

expect class PrinterConnectorFactory {
    constructor()
    fun create(config: PrinterConfig): PrinterConnector
    suspend fun discovery(type: String, onLog: (String) -> Unit): Flow<List<DiscoveredPrinter>>
}
