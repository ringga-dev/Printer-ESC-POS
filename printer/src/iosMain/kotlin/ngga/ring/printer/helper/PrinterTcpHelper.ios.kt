package ngga.ring.printer.helper

import ngga.ring.printer.util.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * iOS placeholder for PrinterTcpHelper.
 */
actual class PrinterTcpHelper {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    actual val connectionState: StateFlow<ConnectionState> = _connectionState

    actual suspend fun connect(host: String, port: Int): Boolean = false
    actual suspend fun disconnect() {}
    actual suspend fun print(host: String, port: Int, content: ByteArray): Boolean = false
}
