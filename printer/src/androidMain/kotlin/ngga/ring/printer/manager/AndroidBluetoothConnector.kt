package ngga.ring.printer.manager

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ngga.ring.printer.model.PrinterConfig
import java.util.*

/**
 * Android Implementation for Bluetooth Classic (SPP).
 */
@SuppressLint("MissingPermission")
class AndroidBluetoothConnector : PrinterConnector {
    private var socket: BluetoothSocket? = null
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    override suspend fun connect(config: PrinterConfig): Boolean = withContext(Dispatchers.IO) {
        val context = PrinterInitializer.getContext()
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter ?: return@withContext false
        
        try {
            val device = adapter.getRemoteDevice(config.address ?: return@withContext false)
            
            // Cancel discovery as it slows down connection
            adapter.cancelDiscovery()
            
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket?.connect()
            socket?.isConnected ?: false
        } catch (e: Exception) {
            // Fallback for some devices that require reflection
            try {
                val device = adapter.getRemoteDevice(config.address)
                val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                socket = m.invoke(device, 1) as BluetoothSocket
                socket?.connect()
                socket?.isConnected ?: false
            } catch (e2: Exception) {
                false
            }
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
