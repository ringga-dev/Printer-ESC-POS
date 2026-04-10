package ngga.ring.printer.manager

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ngga.ring.printer.model.PrinterConfig
import java.util.*

/**
 * Android Implementation for Bluetooth Classic (SPP).
 */
class AndroidBluetoothConnector : PrinterConnector {
    private var socket: BluetoothSocket? = null
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    override suspend fun connect(config: PrinterConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return@withContext false
            val device = adapter.getRemoteDevice(config.address ?: return@withContext false)
            
            // Cancel discovery as it slows down connection
            adapter.cancelDiscovery()
            
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket?.connect()
            socket?.isConnected ?: false
        } catch (e: Exception) {
            // Fallback for some devices that require reflection
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
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
