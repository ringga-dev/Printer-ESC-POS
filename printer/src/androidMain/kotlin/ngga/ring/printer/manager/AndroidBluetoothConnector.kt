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
        val address = config.address ?: return@withContext false
        val context = PrinterInitializer.getContext()
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter ?: return@withContext false
        
        // Ensure standard disconnect first to clear previous state
        disconnect()
        
        try {
            val device = adapter.getRemoteDevice(address)
            adapter.cancelDiscovery()
            
            // Try standard way
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket?.connect()
                if (socket?.isConnected == true) return@withContext true
            } catch (e: Exception) {
                socket?.close()
                socket = null
            }

            // Fallback for some devices (reflection)
            try {
                val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                socket = m.invoke(device, 1) as BluetoothSocket
                socket?.connect()
                socket?.isConnected == true
            } catch (e2: Exception) {
                socket?.close()
                socket = null
                false
            }
        } catch (e: Exception) {
            socket?.close()
            socket = null
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
            socket?.let { s ->
                if (s.isConnected) {
                    s.outputStream?.flush()
                }
                s.close()
            }
        } catch (e: Exception) {
            // Logged as silent but prevented from crashing
        } finally {
            socket = null
        }
        Unit
    }

    override fun isConnected(): Boolean = socket?.isConnected ?: false
}
