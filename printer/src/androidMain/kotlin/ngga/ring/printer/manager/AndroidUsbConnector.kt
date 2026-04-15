package ngga.ring.printer.manager

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ngga.ring.printer.model.PrinterConfig

/**
 * Android Implementation for USB OTG Printing.
 */
class AndroidUsbConnector : PrinterConnector {
    private var usbManager: UsbManager? = null
    private var usbDevice: UsbDevice? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var usbEndpoint: UsbEndpoint? = null

    private val ACTION_USB_PERMISSION = "ngga.ring.printer.USB_PERMISSION"

    override suspend fun connect(config: PrinterConfig): Boolean = withContext(Dispatchers.IO) {
        val context = PrinterInitializer.getContext()
        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        
        val deviceList = usbManager?.deviceList
        val address = config.address // Expected format "VID:PID"

        usbDevice = if (address != null && address.contains(":")) {
            val parts = address.split(":")
            val vid = parts[0].toIntOrNull()
            val pid = parts[1].toIntOrNull()
            deviceList?.values?.find { it.vendorId == vid && it.productId == pid }
        } else {
            // Find first printer if address not specified or invalid
            deviceList?.values?.find { device ->
                (0 until device.interfaceCount).any { i ->
                    device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_PRINTER
                }
            }
        }

        val device = usbDevice ?: return@withContext false

        if (usbManager?.hasPermission(device) == true) {
            openDevice(device)
        } else {
            // Request permission (Note: This is asynchronous and usually requires a receiver)
            // For now, we return false and assume the caller will handle permission 
            // OR we wait for permission.
            requestUsbPermission(context, device)
            false // Return false because permission is pending
        }
    }

    private fun requestUsbPermission(context: Context, device: UsbDevice) {
        val permissionIntent = PendingIntent.getBroadcast(
            context, 0, Intent(ACTION_USB_PERMISSION), 
            PendingIntent.FLAG_IMMUTABLE
        )
        usbManager?.requestPermission(device, permissionIntent)
    }

    private fun openDevice(device: UsbDevice): Boolean {
        usbInterface = (0 until device.interfaceCount)
            .map { device.getInterface(it) }
            .find { it.interfaceClass == UsbConstants.USB_CLASS_PRINTER }
            ?: device.getInterface(0)

        usbEndpoint = (0 until (usbInterface?.endpointCount ?: 0))
            .map { usbInterface?.getEndpoint(it) }
            .find { it?.direction == UsbConstants.USB_DIR_OUT }

        usbConnection = usbManager?.openDevice(device)
        return usbConnection?.claimInterface(usbInterface, true) ?: false
    }

    override suspend fun sendData(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val connection = usbConnection ?: return@withContext false
        val endpoint = usbEndpoint ?: return@withContext false
        
        val result = connection.bulkTransfer(endpoint, data, data.size, 5000)
        result >= 0
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        usbConnection?.releaseInterface(usbInterface)
        usbConnection?.close()
        usbConnection = null
        usbInterface = null
        usbEndpoint = null
        usbDevice = null
    }

    override fun isConnected(): Boolean = usbConnection != null
}
