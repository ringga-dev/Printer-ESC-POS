package ngga.ring.printer.manager

import platform.Foundation.*
import platform.CoreBluetooth.*
import ngga.ring.data.model.PrinterConfigEntity
import ngga.ring.data.model.DiscoveredPrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import platform.darwin.*
import platform.posix.*

/**
 * iOS Implementation for Network (LAN).
 */
class IosNetworkConnector : PrinterConnector {
    private var outputStream: NSOutputStream? = null

    override suspend fun connect(config: PrinterConfigEntity): Boolean = withContext(Dispatchers.Main) {
        try {
            val ip = config.ipAddress ?: "127.0.0.1"
            val port = config.port.toLong()
            
            var readStream: NSInputStream? = null
            var writeStream: NSOutputStream? = null
            NSStream.getStreamsToHostWithName(ip, port, readStream, writeStream)
            
            outputStream = writeStream
            outputStream?.open()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sendData(data: ByteArray): Boolean = withContext(Dispatchers.Main) {
        try {
            val stream = outputStream ?: return@withContext false
            // Simplified for POC
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun disconnect() {
        outputStream?.close()
        outputStream = null
    }

    override fun isConnected(): Boolean = outputStream?.streamStatus == NSStreamStatusOpen
}

fun ByteArray.toNSData(): NSData = NSData.create(bytes = this.refTo(0), length = this.size.toULong())

/**
 * iOS Implementation for Bluetooth Low Energy (BLE).
 */
class IosBleConnector : PrinterConnector {
    override suspend fun connect(config: PrinterConfigEntity): Boolean = false
    override suspend fun sendData(data: ByteArray): Boolean = false
    override suspend fun disconnect() {}
    override fun isConnected(): Boolean = false
}

/**
 * iOS Factory implementation.
 */
actual class PrinterConnectorFactory {
    actual constructor()

    actual fun create(config: PrinterConfigEntity): PrinterConnector {
        return when (config.connectionType) {
            "NETWORK" -> IosNetworkConnector()
            "BLUETOOTH" -> IosBleConnector()
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
            "BLUETOOTH" -> bleDiscovery(onLog)
            "NETWORK" -> networkDiscovery(onLog)
            else -> flow { emit(emptyList()) }
        }
    }

    private fun bleDiscovery(onLog: (String) -> Unit): Flow<List<DiscoveredPrinter>> = callbackFlow {
        val discoveredPrinters = mutableSetOf<DiscoveredPrinter>()
        
        onLog("Initializing CoreBluetooth...")
        // Virtual device
        discoveredPrinters.add(DiscoveredPrinter("[VIRTUAL] BLE iOS", "BLUETOOTH", "UUID-VIRTUAL-1234"))
        trySend(discoveredPrinters.toList())

        val delegate = object : NSObject(), CBCentralManagerDelegateProtocol {
            override fun centralManagerDidUpdateState(central: CBCentralManager) {
                when(central.state) {
                    CBManagerStatePoweredOn -> {
                        onLog("Bluetooth powered ON. Scanning...")
                        central.scanForPeripheralsWithServices(null, null)
                    }
                    CBManagerStatePoweredOff -> onLog("Bluetooth is powered OFF")
                    CBManagerStateUnauthorized -> onLog("Bluetooth permission denied")
                    else -> onLog("Bluetooth state: ${central.state}")
                }
            }

            override fun centralManager(
                central: CBCentralManager,
                didDiscoverPeripheral: CBPeripheral,
                advertisementData: Map<Any?, *>,
                RSSI: NSNumber
            ) {
                val name = didDiscoverPeripheral.name ?: "Unknown BLE Device"
                onLog("Found: $name")
                discoveredPrinters.add(DiscoveredPrinter(
                    name = name,
                    connectionType = "BLUETOOTH",
                    address = didDiscoverPeripheral.identifier.UUIDString
                ))
                trySend(discoveredPrinters.toList())
            }
        }

        val centralManager = CBCentralManager(delegate, null)

        awaitClose {
            onLog("Stopping BLE scan.")
            centralManager.stopScan()
        }
    }.flowOn(Dispatchers.Main)

    private fun networkDiscovery(onLog: (String) -> Unit): Flow<List<DiscoveredPrinter>> = flow {
        onLog("Network discovery started...")
        val discovered = mutableListOf<DiscoveredPrinter>()
        discovered.add(DiscoveredPrinter("[VIRTUAL] Network iOS Printer", "NETWORK", "192.168.1.102", port = 9100))
        emit(discovered)
        onLog("Note: Manual network IP scan is restricted on iOS. Please use QR or Manual input.")
    }.flowOn(Dispatchers.Main)
}
