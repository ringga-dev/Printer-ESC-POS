package ngga.ring.printer.manager

import ngga.ring.data.model.DiscoveredPrinter
import ngga.ring.printer.helper.PrinterBluetoothHelper
import ngga.ring.printer.helper.PrinterUsbHelper
import kotlinx.coroutines.flow.*

/**
 * Compatibility layer for printer discovery.
 * Bridges the old Flow-based discovery to the new Helper-based scan system.
 */
expect class PrinterConnectorFactory() {
    suspend fun discovery(type: String, onLog: (String) -> Unit): Flow<List<DiscoveredPrinter>>

    fun create(config: ngga.ring.data.model.PrinterConfigEntity): PrinterConnector
}
