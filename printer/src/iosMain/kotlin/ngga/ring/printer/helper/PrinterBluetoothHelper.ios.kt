package ngga.ring.printer.helper

import ngga.ring.printer.util.ConnectionState
import ngga.ring.printer.util.ScanStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.CoreBluetooth.*
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.darwin.NSObject
import platform.Foundation.NSData
import platform.Foundation.create
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.memcpy

/**
 * iOS implementation of PrinterBluetoothHelper using CoreBluetooth (BLE).
 */
actual class PrinterBluetoothHelper : NSObject(), CBCentralManagerDelegateProtocol, CBPeripheralDelegateProtocol {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    actual val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _scanState = MutableStateFlow(ScanStatus.Idle)
    actual val scanState: StateFlow<ScanStatus> = _scanState

    private val _discoveredDevices = MutableStateFlow<List<PrinterBluetoothDevice>>(emptyList())
    actual val discoveredDevices: StateFlow<List<PrinterBluetoothDevice>> = _discoveredDevices

    private var centralManager: CBCentralManager? = null
    private var connectedPeripheral: CBPeripheral? = null
    private var writeCharacteristic: CBCharacteristic? = null

    init {
        centralManager = CBCentralManager(delegate = this, queue = null)
    }

    actual suspend fun startScan() {
        if (centralManager?.state == CBManagerStatePoweredOn) {
            _discoveredDevices.value = emptyList()
            _scanState.value = ScanStatus.Scanning
            centralManager?.scanForPeripheralsWithServices(null, null)
        } else {
            _scanState.value = ScanStatus.Error
        }
    }

    actual suspend fun stopScan() {
        centralManager?.stopScan()
        _scanState.value = ScanStatus.Idle
    }

    actual suspend fun connect(address: String, timeoutMs: Long): Boolean {
        // CoreBluetooth uses UUIDs instead of MAC addresses
        val peripheral = _discoveredPeripherals[address] ?: return false
        _connectionState.value = ConnectionState.Connecting
        connectedPeripheral = peripheral
        centralManager?.connectPeripheral(peripheral, null)
        return true
    }

    actual suspend fun disconnect() {
        connectedPeripheral?.let {
            centralManager?.cancelPeripheralConnection(it)
        }
    }

    actual suspend fun print(address: String, content: ByteArray): Boolean {
        val char = writeCharacteristic ?: return false
        val peripheral = connectedPeripheral ?: return false

        val data = content.toNSData()
        peripheral.writeValue(data, forCharacteristic = char, type = CBCharacteristicWriteWithResponse)
        return true
    }

    // ---------------------------------------------------------------------
    // Internal State
    // ---------------------------------------------------------------------
    private val _discoveredPeripherals = mutableMapOf<String, CBPeripheral>()

    // ---------------------------------------------------------------------
    // CBCentralManagerDelegate
    // ---------------------------------------------------------------------
    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        if (central.state == CBManagerStatePoweredOn && _scanState.value == ScanStatus.Scanning) {
            central.scanForPeripheralsWithServices(null, null)
        }
    }

    override fun centralManager(central: CBCentralManager, didDiscoverPeripheral: CBPeripheral, advertisementData: Map<Any?, *>, RSSI: NSNumber) {
        val name = didDiscoverPeripheral.name ?: "Unknown"
        val uuid = didDiscoverPeripheral.identifier.UUIDString
        
        if (!_discoveredPeripherals.containsKey(uuid)) {
            _discoveredPeripherals[uuid] = didDiscoverPeripheral
            val newList = _discoveredDevices.value.toMutableList()
            newList.add(PrinterBluetoothDevice(name, uuid, false))
            _discoveredDevices.value = newList
        }
    }

    override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral) {
        connectedPeripheral = didConnectPeripheral
        didConnectPeripheral.delegate = this
        didConnectPeripheral.discoverServices(null)
    }

    override fun centralManager(central: CBCentralManager, didFailToConnectPeripheral: CBPeripheral, error: NSError?) {
        _connectionState.value = ConnectionState.Error(error?.localizedDescription ?: "Connection failed")
    }

    override fun centralManager(central: CBCentralManager, didDisconnectPeripheral: CBPeripheral, error: NSError?) {
        _connectionState.value = ConnectionState.Disconnected
        connectedPeripheral = null
        writeCharacteristic = null
    }

    // ---------------------------------------------------------------------
    // CBPeripheralDelegate
    // ---------------------------------------------------------------------
    override fun peripheral(peripheral: CBPeripheral, didDiscoverServices: NSError?) {
        peripheral.services?.forEach { service ->
            val s = service as CBService
            peripheral.discoverCharacteristics(null, forService = s)
        }
    }

    override fun peripheral(peripheral: CBPeripheral, didDiscoverCharacteristicsForService: CBService, error: NSError?) {
        didDiscoverCharacteristicsForService.characteristics?.forEach { characteristic ->
            val c = characteristic as CBCharacteristic
            // Most thermal printers use characteristics with Write permission
            if (c.properties and CBCharacteristicPropertyWrite != 0uL || 
                c.properties and CBCharacteristicPropertyWriteWithoutResponse != 0uL) {
                writeCharacteristic = c
                _connectionState.value = ConnectionState.Connected(peripheral.name ?: "Printer", peripheral.identifier.UUIDString)
            }
        }
    }
}

// Helper to convert ByteArray to NSData
private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
}
