package ngga.ring.printer.helper

import ngga.ring.printer.util.ConnectionState
import ngga.ring.printer.util.ScanStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * iOS placeholder for PrinterBluetoothHelper.
 */
actual class PrinterBluetoothHelper {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    actual val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _scanState = MutableStateFlow(ScanStatus.Idle)
    actual val scanState: StateFlow<ScanStatus> = _scanState

    private val _discoveredDevices = MutableStateFlow<List<PrinterBluetoothDevice>>(emptyList())
    actual val discoveredDevices: StateFlow<List<PrinterBluetoothDevice>> = _discoveredDevices

    actual suspend fun startScan() {}
    actual suspend fun stopScan() {}
    actual suspend fun connect(address: String, timeoutMs: Long): Boolean = false
    actual suspend fun disconnect() {}
    actual suspend fun print(address: String, content: ByteArray): Boolean = false
}
