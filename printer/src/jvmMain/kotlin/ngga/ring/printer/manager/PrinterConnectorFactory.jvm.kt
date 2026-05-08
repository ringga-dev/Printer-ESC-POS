package ngga.ring.printer.manager

import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.DiscoveredPrinter
import ngga.ring.printer.model.DiscoveryConfig
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Collections
import com.fazecast.jSerialComm.SerialPort

/**
 * JVM Implementation for Network (TCP) printers.
 */
class JvmNetworkConnector : BasePrinterConnector() {
    private var socket: Socket? = null

    override suspend fun connect(config: PrinterConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            socket = Socket()
            socket?.connect(InetSocketAddress(config.address ?: "127.0.0.1", config.port), config.connectionTimeoutMs)
            socket?.soTimeout = config.readTimeoutMs
            socket?.isConnected ?: false
        } catch (e: Exception) {
            println("PrinterJVM: Network connection failed: ${e.message}")
            false
        }
    }

    override suspend fun sendRawData(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            socket?.outputStream?.write(data)
            socket?.outputStream?.flush()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun readData(count: Int, timeout: Long): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val input = socket?.inputStream ?: return@withContext null
            
            val start = System.currentTimeMillis()
            while (input.available() <= 0) {
                if (System.currentTimeMillis() - start > timeout) return@withContext null
                kotlinx.coroutines.delay(10)
            }
            
            val buffer = ByteArray(count.coerceAtMost(input.available()))
            val read = input.read(buffer)
            if (read > 0) buffer.copyOf(read) else null
        } catch (e: Exception) {
            null
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
            "USB", "BLUETOOTH", "SERIAL", "BLUETOOTH_LE" -> JvmSerialConnector()
            "VIRTUAL" -> VirtualPrinterConnector()
            else -> object : PrinterConnector {
                override suspend fun connect(config: PrinterConfig) = false
                override suspend fun sendData(data: ByteArray) = false
                override suspend fun readData(count: Int, timeout: Long) = null
                override suspend fun disconnect() {}
                override fun isConnected() = false
            }
        }
    }

    actual fun discovery(
        type: String, 
        config: DiscoveryConfig,
        onLog: (String) -> Unit
    ): Flow<List<DiscoveredPrinter>> = callbackFlow {
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
        } else if (type == "USB" || type == "BLUETOOTH" || type == "SERIAL" || type == "BLUETOOTH_LE") {
            launch(Dispatchers.IO) {
                onLog("JVM: Scanning Serial Ports...")
                val ports = SerialPort.getCommPorts()
                ports.forEach { port ->
                    val name = port.descriptivePortName ?: port.systemPortName
                    val address = port.systemPortName
                    
                    // Basic filtering to find printers
                    val lowerName = name.lowercase()
                    val isPrinter = lowerName.contains("printer") || lowerName.contains("esc") || lowerName.contains("pos")
                    
                    if (isPrinter || type == "SERIAL" || type == "USB") {
                        discoveredDevices.add(DiscoveredPrinter(name, type, address))
                        trySend(discoveredDevices.toList())
                    }
                }
                onLog("JVM: Found ${ports.size} total ports")
            }
        }

        awaitClose {
            // Cleanup if needed
        }
    }.flowOn(Dispatchers.IO)
}
