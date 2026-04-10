package ngga.ring.printer

import ngga.ring.printer.util.escpos.ESCPosCommandBuilder
import ngga.ring.printer.model.*
import kotlin.time.Clock
import kotlinx.datetime.*

/**
 * Service for generating thermal printer receipt bytes.
 * Uses ESC/POS command builder for high compatibility across devices.
 */
class ReceiptService {

    /**
     * Entry point for receipt generation based on the user's role and data.
     */
    fun generateReceipt(
        business: BusinessInfo?,
        data: ReceiptData,
        role: String = "KASIR"
    ): ByteArray {
        val builder = ESCPosCommandBuilder().initialize()
        
        when (role) {
            "DAPUR", "STAN" -> generateKitchenSlip(builder, data, role)
            else -> generateFullReceipt(builder, business, data)
        }
        
        return builder.feedLines(3).cut().build()
    }

    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val instant = Instant.fromEpochMilliseconds(timestamp)
            val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val day = ldt.dayOfMonth.toString().padStart(2, '0')
            val month = ldt.monthNumber.toString().padStart(2, '0')
            val year = ldt.year
            val hour = ldt.hour.toString().padStart(2, '0')
            val minute = ldt.minute.toString().padStart(2, '0')
            "$day-$month-$year $hour:$minute"
        } catch (e: Exception) {
            "Tgl: $timestamp"
        }
    }

    private fun generateFullReceipt(
        builder: ESCPosCommandBuilder,
        business: BusinessInfo?,
        data: ReceiptData
    ) {
        val symbol = business?.currencySymbol ?: "Rp"

        // HEADER
        builder.alignCenter()
            .bold(true)
            .bigFont()
            .line(business?.name ?: "RECEIPT")
            .normalFont()
            .bold(false)
            .line(business?.address ?: "")
            if (!business?.phone.isNullOrBlank()) builder.line("Telp: ${business?.phone}")
            if (!business?.taxId.isNullOrBlank()) builder.line("NPWP: ${business?.taxId}")
            builder.divider('-')
            
        // TRANSACTION INFO
        builder.alignLeft()
            .line("No Ref: ${data.headerId}")
            if (!data.transactionId.isNullOrBlank()) builder.line("Trans ID: ${data.transactionId}")
            builder.line("Cust  : ${data.customerName ?: "Umum"}")
            builder.line("Tgl   : ${formatTimestamp(data.timestamp)}")
            builder.divider('-')

        // ITEMS
        data.items.forEach { item ->
            builder.line(item.name)
            val priceDetail = "${item.quantity.toLong()} x ${item.price.toLong()}"
            val subtotal = item.subtotal.toLong().toString()
            builder.segmentedLine(priceDetail, "$symbol $subtotal")
            if (item.discount > 0) {
                builder.alignRight().line("Disc: -$symbol ${item.discount.toLong()}").alignLeft()
            }
        }
        builder.divider('-')

        // SUMMARY (The "Professional" Breakdown)
        builder.alignRight()
            if (data.subtotal > 0) builder.segmentedLine("Subtotal", "$symbol ${data.subtotal.toLong()}")
            if (data.taxAmount > 0) builder.segmentedLine("Pajak (VAT)", "$symbol ${data.taxAmount.toLong()}")
            if (data.discountAmount > 0) builder.segmentedLine("Potongan", "-$symbol ${data.discountAmount.toLong()}")
            builder.bold(true)
                .segmentedLine("TOTAL", "$symbol ${data.totalAmount.toLong()}")
                .bold(false)
            
        if (data.amountPaid > 0) {
            builder.divider('.')
                .segmentedLine("Bayar (${data.paymentMethod ?: "-"})", "$symbol ${data.amountPaid.toLong()}")
                .segmentedLine("Kembali", "$symbol ${data.amountReturn.toLong()}")
        }
        builder.divider('-')

        // FOOTER & VERIFICATION
        if (!data.notes.isNullOrBlank()) {
            builder.alignLeft().line("Notes: ${data.notes}").divider('-')
        }

        builder.alignCenter()
            .feedLines(1)
            .line(data.footerMessage ?: "Terima Kasih Atas Kunjungan Anda")
            
        // QR Code for verification
        if (!data.verificationUrl.isNullOrBlank()) {
            builder.feed(1).qrCode(data.verificationUrl, size = 6).feed(1)
                .line("Scan to Verify")
        }
        
        // Barcode for transaction
        if (!data.transactionId.isNullOrBlank()) {
            builder.feed(1).barcode(data.transactionId).feed(1)
        }
    }

    private fun generateKitchenSlip(
        builder: ESCPosCommandBuilder,
        data: ReceiptData,
        role: String
    ) {
        builder.alignCenter()
            .bold(true)
            .bigFont()
            .line(if (role == "DAPUR") "ORDER DAPUR" else "ORDER STAND")
            .normalFont()
            .divider('-')
            .alignLeft()
            .line("Kode : ${data.headerId}")
            .line("Cust : ${data.customerName ?: "Umum"}")
            builder.divider('-')
            .bold(true)
            
        data.items.forEach { item ->
            builder.line("${item.quantity.toLong()} x ${item.name}")
            if (!data.notes.isNullOrBlank()) builder.line("  * ${data.notes}")
        }
        
        builder.bold(false)
            .divider('-')
            .alignCenter()
            .line("Printed at: ${formatTimestamp(Clock.System.now().toEpochMilliseconds())}")
    }
}
