package ngga.ring.printer.helper

import com.fazecast.jSerialComm.SerialPort
import ngga.ring.printer.util.ScanStatus
import ngga.ring.printer.util.ConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * JVM (Desktop) implementation of PrinterUsbHelper using jSerialComm.
 * Most USB thermal printers appear as Serial/COM ports on Desktop.
 */
actual class PrinterUsbHelper {

    private val _scanState = MutableStateFlow(ScanStatus.Idle)
    actual val scanState: StateFlow<ScanStatus> = _scanState

    private val _discoveredDevices = MutableStateFlow<List<PrinterUsbDevice>>(emptyList())
    actual val discoveredDevices: StateFlow<List<PrinterUsbDevice>> = _discoveredDevices

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    actual val connectionState: StateFlow<ConnectionState> = _connectionState

    @Volatile
    private var serialPort: SerialPort? = null
    private var lastVendorId: Int = 0
    private var lastProductId: Int = 0

    actual suspend fun startScan() = withContext(Dispatchers.IO) {
        _scanState.value = ScanStatus.Scanning
        val ports = SerialPort.getCommPorts().map { port ->
            PrinterUsbDevice(
                name = port.descriptivePortName,
                vendorId = port.vendorID,
                productId = port.productID,
                manufacturerName = port.portDescription,
                productName = port.systemPortName
            )
        }
        _discoveredDevices.value = ports
        _scanState.value = ScanStatus.Idle
    }

    actual suspend fun stopScan() {
        _scanState.value = ScanStatus.Idle
    }

    actual suspend fun connect(vendorId: Int, productId: Int): Boolean = withContext(Dispatchers.IO) {
        // In Desktop context, we often connect via Port Name or first found port
        val port = SerialPort.getCommPorts().firstOrNull { it.vendorID == vendorId && it.productID == productId }
            ?: SerialPort.getCommPorts().firstOrNull() ?: return@withContext false

        serialPort?.closePort()
        serialPort = port
        
        if (port.openPort()) {
            port.setBaudRate(9600)
            _connectionState.value = ConnectionState.Connected(port.descriptivePortName, port.systemPortName)
            true
        } else {
            _connectionState.value = ConnectionState.Error("Failed to open port: ${port.systemPortName}")
            false
        }
    }

    actual suspend fun print(vendorId: Int, productId: Int, content: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val activePort = serialPort ?: return@withContext false
        if (!activePort.isOpen) if (!connect(vendorId, productId)) return@withContext false
        
        val bytesWritten = activePort.writeBytes(content, content.size)
        bytesWritten == content.size
    }

    actual suspend fun disconnect() = withContext(Dispatchers.IO) {
        serialPort?.closePort()
        serialPort = null
        _connectionState.value = ConnectionState.Disconnected
    }
}
