package ngga.ring.printer.helper

import ngga.ring.printer.util.CommandQueue
import ngga.ring.printer.util.ConnectionState
import ngga.ring.printer.util.chunkedForWrite
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Android implementation of PrinterTcpHelper for Ethernet/WiFi printers.
 */
actual class PrinterTcpHelper {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    actual val connectionState: StateFlow<ConnectionState> = _connectionState

    @Volatile
    private var socket: Socket? = null

    // Cache for auto-reconnection
    private var lastHost: String? = null
    private var lastPort: Int = 9100

    private val queue = CommandQueue { bytes -> safeWrite(bytes) }.also { it.start() }

    actual suspend fun connect(host: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        _connectionState.value = ConnectionState.Connecting
        lastHost = host
        lastPort = port
        
        try {
            socket?.close()
        } catch (_: Exception) {}

        try {
            val newSocket = Socket()
            newSocket.connect(InetSocketAddress(host, port), 5000)
            socket = newSocket
            _connectionState.value = ConnectionState.Connected("Network Printer", "$host:$port")
            true
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error("Network Error: ${e.message}", e)
            false
        }
    }

    actual suspend fun print(host: String, port: Int, content: ByteArray): Boolean {
        val current = _connectionState.value
        
        // Auto-reconnect if destination changed or disconnected
        if (current !is ConnectionState.Connected || lastHost != host || lastPort != port) {
            if (!connect(host, port)) return false
        }

        // Chunk large data
        content.chunkedForWrite(2048).forEach { chunk ->
            queue.enqueue(chunk)
        }
        
        return true
    }

    private suspend fun safeWrite(bytes: ByteArray) = withContext(Dispatchers.IO) {
        val currentSocket = socket
        if (currentSocket == null || !currentSocket.isConnected || currentSocket.isClosed) {
            val host = lastHost ?: throw IOException("No network address cached")
            if (!connect(host, lastPort)) throw IOException("Failed to reconnect to network printer at $host")
        }

        val activeSocket = socket ?: throw IOException("Socket is null after reconnection attempt")

        try {
            activeSocket.getOutputStream().write(bytes)
            activeSocket.getOutputStream().flush()
        } catch (e: Exception) {
            // If it fails, mark as disconnected
            _connectionState.value = ConnectionState.Error("Write error: ${e.message}", e)
            throw e
        }
    }

    actual suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        _connectionState.value = ConnectionState.Disconnected
    }
}
