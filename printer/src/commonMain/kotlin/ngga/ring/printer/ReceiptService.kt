package ngga.ring.printer

import ngga.ring.printer.util.escpos.ESCPosCommandBuilder
import ngga.ring.printer.util.escpos.TextAlignment
import ngga.ring.data.model.TransactionEntity
import ngga.ring.data.model.TransactionItemEntity
import ngga.ring.data.model.BusinessProfileEntity
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
        business: BusinessProfileEntity?,
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>,
        role: String = "KASIR", // KASIR, DAPUR, STAN, LAUNDRY
        laundryOrder: ngga.ring.data.model.LaundryOrderEntity? = null,
        laundryItems: List<ngga.ring.data.model.LaundryItemEntity> = emptyList()
    ): ByteArray {
        val builder = ESCPosCommandBuilder().initialize()
        
        when (role) {
            "DAPUR", "STAN" -> generateKitchenSlip(builder, transaction, items, role)
            "LAUNDRY" -> if (laundryOrder != null) {
                generateLaundryReceipt(builder, business, laundryOrder, laundryItems)
            } else {
                generateFullReceipt(builder, business, transaction, items)
            }
            else -> generateFullReceipt(builder, business, transaction, items)
        }
        
        return builder.feedLines(3).cut().build()
    }

    private fun generateLaundryReceipt(
        builder: ESCPosCommandBuilder,
        business: BusinessProfileEntity?,
        order: ngga.ring.data.model.LaundryOrderEntity,
        items: List<ngga.ring.data.model.LaundryItemEntity>
    ) {
        val symbol = business?.currencySymbol ?: "Rp"

        builder.alignCenter()
            .bold(true)
            .bigFont()
            .line(business?.name ?: "LAUNDRY Q")
            .normalFont()
            .bold(false)
            .line(business?.address ?: "")
            .line("Telp: ${business?.phone ?: "-"}")
            .divider('=')
            
        builder.alignCenter()
            .bold(true)
            .line("STRUK LAUNDRY")
            .bold(false)
            .divider('-')

        builder.alignLeft()
            .line("No Order : ${order.orderCode}")
            .line("Pelanggan: ${order.customerName}")
            .line("Telepon  : ${order.customerPhone}")
            .line("Tanggal  : ${formatTimestamp(order.createdAt)}")
            .divider('-')

        builder.alignCenter()
            .bold(true)
            .line("LAYANAN: ${order.serviceType}")
            .bold(false)
            .divider('-')

        builder.alignLeft()
        items.forEach { item ->
            builder.line(item.itemName)
            val qtyDetail = "${item.quantity} ${item.unit} x ${item.price.toLong()}"
            builder.segmentedLine(qtyDetail, "$symbol ${item.subtotal.toLong()}")
        }

        builder.divider('-')
            .alignRight()
            .line("Total Berat: ${order.totalWeight} Kg")
            .bold(true)
            .line("TOTAL HARGA: $symbol ${items.sumOf { it.subtotal }.toLong()}")
            .bold(false)
            .divider('-')

        builder.alignLeft()
            .line("Estimasi Selesai:")
            .bold(true)
            .line(order.estimatedFinish?.let { formatTimestamp(it) } ?: "-")
            .bold(false)
            .divider('-')

        if (!order.notes.isNullOrBlank()) {
            builder.line("Catatan:")
                .line(order.notes ?: "")
                .divider('-')
        }

        builder.alignCenter()
            .feedLines(1)
            .line("SYARAT & KETENTUAN:")
            .line("1. Pengambilan harus bawa struk")
            .line("2. Komplain max 24 jam")
            .line("3. Barang hilang ganti 2x ongkos")
            .feedLines(1)
            .line("TERIMA KASIH")
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
        business: BusinessProfileEntity?,
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ) {
        builder.alignCenter()
            .bold(true)
            .bigFont()
            .line(business?.name ?: "LAUNDRY Q")
            .normalFont()
            .bold(false)
            .line(business?.address ?: "")
            .line("Telp: ${business?.phone ?: "-"}")
            .divider('-')
            
        builder.alignLeft()
            .line("No: ${transaction.transactionCode}")
            .line("Cust: ${transaction.customerName ?: "Umum"}")
            .line("Tgl: ${formatTimestamp(transaction.timestamp)}")
            .divider('-')

        val symbol = business?.currencySymbol ?: "Rp"
        
        items.forEach { item ->
            builder.line(item.itemName)
            val priceDetail = "${item.quantity.toLong()} x ${item.priceAtTime.toLong()}"
            val subtotal = (item.quantity * item.priceAtTime).toLong().toString()
            
            // Using modern segmented line for perfect alignment
            builder.segmentedLine(priceDetail, "$symbol $subtotal")
        }

        builder.divider('-')
            .alignRight()
            .bold(true)
            .line("TOTAL: $symbol ${transaction.totalAmount.toLong()}")
            .bold(false)
            .divider('-')
            .alignCenter()
            .feedLines(1)
            .line("Terima Kasih")
            .line("Sudah Mempercayai Kami")
    }

    private fun generateKitchenSlip(
        builder: ESCPosCommandBuilder,
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>,
        role: String
    ) {
        builder.alignCenter()
            .bold(true)
            .bigFont()
            .line(if (role == "DAPUR") "ORDER DAPUR" else "ORDER STAND")
            .normalFont()
            .divider('-')
            .alignLeft()
            .line("Kode: ${transaction.transactionCode}")
            .line("Cust: ${transaction.customerName ?: "Umum"}")
            .divider('-')
            .bold(true)
            
        items.forEach { item ->
            builder.line("${item.quantity.toLong()} x ${item.itemName}")
        }
        
        builder.bold(false)
            .divider('-')
            .alignCenter()
            .line("Printed at: ${formatTimestamp(Clock.System.now().toEpochMilliseconds())}")
    }
}
