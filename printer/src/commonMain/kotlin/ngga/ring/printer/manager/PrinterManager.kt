package ngga.ring.printer.manager

import ngga.ring.data.model.PrinterConfigEntity
import ngga.ring.printer.ReceiptService
import ngga.ring.printer.helper.PrinterBluetoothHelper
import ngga.ring.printer.helper.PrinterTcpHelper
import ngga.ring.printer.helper.PrinterUsbHelper
import ngga.ring.printer.util.escpos.ESCPosCommandBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A compatibility facade that bridges the existing ViewModels to the new KMP helpers.
 */
class PrinterManager(
    private val receiptService: ReceiptService,
    private val bluetoothHelper: PrinterBluetoothHelper,
    private val tcpHelper: PrinterTcpHelper,
    private val usbHelper: PrinterUsbHelper
) {

    suspend fun testPrint(config: PrinterConfigEntity): Boolean = withContext(Dispatchers.Default) {
        val escConfig = ngga.ring.printer.util.escpos.ESCPosConfig(
            charsPerLine = config.characterPerLine
        )
        
        val testData = ESCPosCommandBuilder(escConfig)
            .initialize()
            
            // EDGE RULER: This proves the width adjustment (32 vs 48)
            .alignLeft()
            .line("[1]${"-".repeat((config.characterPerLine - 6).coerceAtLeast(0))}[${config.characterPerLine}]")
            
            .alignCenter()
            // Header
            .withTextSize(2, 2) {
                line("LAUNDRY Q")
            }
            .line("Clean & Fresh Everyday")
            .divider('-')
            
            // Meta Info
            .alignLeft()
            .segmentedLine("Order ID:", "#LQ-20240402")
            .segmentedLine("Date:", "02 Apr 15:45")
            .divider('-')
            
            // Items
            .segmentedLine("Cuci Kiloan x 2kg", "20.000")
            .segmentedLine("Setrika Aja x 1pc", "5.000")
            .segmentedLine("Parfum Premium", "2.000")
            .subDivider('.')
            
            // Total Section (Double Width)
            .withTextSize(2, 1) {
                segmentedLine("TOTAL", "27.000")
            }
            .divider('-')
            
            // Footer
            .alignCenter()
            .line("Thank you for your trust!")
            .line("Actual Settings:")
            .line("Paper: ${config.paperWidth}mm | Density: ${config.characterPerLine} CPL")
            .feedLines(3)
            .cut()
            .build()
            
        print(config, testData)
    }

    suspend fun print(config: PrinterConfigEntity, data: ByteArray): Boolean {
        var attempts = 0
        val maxAttempts = 3
        var success = false
        
        while (attempts < maxAttempts && !success) {
            attempts++
            success = try {
                when (config.connectionType) {
                    "BLUETOOTH" -> bluetoothHelper.print(config.macAddress.orEmpty(), data)
                    "NETWORK" -> tcpHelper.print(config.ipAddress.orEmpty(), config.port, data)
                    "USB" -> {
                        val parts = config.macAddress?.split(":")
                        val vid = parts?.getOrNull(0)?.toIntOrNull() ?: 0
                        val pid = parts?.getOrNull(1)?.toIntOrNull() ?: 0
                        usbHelper.print(vid, pid, data)
                    }
                    else -> false
                }
            } catch (e: Exception) {
                false
            }
            
            if (!success && attempts < maxAttempts) {
                kotlinx.coroutines.delay(1000) // Wait 1s before retry
            }
        }
        
        return success
    }
}
