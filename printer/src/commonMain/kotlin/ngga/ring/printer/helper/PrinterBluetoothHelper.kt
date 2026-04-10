package ngga.ring.printer.helper

import ngga.ring.printer.util.ConnectionState
import ngga.ring.printer.util.ScanStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Representation of a discovered Bluetooth printer.
 */
data class PrinterBluetoothDevice(
    val name: String?,
    val address: String,
    val bonded: Boolean = false
)

/**
 * Helper for managing Bluetooth printer discovery and communication.
 */
expect class PrinterBluetoothHelper {
    /** Current connection state of the printer. */
    val connectionState: StateFlow<ConnectionState>

    /** Current status of device discovery. */
    val scanState: StateFlow<ScanStatus>

    /** List of discovered (and bonded) devices. */
    val discoveredDevices: StateFlow<List<PrinterBluetoothDevice>>

    /** Starts scanning for Bluetooth devices. */
    suspend fun startScan()

    /** Stops active scanning. */
    suspend fun stopScan()

    /** Connects to a specific printer by its hardware address. */
    suspend fun connect(address: String, timeoutMs: Long = 6000): Boolean

    /** Gracefully disconnects the current printer. */
    suspend fun disconnect()

    /**
     * Sends raw bytes to the printer. 
     * Auto-connects if the address matches but connection is lost.
     */
    suspend fun print(address: String, content: ByteArray): Boolean
}
