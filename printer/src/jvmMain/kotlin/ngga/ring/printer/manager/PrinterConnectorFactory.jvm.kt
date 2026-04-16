package ngga.ring.printer.manager

import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.DiscoveredPrinter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.channels.awaitClose
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Collections

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
            "VIRTUAL" -> VirtualPrinterConnector()
            else -> object : PrinterConnector {
                override suspend fun connect(config: PrinterConfig) = false
                override suspend fun sendData(data: ByteArray) = false
                override suspend fun disconnect() {}
                override fun isConnected() = false
            }
        }
    }

    actual fun discovery(
        type: String, 
        config: ngga.ring.printer.model.DiscoveryConfig,
        onLog: (String) -> Unit
    ): Flow<List<DiscoveredPrinter>> = callbackFlow<List<DiscoveredPrinter>> {
        val discoveredDevices = Collections.synchronizedSet(mutableSetOf<DiscoveredPrinter>())

        if (config.showVirtualDevices) {
            discoveredDevices.add(DiscoveredPrinter("[VIRTUAL] $type JVM Printer", type, if(type == "NETWORK") "192.168.1.103" else "COM1-VIRTUAL"))
            trySend(discoveredDevices.toList())
        }

        if (type == "NETWORK") {
            val socket = java.net.DatagramSocket().apply {
                broadcast = true
                soTimeout = config.networkScanTimeoutMs
            }

            launch(Dispatchers.IO) {
                try {
                    val probeData = byteArrayOf(0x1B, 0x40)
                    val packet = java.net.DatagramPacket(
                        probeData, probeData.size,
                        java.net.InetAddress.getByName("255.255.255.255"),
                        9100
                    )
                    socket.send(packet)
                    onLog("JVM: UDP Broadcast sent")

                    val buffer = ByteArray(1024)
                    while (isActive) {
                        val receivePacket = java.net.DatagramPacket(buffer, buffer.size)
                        try {
                            socket.receive(receivePacket)
                            val address = receivePacket.address.hostAddress
                            discoveredDevices.add(DiscoveredPrinter("Printer ($address)", "NETWORK", address, 9100))
                            trySend(discoveredDevices.toList())
                        } catch (e: java.net.SocketTimeoutException) {
                            break
                        }
                    }
                } catch (e: Exception) {
                    onLog("JVM Network discovery error: ${e.message}")
                } finally {
                    socket.close()
                }
            }
        }

        awaitClose {
            // Socket is closed in finally block or here if needed
        }
    }.flowOn(Dispatchers.IO)
}
