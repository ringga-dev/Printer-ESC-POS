package ngga.ring.printer.helper

import ngga.ring.printer.util.ScanStatus
import ngga.ring.printer.util.ConnectionState
import kotlinx.coroutines.flow.StateFlow

/**
 * Representation of a discovered USB printer.
 */
data class PrinterUsbDevice(
    val name: String?,
    val vendorId: Int,
    val productId: Int,
    val manufacturerName: String? = null,
    val productName: String? = null
)

/**
 * Helper for managing USB direct connection to printers.
 */
expect class PrinterUsbHelper {
    /** Current status of USB device discovery. */
    val scanState: StateFlow<ScanStatus>

    /** List of discovered USB devices. */
    val discoveredDevices: StateFlow<List<PrinterUsbDevice>>

    /** Current connection state of the USB printer. */
    val connectionState: StateFlow<ConnectionState>

    /** Scans for available USB devices with printer capabilities. */
    suspend fun startScan()

    /** Stops active USB scanning. */
    suspend fun stopScan()

    /** Connects to a specific USB printer. */
    suspend fun connect(vendorId: Int, productId: Int): Boolean

    /** Gracefully disconnects the current USB device. */
    suspend fun disconnect()

    /**
     * Sends raw bytes to the USB printer.
     * Auto-connects if the IDs match but connection is lost.
     */
    suspend fun print(vendorId: Int, productId: Int, content: ByteArray): Boolean
}
