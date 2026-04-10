package ngga.ring.printer.manager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ngga.ring.printer.model.PrinterConfig
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Android Implementation for Network (LAN).
 */
class AndroidNetworkConnector : PrinterConnector {
    private var socket: Socket? = null

    override suspend fun connect(config: PrinterConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            socket = Socket()
            socket?.connect(InetSocketAddress(config.address ?: "127.0.0.1", config.port), 5000)
            socket?.isConnected ?: false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sendData(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            socket?.getOutputStream()?.write(data)
            socket?.getOutputStream()?.flush()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            socket?.close()
            socket = null
        } catch (e: Exception) {}
    }

    override fun isConnected(): Boolean = socket?.isConnected ?: false
}
