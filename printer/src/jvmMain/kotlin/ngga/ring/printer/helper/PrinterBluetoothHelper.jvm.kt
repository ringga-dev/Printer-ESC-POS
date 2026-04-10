package ngga.ring.printer.helper

import ngga.ring.printer.util.ConnectionState
import ngga.ring.printer.util.ScanStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * JVM placeholder for PrinterBluetoothHelper.
 * Full Bluetooth support on JVM typically requires BlueCove or similar OS-specific natives.
 */
actual class PrinterBluetoothHelper {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    actual val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _scanState = MutableStateFlow(ScanStatus.Idle)
    actual val scanState: StateFlow<ScanStatus> = _scanState

    private val _discoveredDevices = MutableStateFlow<List<PrinterBluetoothDevice>>(emptyList())
    actual val discoveredDevices: StateFlow<List<PrinterBluetoothDevice>> = _discoveredDevices

    actual suspend fun startScan() {
        _scanState.value = ScanStatus.Error
    }

    actual suspend fun stopScan() {
        _scanState.value = ScanStatus.Idle
    }

    actual suspend fun connect(address: String, timeoutMs: Long): Boolean {
        _connectionState.value = ConnectionState.Error("Bluetooth not supported on JVM in this version")
        return false
    }

    actual suspend fun disconnect() {
        _connectionState.value = ConnectionState.Disconnected
    }

    actual suspend fun print(address: String, content: ByteArray): Boolean {
        return false
    }
}
