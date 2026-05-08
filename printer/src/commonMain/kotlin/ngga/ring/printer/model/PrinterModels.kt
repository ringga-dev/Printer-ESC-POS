package ngga.ring.printer.model

/**
 * Configuration for connecting to a thermal printer.
 * Standalone DTO to remove dependency on :data module.
 */
data class PrinterConfig(
    val name: String,
    val connectionType: String, // "BLUETOOTH", "USB", "NETWORK"
    val address: String? = null, // MAC for BT, IP for Network, VID:PID for USB
    val port: Int = 9100,
    val characterPerLine: Int = 31,
    val paperWidth: Int = 58,
    val paperWidthDots: Int = 0, // Physical Hardware Dots (e.g. 384 or 576)
    val leftMargin: Int = 0,
    val autoCenter: Boolean = false,
    val charsetName: String = "UTF-8",
    val escPosCodePage: Byte = 0x00,
    val connectionTimeoutMs: Int = 5000,
    val readTimeoutMs: Int = 2000,
)

/**
 * Detailed real-time status of the printer.
 */
data class PrinterStatus(
    val isOnline: Boolean = true,
    val isCoverOpen: Boolean = false,
    val isPaperOut: Boolean = false,
    val isPaperNearEnd: Boolean = false,
    val isError: Boolean = false,
    val rawBytes: ByteArray? = null
)

/**
 * Real-time events emitted by the printer monitor.
 */
sealed class PrinterStatusEvent {
    object Online : PrinterStatusEvent()
    object Offline : PrinterStatusEvent()
    object CoverOpen : PrinterStatusEvent()
    object PaperOut : PrinterStatusEvent()
    object PaperNearEnd : PrinterStatusEvent()
    data class Error(val message: String) : PrinterStatusEvent()
}

/**
 * Result from a printer discovery process.
 */
data class DiscoveredPrinter(
    val name: String,
    val connectionType: String,
    val address: String,
    val port: Int = 9100
)
