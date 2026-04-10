package ngga.ring.printer.manager

import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.DiscoveredPrinter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * JVM Implementation for Network (TCP) printers.
 */
class JvmNetworkConnector : PrinterConnector {
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
            socket?.outputStream?.write(data)
            socket?.outputStream?.flush()
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

actual class PrinterConnectorFactory {
    actual constructor()

    actual fun create(config: PrinterConfig): PrinterConnector {
        return when (config.connectionType) {
            "NETWORK" -> JvmNetworkConnector()
            // USB and Bluetooth on JVM often require native libs like jSerialComm 
            // or specialized drivers. We provide the architecture for expansion.
            else -> object : PrinterConnector {
                override suspend fun connect(config: PrinterConfig) = false
                override suspend fun sendData(data: ByteArray) = false
                override suspend fun disconnect() {}
                override fun isConnected() = false
            }
        }
    }

    actual suspend fun discovery(
        type: String, 
        onLog: (String) -> Unit
    ): Flow<List<DiscoveredPrinter>> = flow {
        onLog("Discovery started on JVM for $type...")
        val virtualList = listOf(
            DiscoveredPrinter("[VIRTUAL] $type JVM Printer", type, if(type == "NETWORK") "192.168.1.103" else "COM1-VIRTUAL")
        )
        emit(virtualList)
    }
}
