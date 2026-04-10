package ngga.ring.printer.helper

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import ngga.ring.printer.util.CommandQueue
import ngga.ring.printer.util.ConnectionState
import ngga.ring.printer.util.ScanStatus
import ngga.ring.printer.util.chunkedForWrite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import kotlin.collections.set
import kotlin.coroutines.resume
import kotlin.time.Clock

actual class PrinterBluetoothHelper(
    private val context: Context
) {
    companion object {
        internal var instance: PrinterBluetoothHelper? = null
    }

    init {
        instance = this
    }

    val turnOnRequestMap: MutableMap<Long, () -> Unit> = mutableMapOf()

    private val sppUUID: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val adapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(BluetoothManager::class.java)
        manager?.adapter
    }

    private var socket: BluetoothSocket? = null

    private val _connectionState =
        MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    actual val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _scanState =
        MutableStateFlow(ScanStatus.Idle)
    actual val scanState: StateFlow<ScanStatus> = _scanState

    private val _discoveredDevices =
        MutableStateFlow<List<PrinterBluetoothDevice>>(emptyList())
    actual val discoveredDevices: StateFlow<List<PrinterBluetoothDevice>> = _discoveredDevices

    private val queue = CommandQueue { bytes -> safeWrite(bytes) }.also { it.start() }


    private suspend fun turnOnAdapter() : Boolean = suspendCancellableCoroutine { continuation ->
        if (adapter?.isEnabled == false) {
            val identifier = Clock.System.now().toEpochMilliseconds()

            turnOnRequestMap[identifier] = {
                turnOnRequestMap.remove(identifier)
                continuation.resume(adapter?.isEnabled == true)
            }
            val enableBtIntent = Intent(context, BluetoothEnableContainerActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("identifier", identifier)
            context.startActivity(enableBtIntent)
        } else {
            continuation.resume(true)
        }
    }

    // ---------------------------------------------------------------------
    // SCAN
    // ---------------------------------------------------------------------
    @SuppressLint("MissingPermission")
    actual suspend fun startScan() = withContext(Dispatchers.IO) {
        val ok = turnOnAdapter()
        if (!ok) {
            _scanState.value = ScanStatus.Error
            return@withContext
        }

        val bt = adapter ?: run {
            _scanState.value = ScanStatus.Error
            return@withContext
        }

        _scanState.value = ScanStatus.Scanning

        try {
            val bonded = bt.bondedDevices.map {
                PrinterBluetoothDevice(it.name, it.address, true)
            }
            _discoveredDevices.value = bonded

            bt.cancelDiscovery()
            bt.startDiscovery()

            _scanState.value = ScanStatus.Idle
        } catch (e: Throwable) {
            _scanState.value = ScanStatus.Error
            _connectionState.value =
                ConnectionState.Error("Scan failed: ${e.message}", e)
        }
    }

    @SuppressLint("MissingPermission")
    actual suspend fun stopScan() = withContext(Dispatchers.IO) {
        try { adapter?.cancelDiscovery() } catch (_: Throwable) {}
        if (_scanState.value == ScanStatus.Scanning) {
            _scanState.value = ScanStatus.Idle
        }
    }

    // ---------------------------------------------------------------------
    // CONNECT
    // ---------------------------------------------------------------------
    @SuppressLint("MissingPermission")
    actual suspend fun connect(address: String, timeoutMs: Long): Boolean =
        withContext(Dispatchers.IO) {
            val ok = turnOnAdapter()
            if (!ok) return@withContext false

            val btAdapter = adapter ?: return@withContext false

            try {
                _connectionState.value = ConnectionState.Connecting

                btAdapter.cancelDiscovery()

                val device = btAdapter.getRemoteDevice(address)

                // Close previous connection if any
                try { socket?.close() } catch (_: Throwable) {}
                socket = null

                val newSocket = device.createRfcommSocketToServiceRecord(sppUUID)
                socket = newSocket

                newSocket.connect()

                _connectionState.value =
                    ConnectionState.Connected(device.name, device.address)
                true

            } catch (e: Throwable) {
                _connectionState.value =
                    ConnectionState.Error("Connect failed: ${e.message}", e)
                try { socket?.close() } catch (_: Throwable) {}
                socket = null
                false
            }
        }

    // ---------------------------------------------------------------------
    // PRINT
    // ---------------------------------------------------------------------
    actual suspend fun print(address: String, content: ByteArray): Boolean {
        val ok = turnOnAdapter()
        if (!ok) return false

        val current = connectionState.value

        val samePrinter =
            current is ConnectionState.Connected && current.address == address

        if (!samePrinter) {
            if (current is ConnectionState.Connected) disconnect()
            val ok = connect(address, timeoutMs = 6000)
            if (!ok) return false
        }

        // Chunk & enqueue
        content.chunkedForWrite(512).forEach { chunk ->
            queue.enqueue(chunk)
        }
        return true
    }

    // ---------------------------------------------------------------------
    // SAFE WRITE (ANTI BROKEN PIPE)
    // ---------------------------------------------------------------------
    private suspend fun safeWrite(bytes: ByteArray) = withContext(Dispatchers.IO) {
        val active = socket
        if (active == null || !active.isConnected) {
            throw IOException("Socket is closed before writing")
        }

        try {
            active.outputStream.write(bytes)
            active.outputStream.flush()
        } catch (e: IOException) {

            // HARD FIX BROKEN PIPE → reconnect & retry one time
            if (e.message?.contains("Broken pipe", true) == true) {
                val last = connectionState.value
                if (last is ConnectionState.Connected) {
                    reconnectAndRetry(last.address.orEmpty(), bytes)
                    return@withContext
                }
            }

            throw e
        }
    }

    private suspend fun reconnectAndRetry(address: String, bytes: ByteArray) {
        disconnect()
        val ok = connect(address, 6000)
        if (!ok) throw IOException("Reconnect failed")

        socket?.outputStream?.write(bytes)
        socket?.outputStream?.flush()
    }

    // ---------------------------------------------------------------------
    // DISCONNECT
    // ---------------------------------------------------------------------
    actual suspend fun disconnect() = withContext(Dispatchers.IO) {
        try { socket?.close() } catch (_: Throwable) {}
        socket = null
        _connectionState.value = ConnectionState.Disconnected
    }
}
