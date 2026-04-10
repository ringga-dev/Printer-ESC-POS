package ngga.ring.printer.manager

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.os.Build
import android.hardware.usb.UsbManager
import ngga.ring.printer.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.util.*

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

    actual fun create(config: PrinterConfig): PrinterConnector {
        return when (config.connectionType) {
            "NETWORK" -> AndroidNetworkConnector()
            "BLUETOOTH" -> AndroidBluetoothConnector()
            "BLUETOOTH_LE" -> AndroidBleConnector(context)
            "USB" -> AndroidUsbConnector(context)
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
    ): Flow<List<DiscoveredPrinter>> {
        return when (type) {
            "BLUETOOTH" -> bluetoothDiscovery(config, onLog)
            "USB" -> usbDiscovery(config, onLog)
            "NETWORK" -> networkDiscovery(config, onLog)
            else -> flow { emit(emptyList<DiscoveredPrinter>()) }
        }
    }

    private fun bluetoothDiscovery(config: DiscoveryConfig, onLog: (String) -> Unit): Flow<List<DiscoveredPrinter>> = callbackFlow {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        val discoveredDevices = Collections.synchronizedSet(mutableSetOf<DiscoveredPrinter>())

        if (config.showVirtualDevices) {
            discoveredDevices.add(DiscoveredPrinter("[VIRTUAL] Bluetooth Android", "BLUETOOTH", "00:AA:BB:CC:DD:EE"))
            launch { send(discoveredDevices.toList()) }
        }

        if (adapter == null) {
            onLog("Error: Bluetooth adapter not available")
            close()
            return@callbackFlow
        }

        // 1. Bonded devices
        adapter.bondedDevices?.forEach { device ->
            discoveredDevices.add(DiscoveredPrinter(
                name = device.name ?: "Unknown (Classic)",
                connectionType = "BLUETOOTH",
                address = device.address
            ))
            launch { send(discoveredDevices.toList()) }
        }

        // 2. Scan for new devices
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        device?.let {
                            discoveredDevices.add(DiscoveredPrinter(
                                name = it.name ?: "Unknown Device",
                                connectionType = if (it.type == BluetoothDevice.DEVICE_TYPE_LE) "BLUETOOTH_LE" else "BLUETOOTH",
                                address = it.address
                            ))
                            launch { send(discoveredDevices.toList()) }
                        }
                    }
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        adapter.startDiscovery()

        awaitClose {
            adapter.cancelDiscovery()
            try { context.unregisterReceiver(receiver) } catch (e: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    private fun usbDiscovery(config: DiscoveryConfig, onLog: (String) -> Unit): Flow<List<DiscoveredPrinter>> = flow {
        val discovered = mutableListOf<DiscoveredPrinter>()
        if (config.showVirtualDevices) {
            discovered.add(DiscoveredPrinter("[VIRTUAL] USB Android Printer", "USB", "1234:5678"))
        }
        
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        usbManager.deviceList.values.forEach { device ->
            discovered.add(DiscoveredPrinter(
                name = device.productName ?: "USB Device",
                connectionType = "USB",
                address = "${device.vendorId}:${device.productId}"
            ))
        }
        emit(discovered)
    }.flowOn(Dispatchers.IO)

    private fun networkDiscovery(config: DiscoveryConfig, onLog: (String) -> Unit): Flow<List<DiscoveredPrinter>> = kotlinx.coroutines.flow.channelFlow {
        val discovered = mutableListOf<DiscoveredPrinter>()
        if (config.showVirtualDevices) {
            discovered.add(DiscoveredPrinter("[VIRTUAL] Network Android Printer", "NETWORK", "192.168.1.101", port = 9100))
            send(discovered.toList())
        }
        
        try {
            val ipAddress = getLocalIpAddress()
            if (ipAddress != null) {
                val parts = ipAddress.split(".")
                if (parts.size == 4) {
                    val baseSubnet = "${parts[0]}.${parts[1]}.${parts[2]}"
                    
                    for (i in 1..254) {
                        launch {
                            val host = "$baseSubnet.$i"
                            if (isPortOpen(host, 9100, config.networkScanTimeoutMs)) {
                                synchronized(discovered) {
                                    if (discovered.none { it.address == host }) {
                                        discovered.add(DiscoveredPrinter("Printer ($host)", "NETWORK", host, 9100))
                                    }
                                }
                                send(discovered.toList())
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            onLog("Network scan error: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    private fun getLocalIpAddress(): String? {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val linkProperties = connectivityManager.getLinkProperties(activeNetwork)
            linkProperties?.linkAddresses?.forEach { linkAddress ->
                val address = linkAddress.address
                if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                    return address.hostAddress
                }
            }
        } catch (e: Exception) {}
        return null
    }

    private fun isPortOpen(host: String, port: Int, timeout: Int): Boolean {
        return try {
            java.net.Socket().use { it.connect(java.net.InetSocketAddress(host, port), timeout); true }
        } catch (e: Exception) { false }
    }
}
