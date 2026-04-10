package ngga.ring.printer.helper

import android.content.Context
import android.hardware.usb.*
import ngga.ring.printer.util.ScanStatus
import ngga.ring.printer.util.ConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * Android implementation of PrinterUsbHelper using USB Host API.
 */
actual class PrinterUsbHelper(
    context: Context
) {

    private val usbManager =
        context.getSystemService(Context.USB_SERVICE) as UsbManager

    private val _scanState = MutableStateFlow(ScanStatus.Idle)
    actual val scanState: StateFlow<ScanStatus> = _scanState

    private val _discoveredDevices =
        MutableStateFlow<List<PrinterUsbDevice>>(emptyList())
    actual val discoveredDevices: StateFlow<List<PrinterUsbDevice>> = _discoveredDevices

    private val _connectionState =
        MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    actual val connectionState: StateFlow<ConnectionState> = _connectionState

    private var usbDevice: UsbDevice? = null
    private var usbInterface: UsbInterface? = null
    private var usbEndpointOut: UsbEndpoint? = null
    private var usbConnection: UsbDeviceConnection? = null


    // -----------------------------------------------------
    // SCAN
    // -----------------------------------------------------
    actual suspend fun startScan() = withContext(Dispatchers.IO) {
        _scanState.value = ScanStatus.Scanning

        val list = usbManager.deviceList.values.map { dev ->
            PrinterUsbDevice(
                name = dev.productName,
                vendorId = dev.vendorId,
                productId = dev.productId,
                manufacturerName = dev.manufacturerName,
                productName = dev.productName
            )
        }

        _discoveredDevices.value = list
        _scanState.value = ScanStatus.Idle
    }

    actual suspend fun stopScan() {
        _scanState.value = ScanStatus.Idle
    }


    // -----------------------------------------------------
    // CONNECT
    // -----------------------------------------------------
    actual suspend fun connect(vendorId: Int, productId: Int): Boolean =
        withContext(Dispatchers.IO) {
            val dev = usbManager.deviceList.values.firstOrNull {
                it.vendorId == vendorId && it.productId == productId
            } ?: return@withContext false

            usbDevice = dev

            if (!usbManager.hasPermission(dev)) {
                _connectionState.value =
                    ConnectionState.Error("USB permission not granted")
                return@withContext false
            }

            var foundIntf: UsbInterface? = null
            var foundEp: UsbEndpoint? = null

            // Find the first Bulk OUT endpoint (common for printers)
            for (i in 0 until dev.interfaceCount) {
                val intf = dev.getInterface(i)
                for (j in 0 until intf.endpointCount) {
                    val ep = intf.getEndpoint(j)
                    if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                        ep.direction == UsbConstants.USB_DIR_OUT
                    ) {
                        foundIntf = intf
                        foundEp = ep
                        break
                    }
                }
                if (foundIntf != null) break
            }

            if (foundIntf == null || foundEp == null) {
                _connectionState.value =
                    ConnectionState.Error("Printer bulk OUT endpoint not found")
                return@withContext false
            }

            usbInterface = foundIntf
            usbEndpointOut = foundEp

            val conn = usbManager.openDevice(dev)
            if (conn == null) {
                _connectionState.value =
                    ConnectionState.Error("Failed to open USB device")
                return@withContext false
            }

            usbConnection = conn
            conn.claimInterface(foundIntf, true)

            _connectionState.value = ConnectionState.Connected(
                name = dev.productName ?: "USB Printer",
                address = "${dev.vendorId}:${dev.productId}"
            )

            true
        }


    // -----------------------------------------------------
    // PRINT
    // -----------------------------------------------------
    actual suspend fun print(vendorId: Int, productId: Int, content: ByteArray): Boolean =
        withContext(Dispatchers.IO) {
            val dev = usbDevice
            if (dev == null ||
                dev.vendorId != vendorId ||
                dev.productId != productId
            ) {
                // Attempt auto-reconnect if matching device is found
                if (!connect(vendorId, productId)) {
                    _connectionState.value = ConnectionState.Error("USB device not connected")
                    return@withContext false
                }
            }

            val conn = usbConnection ?: return@withContext false
            val ep = usbEndpointOut ?: return@withContext false

            // Standard timeout for mechanical printing
            val result = conn.bulkTransfer(ep, content, content.size, 5000)

            if (result < 0) {
                _connectionState.value = ConnectionState.Error("USB print failed (code: $result)")
                return@withContext false
            }

            return@withContext true
        }


    // -----------------------------------------------------
    // DISCONNECT
    // -----------------------------------------------------
    actual suspend fun disconnect() = withContext(Dispatchers.IO) {
        try { usbConnection?.releaseInterface(usbInterface) } catch (_: Exception) {}
        try { usbConnection?.close() } catch (_: Exception) {}

        usbDevice = null
        usbInterface = null
        usbEndpointOut = null
        usbConnection = null

        _connectionState.value = ConnectionState.Disconnected
    }
}
