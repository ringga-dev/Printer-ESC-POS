package ngga.ring.printer.helper

import ngga.ring.printer.util.ScanStatus
import ngga.ring.printer.util.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * iOS placeholder for PrinterUsbHelper.
 * iOS usually does not support standard USB ESC/POS printing over Lightning/Type-C without MFi.
 */
actual class PrinterUsbHelper {
    private val _scanState = MutableStateFlow(ScanStatus.Idle)
    actual val scanState: StateFlow<ScanStatus> = _scanState

    private val _discoveredDevices = MutableStateFlow<List<PrinterUsbDevice>>(emptyList())
    actual val discoveredDevices: StateFlow<List<PrinterUsbDevice>> = _discoveredDevices

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    actual val connectionState: StateFlow<ConnectionState> = _connectionState

    actual suspend fun startScan() {}
    actual suspend fun stopScan() {}
    actual suspend fun connect(vendorId: Int, productId: Int): Boolean = false
    actual suspend fun disconnect() {}
    actual suspend fun print(vendorId: Int, productId: Int, content: ByteArray): Boolean = false
}
