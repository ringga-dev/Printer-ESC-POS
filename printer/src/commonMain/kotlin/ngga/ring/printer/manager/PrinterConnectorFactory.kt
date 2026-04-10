package ngga.ring.printer.manager

import ngga.ring.printer.model.DiscoveryConfig
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.DiscoveredPrinter
import kotlinx.coroutines.flow.Flow

expect class PrinterConnectorFactory {
    constructor()
    fun create(config: PrinterConfig): PrinterConnector
    fun discovery(
        type: String, 
        config: DiscoveryConfig = DiscoveryConfig(),
        onLog: (String) -> Unit
    ): Flow<List<DiscoveredPrinter>>
}
