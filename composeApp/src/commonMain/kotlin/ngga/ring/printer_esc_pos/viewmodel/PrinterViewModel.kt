package ngga.ring.printer_esc_pos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ngga.ring.printer.KmpPrinter
import ngga.ring.printer.util.preview.PreviewBlock
import ngga.ring.printer.model.*
import ngga.ring.printer.util.ConnectionState

class PrinterViewModel : ViewModel() {
    private val printer = KmpPrinter()

    // --- Config State ---
    private val _config = MutableStateFlow(PrinterConfig(name = "Not Selected", connectionType = "VIRTUAL", address = ""))
    val config: StateFlow<PrinterConfig> = _config.asStateFlow()

    private val _showVirtual = MutableStateFlow(false)
    val showVirtual: StateFlow<Boolean> = _showVirtual.asStateFlow()

    // --- Discovery State ---
    private val _discoveryMode = MutableStateFlow("BLUETOOTH")
    val discoveryMode: StateFlow<String> = _discoveryMode.asStateFlow()

    private val _discoveredPrinters = MutableStateFlow<List<DiscoveredPrinter>>(emptyList())
    val discoveredPrinters: StateFlow<List<DiscoveredPrinter>> = _discoveredPrinters.asStateFlow()

    private val _discoveryLog = MutableStateFlow("Ready to scan...")
    val discoveryLog: StateFlow<String> = _discoveryLog.asStateFlow()

    // --- Connection & Print State ---
    val connectionState: StateFlow<ConnectionState> = printer.connectionState

    private val _printStatus = MutableStateFlow<PrintStatus>(PrintStatus.Idle)
    val printStatus: StateFlow<PrintStatus> = _printStatus.asStateFlow()

    private val _previewBlocks = MutableStateFlow<List<PreviewBlock>>(emptyList())
    val previewBlocks: StateFlow<List<PreviewBlock>> = _previewBlocks.asStateFlow()

    init {
        // Update preview when config changes
        _config.onEach { updatePreview(it) }.launchIn(viewModelScope)
        
        // Start auto-discovery when mode changes
        combine(_discoveryMode, _showVirtual) { mode, virtual -> mode to virtual }
            .onEach { (mode, virtual) ->
                startDiscovery(mode, virtual)
            }.launchIn(viewModelScope)
    }

    fun setDiscoveryMode(mode: String) {
        _discoveryMode.value = mode
    }

    fun toggleVirtual(enabled: Boolean) {
        _showVirtual.value = enabled
    }

    private fun startDiscovery(mode: String, showVirtual: Boolean) {
        viewModelScope.launch {
            printer.checkAndRequestPermissions(mode) { granted ->
                if (granted) {
                    doDiscovery(mode, showVirtual)
                } else {
                    _discoveryLog.value = "Permission denied. Please enable in settings."
                }
            }
        }
    }

    private fun doDiscovery(mode: String, showVirtual: Boolean) {
        viewModelScope.launch {
            val discoveryConfig = DiscoveryConfig(showVirtualDevices = showVirtual)
            printer.discovery(mode, discoveryConfig) { log ->
                _discoveryLog.value = log
            }.collectLatest { devices ->
                _discoveredPrinters.value = devices
            }
        }
    }

    fun selectPrinter(discovered: DiscoveredPrinter) {
        _config.value = _config.value.copy(
            name = discovered.name,
            connectionType = discovered.connectionType,
            address = discovered.address,
            port = discovered.port ?: 9100
        )
    }

    fun updateConfig(newConfig: PrinterConfig) {
        _config.value = newConfig
    }

    fun printTestPage() {
        viewModelScope.launch {
            printer.printTestPage(_config.value).collect { status ->
                _printStatus.value = status
            }
        }
    }

    fun printExpertTest() {
        viewModelScope.launch {
            val buildConfig = _config.value
            val data = printer.newCommandBuilder(buildConfig)
                .initialize()
                .selectCodePage(buildConfig.escPosCodePage)
                .line("EXPERT NATIVE TEST")
                .divider()
                .line("Native Barcode (128):")
                .barcode("KMP-PRINTER-V2")
                .feed(1)
                .line("Native QR Code:")
                .qrCodeNative("https://github.com/ringga-dev", size = 8, center = true)
                .feed(1)
                .line("Charset: ${buildConfig.charsetName}")
                .line("Special Char: " + if(buildConfig.charsetName == "UTF-8") "€ £ ¥ ©" else "Testing Charset")
                .feed(3)
                .cut()
                .build()
            
            printer.printRaw(buildConfig, data).collect { status ->
                _printStatus.value = status
            }
        }
    }

    fun resetPrintStatus() {
        _printStatus.value = PrintStatus.Idle
    }

    private fun updatePreview(config: PrinterConfig) {
        _previewBlocks.value = printer.receiptService.generateTestPreview(config)
    }
}
