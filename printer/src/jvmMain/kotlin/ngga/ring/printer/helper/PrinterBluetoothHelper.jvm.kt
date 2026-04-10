package ngga.ring.printer.helper

import com.fazecast.jSerialComm.SerialPort
import ngga.ring.printer.util.ConnectionState
import ngga.ring.printer.util.ScanStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * JVM implementation for PrinterBluetoothHelper.
 * In Desktop environments, Bluetooth SPP printers are typically mapped to COM/Serial ports.
 * This helper scans for those ports.
 */
actual class PrinterBluetoothHelper {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    actual val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _scanState = MutableStateFlow(ScanStatus.Idle)
    actual val scanState: StateFlow<ScanStatus> = _scanState

    private val _discoveredDevices = MutableStateFlow<List<PrinterBluetoothDevice>>(emptyList())
    actual val discoveredDevices: StateFlow<List<PrinterBluetoothDevice>> = _discoveredDevices

    private var serialPort: SerialPort? = null

    actual suspend fun startScan() = withContext(Dispatchers.IO) {
        _scanState.value = ScanStatus.Scanning
        val ports = SerialPort.getCommPorts()
            .filter { it.descriptivePortName.contains("Bluetooth", true) || it.portDescription.contains("Bluetooth", true) }
            .map { port ->
                PrinterBluetoothDevice(
                    name = port.descriptivePortName,
                    address = port.systemPortName,
                    isPaired = true
                )
            }
        _discoveredDevices.value = ports
        _scanState.value = ScanStatus.Idle
    }

    actual suspend fun stopScan() {
        _scanState.value = ScanStatus.Idle
    }

    actual suspend fun connect(address: String, timeoutMs: Long): Boolean = withContext(Dispatchers.IO) {
        _connectionState.value = ConnectionState.Connecting
        try {
            val port = SerialPort.getCommPort(address)
            port.setBaudRate(9600)
            if (port.openPort()) {
                serialPort = port
                _connectionState.value = ConnectionState.Connected(port.descriptivePortName, address)
                true
            } else {
                _connectionState.value = ConnectionState.Error("Failed to open port $address")
                false
            }
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error("Connect error: ${e.message}")
            false
        }
    }

    actual suspend fun disconnect() = withContext(Dispatchers.IO) {
        serialPort?.closePort()
        serialPort = null
        _connectionState.value = ConnectionState.Disconnected
    }

    actual suspend fun print(address: String, content: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val port = serialPort ?: return@withContext false
        if (!port.isOpen) if (!connect(address, 5000)) return@withContext false
        
        val written = port.writeBytes(content, content.size)
        written == content.size
    }
}
