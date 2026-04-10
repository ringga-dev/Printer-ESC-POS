package ngga.ring.printer.manager

import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ngga.ring.data.model.DiscoveredPrinter
import ngga.ring.data.model.PrinterConfigEntity
import java.net.InetSocketAddress
import java.net.Socket

class JvmNetworkConnector : PrinterConnector {
    private var socket: Socket? = null

    override suspend fun connect(config: PrinterConfigEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            socket = Socket()
            socket?.connect(InetSocketAddress(config.ipAddress ?: "127.0.0.1", config.port), 5000)
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

class JvmUsbConnector : PrinterConnector {
    private var serialPort: SerialPort? = null

    override suspend fun connect(config: PrinterConfigEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val parts = config.macAddress?.split(":")
            val vid = parts?.getOrNull(0)?.toIntOrNull() ?: 0
            val pid = parts?.getOrNull(1)?.toIntOrNull() ?: 0
            
            // jSerialComm uses vendorID and productID (uppercase ID)
            val port = SerialPort.getCommPorts().find { it.vendorID == vid && it.productID == pid }
                ?: return@withContext false
            
            if (port.openPort()) {
                serialPort = port
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sendData(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        serialPort?.let { port ->
            val written = port.writeBytes(data, data.size)
            written == data.size
        } ?: false
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        serialPort?.closePort()
        serialPort = null
    }

    override fun isConnected(): Boolean = serialPort?.isOpen ?: false
}

actual class PrinterConnectorFactory {
    actual constructor()

    actual fun create(config: PrinterConfigEntity): PrinterConnector {
        return when (config.connectionType) {
            "NETWORK" -> JvmNetworkConnector()
            "USB" -> JvmUsbConnector()
            else -> object : PrinterConnector {
                override suspend fun connect(config: PrinterConfigEntity) = false
                override suspend fun sendData(data: ByteArray) = false
                override suspend fun disconnect() {}
                override fun isConnected() = false
            }
        }
    }

    actual suspend fun discovery(type: String, onLog: (String) -> Unit): Flow<List<DiscoveredPrinter>> {
        return when (type) {
            "NETWORK" -> networkDiscovery(onLog)
            "USB" -> usbDiscovery(onLog)
            "BLUETOOTH" -> flow { 
                onLog("Bluetooth not natively supported on this JVM setup")
                emit(listOf(DiscoveredPrinter("[VIRTUAL] Bluetooth JVM", "BLUETOOTH", "00:VIRTUAL:00"))) 
            }
            else -> flow { emit(emptyList()) }
        }
    }

    private fun usbDiscovery(onLog: (String) -> Unit): Flow<List<DiscoveredPrinter>> = flow {
        onLog("Scanning USB ports...")
        val discovered = mutableListOf<DiscoveredPrinter>()
        discovered.add(DiscoveredPrinter("[VIRTUAL] USB JVM Printer", "USB", "VID:0000:PID:0000"))
        
        val ports = SerialPort.getCommPorts()
        onLog("Found ${ports.size} COM ports available.")
        ports.forEach { port ->
            val isUsb = port.descriptivePortName.contains("USB", ignoreCase = true)
            onLog("-> ${port.systemPortName}: ${port.descriptivePortName} (USB: $isUsb)")
            
            discovered.add(DiscoveredPrinter(
                name = if (isUsb) port.descriptivePortName else "Serial Port (${port.systemPortName})",
                connectionType = "USB",
                address = "${port.systemPortName} (${port.vendorID}:${port.productID})"
            ))
        }
        emit(discovered)
    }.flowOn(Dispatchers.IO)

    private fun networkDiscovery(onLog: (String) -> Unit): Flow<List<DiscoveredPrinter>> = kotlinx.coroutines.flow.channelFlow {
        val discovered = mutableListOf<DiscoveredPrinter>()
        discovered.add(DiscoveredPrinter("[VIRTUAL] Network JVM Printer", "NETWORK", "192.168.1.254", port = 9100))
        send(discovered.toList())

        try {
            onLog("Detecting local network interfaces...")
            val localIps = getLocalIpAddresses()
            if (localIps.isEmpty()) {
                onLog("Error: No active network interfaces found.")
            }

            localIps.forEach { ip ->
                val ipParts = ip.split(".")
                if (ipParts.size == 4) {
                    val subnet = "${ipParts[0]}.${ipParts[1]}.${ipParts[2]}"
                    onLog("Scanning subnet $subnet.x (Interface: $ip)...")
                    
                    // Parallel scanning for each subnet
                    (1..254).forEach { i ->
                        launch {
                            val host = "$subnet.$i"
                            if (i % 50 == 0) onLog("Searching $subnet.1 - $subnet.$i...")
                            
                            if (isPortOpen(host, 9100, 250)) {
                                synchronized(discovered) {
                                    if (discovered.none { it.address == host }) {
                                        discovered.add(DiscoveredPrinter(
                                            name = "Network Printer ($host)",
                                            connectionType = "NETWORK",
                                            address = host,
                                            port = 9100
                                        ))
                                    }
                                }
                                onLog("Found: $host")
                                send(discovered.toList())
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            onLog("Scan error: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    private fun getLocalIpAddresses(): List<String> {
        val ips = mutableListOf<String>()
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                        ips.add(addr.hostAddress)
                    }
                }
            }
        } catch (e: Exception) {}
        return ips
    }

    private fun isPortOpen(host: String, port: Int, timeout: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeout)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
