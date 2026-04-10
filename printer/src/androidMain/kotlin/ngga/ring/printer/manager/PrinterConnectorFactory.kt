package ngga.ring.printer.manager

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbDevice
import ngga.ring.data.model.PrinterConfigEntity
import ngga.ring.data.model.DiscoveredPrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*

/**
 * Android Implementation for Network (LAN).
 */
class AndroidNetworkConnector : PrinterConnector {
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

/**
 * Android Implementation for Bluetooth Classic.
 */
class AndroidBluetoothConnector : PrinterConnector {
    private var socket: BluetoothSocket? = null
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    override suspend fun connect(config: PrinterConfigEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            val device = adapter.getRemoteDevice(config.macAddress ?: return@withContext false)
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket?.connect()
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

/**
 * Android Implementation for USB (ESC/POS).
 */
class AndroidUsbConnector(private val context: Context) : PrinterConnector {
    private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var usbEndpoint: UsbEndpoint? = null

    override suspend fun connect(config: PrinterConfigEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val deviceList = usbManager.deviceList
            
            // macAddress here used for VID:PID
            val ids = config.macAddress?.split(":") ?: return@withContext false
            if (ids.size < 2) return@withContext false
            
            val device = deviceList.values.find { 
                it.vendorId.toString() == ids[0] && it.productId.toString() == ids[1]
            } ?: return@withContext false

            usbInterface = device.getInterface(0)
            usbEndpoint = if (usbInterface != null) {
                (0 until usbInterface!!.endpointCount)
                    .map { usbInterface!!.getEndpoint(it) }
                    .find { it.direction == UsbConstants.USB_DIR_OUT }
            } else null
            
            if (usbEndpoint == null) return@withContext false

            usbConnection = usbManager.openDevice(device)
            usbConnection?.claimInterface(usbInterface, true) ?: false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sendData(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = usbConnection?.bulkTransfer(usbEndpoint, data, data.size, 5000) ?: -1
            result >= 0
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            usbConnection?.releaseInterface(usbInterface)
            usbConnection?.close()
            usbConnection = null
            usbInterface = null
            usbEndpoint = null
        } catch (e: Exception) {}
    }

    override fun isConnected(): Boolean = usbConnection != null
}

/**
 * Android Factory implementation.
 */
actual class PrinterConnectorFactory {
    private val context: Context

    actual constructor() {
        this.context = PrinterInitializer.getContext()
    }

    constructor(context: Context) {
        this.context = context
    }

    actual fun create(config: PrinterConfigEntity): PrinterConnector {
        return when (config.connectionType) {
            "NETWORK" -> AndroidNetworkConnector()
            "BLUETOOTH" -> AndroidBluetoothConnector()
            "USB" -> AndroidUsbConnector(context)
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
            "BLUETOOTH" -> bluetoothDiscovery(onLog)
            "USB" -> usbDiscovery(onLog)
            "NETWORK" -> networkDiscovery(onLog)
            else -> flow { emit(emptyList<DiscoveredPrinter>()) }
        }
    }

    private fun bluetoothDiscovery(onLog: (String) -> Unit): Flow<List<DiscoveredPrinter>> = callbackFlow {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val discoveredDevices = mutableSetOf<DiscoveredPrinter>()

        onLog("Starting Bluetooth discovery...")
        // Add virtual device for UI verification
        discoveredDevices.add(DiscoveredPrinter("[VIRTUAL] Bluetooth Android", "BLUETOOTH", "00:AA:BB:CC:DD:EE"))
        trySend(discoveredDevices.toList())

        if (adapter == null) {
            onLog("Error: Bluetooth adapter not available on this device")
            close()
            return@callbackFlow
        }

        if (!adapter.isEnabled) {
            onLog("Warning: Bluetooth is disabled. Please enable it.")
        }

        // 1. Point bonded devices first
        val bonded = adapter.bondedDevices
        onLog("Checking ${bonded?.size ?: 0} paired devices...")
        bonded?.forEach { device ->
            discoveredDevices.add(DiscoveredPrinter(
                name = device.name ?: "Unknown",
                connectionType = "BLUETOOTH",
                address = device.address
            ))
            trySend(discoveredDevices.toList())
        }

        // 2. Scan for new devices
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        device?.let {
                            onLog("Found: ${it.name ?: "Unnamed device"}")
                            discoveredDevices.add(DiscoveredPrinter(
                                name = it.name ?: "Unknown Device",
                                connectionType = "BLUETOOTH",
                                address = it.address
                            ))
                            trySend(discoveredDevices.toList())
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> onLog("Scanning for new devices...")
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> onLog("New device scan complete.")
                }
            }
        }

        try {
            context.registerReceiver(receiver, IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            })
            val started = adapter.startDiscovery()
            if (!started) onLog("Scan failed to start (Check Runtime Permissions!)")
        } catch (e: Exception) {
            onLog("Error: ${e.message}")
        }

        awaitClose {
            adapter.cancelDiscovery()
            try { context.unregisterReceiver(receiver) } catch (e: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    private fun usbDiscovery(onLog: (String) -> Unit): Flow<List<DiscoveredPrinter>> = flow {
        onLog("Scanning USB devices...")
        val discovered = mutableListOf<DiscoveredPrinter>()
        discovered.add(DiscoveredPrinter("[VIRTUAL] USB Android Printer", "USB", "1234:5678"))
        
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val list = usbManager.deviceList
        onLog("System reports ${list.size} USB devices")

        list.values.forEach { device ->
            val printerName = device.productName ?: device.deviceName ?: "USB Device"
            val address = "${device.vendorId}:${device.productId}"
            onLog("Detected USB: $printerName ($address)")
            
            discovered.add(DiscoveredPrinter(
                name = printerName,
                connectionType = "USB",
                address = address
            ))
        }
        emit(discovered)
    }.flowOn(Dispatchers.IO)

    private fun networkDiscovery(onLog: (String) -> Unit): Flow<List<DiscoveredPrinter>> = kotlinx.coroutines.flow.channelFlow {
        onLog("Searching Wi-Fi subnet (Port 9100)...")
        val discovered = mutableListOf<DiscoveredPrinter>()
        discovered.add(DiscoveredPrinter("[VIRTUAL] Network Android Printer", "NETWORK", "192.168.1.101", port = 9100))
        send(discovered.toList())
        
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress
            
            if (ipAddress == 0) {
                onLog("Error: Device has no Wi-Fi IP. Connect to Wi-Fi first.")
            } else {
                // Android ipAddress is Little Endian: 0xDDCCBBAA for AA.BB.CC.DD
                val a = (ipAddress shr 0) and 0xFF
                val b = (ipAddress shr 8) and 0xFF
                val c = (ipAddress shr 16) and 0xFF
                
                val baseSubnet = "$a.$b.$c"
                onLog("Local IP detected. Scanning subnet $baseSubnet.1-254...")
                
                // Parallel scanning on Android
                for (i in 1..254) {
                    launch {
                        val host = "$baseSubnet.$i"
                        if (i % 50 == 0) onLog("Searching $host...")
                        
                        if (isPortOpen(host, 9100, 300)) {
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
                            onLog("Found Printer: $host")
                            send(discovered.toList())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            onLog("Network scan error: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

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
