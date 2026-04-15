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
    val paperWidthDots: Int = 0, // Optional: Set explicitly for custom sizes
    val charsetName: String = "UTF-8",
    val escPosCodePage: Byte = 0x00
)

/**
 * Result from a printer discovery process.
 */
data class DiscoveredPrinter(
    val name: String,
    val connectionType: String,
    val address: String,
    val port: Int = 9100
)

/**
 * Minimal business information for receipt headers.
 */
data class BusinessInfo(
    val name: String,
    val address: String? = null,
    val phone: String? = null,
    val taxId: String? = null,
    val website: String? = null,
    val currencySymbol: String = "Rp",
    val logoBytes: ByteArray? = null // Reserved for logo printing
)

/**
 * Generic receipt data structure.
 */
data class ReceiptData(
    val headerId: String,
    val transactionId: String? = null,
    val customerName: String? = null,
    val timestamp: Long,
    val items: List<ReceiptItem>,
    val subtotal: Double = 0.0,
    val taxAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double,
    val paymentMethod: String? = null,
    val amountPaid: Double = 0.0,
    val amountReturn: Double = 0.0,
    val notes: String? = null,
    val footerMessage: String? = null,
    val verificationUrl: String? = null // For QR Code generation
)

/**
 * Generic item for receipt printing.
 */
data class ReceiptItem(
    val name: String,
    val quantity: Double,
    val price: Double,
    val unit: String = "",
    val discount: Double = 0.0,
    val subtotal: Double = (quantity * price) - discount
)
