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
 * JVM (Desktop) implementation of PrinterTcpHelper.
 */
actual class PrinterTcpHelper {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    actual val connectionState: StateFlow<ConnectionState> = _connectionState

    @Volatile
    private var socket: Socket? = null
    private var lastHost: String? = null
    private var lastPort: Int = 9100

    private val queue = CommandQueue { bytes -> safeWrite(bytes) }.also { it.start() }

    actual suspend fun connect(host: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        _connectionState.value = ConnectionState.Connecting
        lastHost = host
        lastPort = port
        
        try { socket?.close() } catch (_: Exception) {}

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
        if (connectionState.value !is ConnectionState.Connected || lastHost != host) {
            if (!connect(host, port)) return false
        }
        content.chunkedForWrite(2048).forEach { queue.enqueue(it) }
        return true
    }

    private suspend fun safeWrite(bytes: ByteArray) = withContext(Dispatchers.IO) {
        val activeSocket = socket ?: throw IOException("Socket is null")
        activeSocket.getOutputStream().write(bytes)
        activeSocket.getOutputStream().flush()
    }

    actual suspend fun disconnect() = withContext(Dispatchers.IO) {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        _connectionState.value = ConnectionState.Disconnected
    }
}
