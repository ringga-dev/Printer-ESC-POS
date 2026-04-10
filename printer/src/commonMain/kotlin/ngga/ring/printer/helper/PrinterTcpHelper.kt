package ngga.ring.printer.helper

import ngga.ring.printer.util.ConnectionState
import kotlinx.coroutines.flow.StateFlow

/**
 * Representation of a discovered or configured network printer.
 */
data class PrinterTcpDevice(
    val label: String?,
    val host: String,
    val port: Int = 9100
)

/**
 * Helper for managing TCP/IP network printer communication.
 */
expect class PrinterTcpHelper {
    /** Current connection state of the printer. */
    val connectionState: StateFlow<ConnectionState>

    /** Connects to a specific network printer. */
    suspend fun connect(host: String, port: Int): Boolean

    /** Gracefully disconnects the current session. */
    suspend fun disconnect()

    /**
     * Sends raw bytes to the network printer. 
     * Auto-connects if host changes or connection is lost.
     */
    suspend fun print(host: String, port: Int, content: ByteArray): Boolean
}
