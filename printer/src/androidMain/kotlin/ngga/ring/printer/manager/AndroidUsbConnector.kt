package ngga.ring.printer.manager

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ngga.ring.printer.model.PrinterConfig

/**
 * Android Implementation for USB (ESC/POS).
 */
class AndroidUsbConnector(private val context: Context) : PrinterConnector {
    private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var usbEndpoint: UsbEndpoint? = null

    override suspend fun connect(config: PrinterConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val deviceList = usbManager.deviceList
            
            // address here used for VID:PID
            val ids = config.address?.split(":") ?: return@withContext false
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
