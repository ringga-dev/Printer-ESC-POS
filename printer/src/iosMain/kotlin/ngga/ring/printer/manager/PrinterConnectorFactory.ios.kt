package ngga.ring.printer.manager

import ngga.ring.printer.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import platform.CoreBluetooth.*
import platform.Foundation.*
import platform.darwin.NSObject
import kotlinx.cinterop.*
import platform.posix.memcpy
import kotlin.coroutines.resume

/**
 * iOS Implementation for Bluetooth Low Energy (BLE).
 * Re-designed with a Delegate Pattern to avoid "Mixing Kotlin and Objective-C supertypes" errors.
 */
@OptIn(ExperimentalForeignApi::class)
class IosBluetoothConnector : PrinterConnector {
    private var centralManager: CBCentralManager? = null
    private var connectedPeripheral: CBPeripheral? = null
    private var writeCharacteristic: CBCharacteristic? = null
    private var continuation: CancellableContinuation<Boolean>? = null
    
    // Hold strong reference to avoid GC
    private var targetUUID: NSUUID? = null
    private val bleDelegate = BleDelegate()

    // Standard UUIDs for Thermal Printers
    private val SERVICE_UUID = CBUUID.UUIDWithString("FF00")
    private val WRITE_UUID = CBUUID.UUIDWithString("FF01")

    override suspend fun connect(config: PrinterConfig): Boolean = suspendCancellableCoroutine { cont ->
        if (config.address == null) {
            cont.resume(false)
            return@suspendCancellableCoroutine
        }
        
        this.continuation = cont
        this.targetUUID = NSUUID(uUIDString = config.address)
        
        if (centralManager == null) {
            centralManager = CBCentralManager(delegate = bleDelegate, queue = null)
        } else {
            if (centralManager?.state == CBManagerStatePoweredOn) {
                initiateConnection()
            }
        }
    }

    private fun initiateConnection() {
        val uuid = targetUUID ?: return
        val peripherals = centralManager?.retrievePeripheralsWithIdentifiers(listOf(uuid)) as? List<CBPeripheral>
        val peripheral = peripherals?.firstOrNull()
        
        if (peripheral != null) {
            connectedPeripheral = peripheral
            peripheral.delegate = bleDelegate
            centralManager?.connectPeripheral(peripheral, options = null)
        } else {
            centralManager?.scanForPeripheralsWithServices(null, null)
        }
    }

    override suspend fun sendData(data: ByteArray): Boolean = withContext(Dispatchers.Default) {
        val char = writeCharacteristic ?: return@withContext false
        val peripheral = connectedPeripheral ?: return@withContext false
        
        val nsData = data.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = data.size.toULong())
        }
        
        peripheral.writeValue(nsData, forCharacteristic = char, type = CBCharacteristicWriteWithoutResponse)
        true
    }

    override suspend fun disconnect() {
        connectedPeripheral?.let { centralManager?.cancelPeripheralConnection(it) }
        connectedPeripheral = null
        writeCharacteristic = null
        targetUUID = null
    }

    override fun isConnected(): Boolean = connectedPeripheral != null && writeCharacteristic != null

    /**
     * Internal Delegate to handle Objective-C CoreBluetooth events.
     */
    private inner class BleDelegate : NSObject(), CBCentralManagerDelegateProtocol, CBPeripheralDelegateProtocol {
        override fun centralManagerDidUpdateState(central: CBCentralManager) {
            if (central.state == CBManagerStatePoweredOn) {
                initiateConnection()
            } else if (central.state == CBManagerStatePoweredOff) {
                continuation?.resume(false)
                continuation = null
            }
        }

        override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral) {
            didConnectPeripheral.discoverServices(listOf(SERVICE_UUID))
        }

        override fun centralManager(central: CBCentralManager, didFailToConnectPeripheral: CBPeripheral, error: NSError?) {
            continuation?.resume(false)
            continuation = null
        }

        override fun peripheral(peripheral: CBPeripheral, didDiscoverServices: NSError?) {
            val service = (peripheral.services ?: emptyList<Any?>()).firstOrNull { 
                (it as? CBService)?.UUID == SERVICE_UUID 
            } as? CBService
            
            if (service != null) {
                peripheral.discoverCharacteristics(listOf(WRITE_UUID), forService = service)
            } else {
                (peripheral.services ?: emptyList<Any?>()).firstOrNull()?.let { 
                    peripheral.discoverCharacteristics(null, forService = it as CBService)
                }
            }
        }

        override fun peripheral(peripheral: CBPeripheral, didDiscoverCharacteristicsForService: CBService, error: NSError?) {
            val char = (didDiscoverCharacteristicsForService.characteristics ?: emptyList<Any?>()).firstOrNull {
                (it as? CBCharacteristic)?.UUID == WRITE_UUID
            } as? CBCharacteristic
            
            writeCharacteristic = char ?: (didDiscoverCharacteristicsForService.characteristics ?: emptyList<Any?>()).firstOrNull() as? CBCharacteristic
            
            if (writeCharacteristic != null) {
                continuation?.resume(true)
            } else {
                continuation?.resume(false)
            }
            continuation = null
        }
    }
}

class IosNetworkConnector : PrinterConnector {
    override suspend fun connect(config: PrinterConfig): Boolean = false
    override suspend fun sendData(data: ByteArray): Boolean = false
    override suspend fun disconnect() {}
    override fun isConnected(): Boolean = false
}

@OptIn(ExperimentalForeignApi::class)
actual class PrinterConnectorFactory {
    actual constructor()
    actual fun create(config: PrinterConfig): PrinterConnector {
        return when (config.connectionType) {
            "BLUETOOTH", "BLUETOOTH_LE" -> IosBluetoothConnector()
            "NETWORK" -> IosNetworkConnector()
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
        config: DiscoveryConfig,
        onLog: (String) -> Unit
    ): Flow<List<DiscoveredPrinter>> = callbackFlow {
        onLog("Scanning for $type...")
        val discoveredDevices = mutableListOf<DiscoveredPrinter>()

        if (config.showVirtualDevices) {
            discoveredDevices.add(DiscoveredPrinter("[VIRTUAL] $type iOS Printer", type, if(type == "NETWORK") "192.168.1.102" else "UUID-IOS-TEST-1234"))
            trySend(discoveredDevices.toList())
        }

        if (type == "BLUETOOTH" || type == "BLUETOOTH_LE") {
            val delegate = object : NSObject(), CBCentralManagerDelegateProtocol {
                override fun centralManagerDidUpdateState(central: CBCentralManager) {
                    if (central.state == CBManagerStatePoweredOn) {
                        central.scanForPeripheralsWithServices(null, null)
                    } else if (central.state == CBManagerStatePoweredOff) {
                        onLog("Bluetooth is OFF")
                    }
                }

                override fun centralManager(central: CBCentralManager, didDiscoverPeripheral: CBPeripheral, advertisementData: Map<Any?, *>, RSSI: NSNumber) {
                    val name = didDiscoverPeripheral.name ?: "Unknown Device"
                    val address = didDiscoverPeripheral.identifier.UUIDString
                    
                    if (discoveredDevices.none { it.address == address }) {
                        discoveredDevices.add(DiscoveredPrinter(name, "BLUETOOTH_LE", address))
                        trySend(discoveredDevices.toList())
                    }
                }
            }
            
            val centralManager = CBCentralManager(delegate = delegate, queue = null)
            awaitClose {
                centralManager.stopScan()
            }
        } else {
            delay(1000)
            close()
        }
    }.flowOn(Dispatchers.Main)
}
