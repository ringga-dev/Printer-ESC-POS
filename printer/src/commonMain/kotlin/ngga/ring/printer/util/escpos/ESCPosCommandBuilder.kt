package ngga.ring.printer.util.escpos

import ngga.ring.printer.util.platform.encodeString
import ngga.ring.printer.util.preview.PreviewBlock

/**
 * A pure Kotlin, KMP-friendly ESC/POS command builder.
 *
 * - Works in commonMain (Kotlin Multiplatform)
 * - Avoids all java.* dependencies
 * - Produces ESC/POS raw byte commands for any thermal receipt printer
 * - API is intentionally similar to modern printer builders for easier migration
 */
class ESCPosCommandBuilder(
    val config: ESCPosConfig = ESCPosConfig()
) {
    companion object {
        fun fromPrinterConfig(config: ngga.ring.printer.model.PrinterConfig): ESCPosCommandBuilder {
            val dots = if (config.paperWidthDots > 0) {
                config.paperWidthDots
            } else {
                // Heuristic: (PaperWidth - 10mm margin) * 8 dots/mm
                ((config.paperWidth - 10) * 8).coerceAtLeast(384)
            }
            val dotsPerChar = dots.toDouble() / config.characterPerLine
            val printWidth = if (config.autoCenter) {
                (dots - (2 * config.leftMargin)).coerceAtLeast(1)
            } else {
                dots
            }

            // Adjust characters per line if area is narrowed by auto-center
            val effectiveChars = if (config.autoCenter) {
                (printWidth / dotsPerChar).toInt().coerceAtLeast(1)
            } else {
                config.characterPerLine
            }

            // SMART CALIBRATION: Calculate the gap between printable dots and actual text dots
            // This ensures characters are centered even if charsPerLine < max possible
            val actualTextWidth = effectiveChars * dotsPerChar
            val centeringPadding = if (config.autoCenter) {
                ((printWidth - actualTextWidth) / 2).toInt().coerceAtLeast(0)
            } else {
                0
            }

            return ESCPosCommandBuilder(
                ESCPosConfig(
                    charsPerLine = effectiveChars,
                    paperWidthDots = (actualTextWidth.toInt()), 
                    leftMargin = config.leftMargin + centeringPadding
                )
            ).apply {
                this.hardwareTotalDots = dots
            }
        }
    }

    private val buffer = mutableListOf<Byte>()
    private val previewBlocks = mutableListOf<PreviewBlock>()
    private var hardwareTotalDots = 0
    private var currentWidthMultiplier = 1
    private var currentHeightMultiplier = 1
    private var isBold = false
    private var isUnderline = false
    private var isInverted = false
    private var currentAlignment = TextAlignment.LEFT

    /**
     * Returns all collected ESC/POS bytes as a ByteArray.
     * Typically this is sent to USB/Bluetooth/TCP printers.
     */
    fun build(): ByteArray = buffer.toByteArray()

    /**
     * Returns a logical list of blocks representing the receipt for UI preview.
     */
    fun buildPreview(): List<PreviewBlock> = previewBlocks.toList()

    /* ------------------------------------------------------------
     * High-level text helpers
     * ------------------------------------------------------------ */

    /** Writes a text line followed by a line feed (LF). Applies safety margin to prevent hardware wrapping. */
    fun line(text: String = ""): ESCPosCommandBuilder {
        if (text.isEmpty()) {
            previewBlocks.add(PreviewBlock.Space)
        } else {
            previewBlocks.add(PreviewBlock.Text(
                text = text,
                alignment = currentAlignment,
                isBold = isBold,
                isUnderline = isUnderline,
                isInverted = isInverted,
                widthMultiplier = currentWidthMultiplier,
                heightMultiplier = currentHeightMultiplier
            ))
        }
        val safeMax = (config.charsPerLine / currentWidthMultiplier - 1).coerceAtLeast(1)
        val safeText = if (text.length > safeMax) text.take(safeMax) else text
        writeText(safeText)
        writeLF()
        return this
    }

    /**
     * Prints a row of a table with relative weights.
     * Use this for complex product listings.
     */
    fun tableRow(columns: List<String>, weights: List<Int>): ESCPosCommandBuilder {
        line(ESCPosTextLayout.tableRow(columns, weights, config.charsPerLine / currentWidthMultiplier))
        return this
    }

    /** Writes plain text (no LF). */
    fun text(text: String): ESCPosCommandBuilder {
        writeText(text)
        return this
    }

    /** Writes an empty line separator. */
    fun breakLine(): ESCPosCommandBuilder {
        previewBlocks.add(PreviewBlock.Space)
        writeLF()
        return this
    }

    fun segmentedLine(
        left: String,
        right: String,
        maxCharsPerLine: Int = config.charsPerLine / currentWidthMultiplier
    ): ESCPosCommandBuilder {
        previewBlocks.add(PreviewBlock.KeyValue(left, right, isBold, isInverted))
        writeText(ESCPosTextLayout.segmentedText(left, right, maxCharsPerLine))
        writeLF()
        return this
    }

    /** Centers the given text (uses hardware alignment if possible, otherwise software spaces). */
    fun centerText(text: String): ESCPosCommandBuilder {
        if (currentAlignment == TextAlignment.CENTER) {
            // Hardware alignment is already center
            line(text.trim())
        } else {
            // Use software spaces
            writeText(ESCPosTextLayout.centeredText(text, config.charsPerLine / currentWidthMultiplier))
            writeLF()
        }
        return this
    }

    /** Centers text and splits into multiple lines if needed (wrapping). */
    fun centerWrapped(text: String, maxLine: Int = 3): ESCPosCommandBuilder {
        writeText(ESCPosTextLayout.centerText(
            maxCharsPerLine = config.charsPerLine / currentWidthMultiplier,
            text = text,
            maxLine = maxLine
        ))
        writeLF()
        return this
    }

    /** Prints a divider line using a specific character. Forces left alignment. */
    fun divider(char: Char = '-'): ESCPosCommandBuilder {
        previewBlocks.add(PreviewBlock.Divider(char))
        val prevAlign = currentAlignment
        setAlignment(TextAlignment.LEFT)
        val safeMax = (config.charsPerLine / currentWidthMultiplier - 1).coerceAtLeast(1)
        line(char.toString().repeat(safeMax))
        setAlignment(prevAlign)
        return this
    }

    /** Prints a sub-divider line using a specific character. Forces left alignment. */
    fun subDivider(char: Char = '-'): ESCPosCommandBuilder {
        val prevAlign = currentAlignment
        setAlignment(TextAlignment.LEFT)
        val safeMax = (config.charsPerLine / currentWidthMultiplier - 1).coerceAtLeast(1)
        line(char.toString().repeat(safeMax))
        setAlignment(prevAlign)
        return this
    }

    /**
     * Executes a block with bold mode enabled, then disabled afterwards.
     */
    fun withBold(
        enabled: Boolean = true,
        block: ESCPosCommandBuilder.() -> Unit
    ): ESCPosCommandBuilder {
        if (enabled) boldOn()
        this.block()
        if (enabled) boldOff()
        return this
    }

    /**
     * Executes a block with a temporary text size, then resets to normal.
     * width/height: 1..8 (ESC/POS spec)
     */
    fun withTextSize(
        width: Int,
        height: Int,
        block: ESCPosCommandBuilder.() -> Unit
    ): ESCPosCommandBuilder {
        setTextSize(width, height)
        this.block()
        setTextSize(1, 1)
        return this
    }

    /**
     * Executes a block with a temporary alignment (ESC a n).
     */
    fun withAlignment(
        alignment: TextAlignment,
        block: ESCPosCommandBuilder.() -> Unit
    ): ESCPosCommandBuilder {
        setAlignment(alignment)
        this.block()
        setAlignment(TextAlignment.LEFT)
        return this
    }

    /** Feeds N empty lines. */
    fun feed(lines: Int = 1): ESCPosCommandBuilder {
        repeat(lines.coerceAtLeast(0)) { 
            previewBlocks.add(PreviewBlock.Space)
            writeLF() 
        }
        return this
    }

    /* ------------------------------------------------------------
     * Graphics, Barcodes, and QR Codes
     * ------------------------------------------------------------ */

    /**
     * Prints a raster bit image (GS v 0).
     * 
     * @param bytes Packaged bitonal bytes (8 pixels per byte).
     * @param width Width in pixels.
     * @param height Height in pixels.
     */
    fun image(
        bytes: ByteArray, 
        width: Int, 
        height: Int,
        center: Boolean = false
    ): ESCPosCommandBuilder {
        if (center) alignCenter()
        previewBlocks.add(PreviewBlock.Image(width, height, currentAlignment))
        
        val widthBytes = (width + 7) / 8
        val xL = widthBytes % 256
        val xH = widthBytes / 256
        val yL = height % 256
        val yH = height / 256

        writeRaw(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH)
        writeBytes(bytes)
        
        if (center) alignLeft()
        return this
    }

    /**
     * Prints a Barcode (GS k).
     *
     * @param data Barcode content string.
     * @param type Barcode system (Default 73 = CODE128).
     * @param height Barcode height in dots (Default 162).
     * @param width Barcode width multiplier 2..6 (Default 3).
     */
    fun barcode(
        data: String,
        height: Int = 162,
        width: Int = 3,
        center: Boolean = false
    ): ESCPosCommandBuilder {
        if (center) alignCenter()
        previewBlocks.add(PreviewBlock.Barcode(data, currentAlignment))
        
        // Set height
        writeRaw(0x1D, 0x68, height.coerceIn(1, 255))
        // Set width
        writeRaw(0x1D, 0x77, width.coerceIn(2, 6))
        
        // Print barcode (System B: 1D 6B 49 n d1...dn)
        val bytes = data.encodeToByteArray()
        writeRaw(0x1D, 0x6B, 73, bytes.size)
        writeBytes(bytes)
        
        if (center) alignLeft()
        return this
    }

    /**
     * Prints a QR Code using the standard native sequence (GS ( k).
     * Strictly follows the 5-step hardware sequence.
     *
     * @param data QR code content.
     * @param size Module size 1..16 (Default 8).
     * @param center Whether to center the QR code.
     */
    fun qrCodeNative(data: String, size: Int = 8, center: Boolean = false): ESCPosCommandBuilder {
        if (center) alignCenter()
        previewBlocks.add(PreviewBlock.QRCode(data, currentAlignment))

        val bytes = data.encodeToByteArray()
        val numBytes = bytes.size + 3
        val pL = numBytes % 256
        val pH = numBytes / 256

        // 1. Tentukan Model (1D 28 6B 04 00 31 41 n1 n2)
        // n1=50 (Model 2), n2=0
        writeRaw(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00)

        // 2. Tentukan Ukuran Module (1D 28 6B 03 00 31 43 n)
        writeRaw(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, size.coerceIn(1, 16))

        // 3. Tentukan Error Correction (1D 28 6B 03 00 31 45 n)
        // n=48 (Level L)
        writeRaw(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 48)

        // 4. Simpan Data ke Memory (1D 28 6B pL pH 31 50 30 d1...dk)
        writeRaw(0x1D, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30)
        writeBytes(bytes)

        // 5. Cetak QR (1D 28 6B 03 00 31 51 30)
        writeRaw(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30)

        if (center) alignLeft()
        return this
    }

    /** Alias for qrCodeNative to maintain backward compatibility. */
    fun qrCode(data: String, size: Int = 8, center: Boolean = false): ESCPosCommandBuilder {
        return qrCodeNative(data, size, center)
    }

    /* ------------------------------------------------------------
     * STATEFUL STYLING (for easier migration)
     * ------------------------------------------------------------ */

    fun alignCenter(): ESCPosCommandBuilder {
        setAlignment(TextAlignment.CENTER)
        return this
    }

    fun alignLeft(): ESCPosCommandBuilder {
        setAlignment(TextAlignment.LEFT)
        return this
    }

    fun alignRight(): ESCPosCommandBuilder {
        setAlignment(TextAlignment.RIGHT)
        return this
    }

    fun bold(enabled: Boolean): ESCPosCommandBuilder {
        this.isBold = enabled
        if (enabled) boldOn() else boldOff()
        return this
    }

    fun underline(enabled: Boolean): ESCPosCommandBuilder {
        this.isUnderline = enabled
        if (enabled) underlineOn() else underlineOff()
        return this
    }

    fun invert(enabled: Boolean): ESCPosCommandBuilder {
        this.isInverted = enabled
        if (enabled) invertOn() else invertOff()
        return this
    }

    fun bigFont(): ESCPosCommandBuilder {
        setTextSize(2, 2)
        return this
    }

    fun normalFont(): ESCPosCommandBuilder {
        setTextSize(1, 1)
        return this
    }

    fun feedLines(lines: Int): ESCPosCommandBuilder = feed(lines)

    /**
     * Prints a calibration ruler to help identify the hardware's printable area in dots.
     * Output format: 0    50   100  150...
     *                |....|....|....|....
     */
    fun printRuler(): ESCPosCommandBuilder {
        val totalDots = if (hardwareTotalDots > 0) hardwareTotalDots else config.paperWidthDots
        line("HARDWARE CALIBRATION RULER")
        line("-".repeat(config.charsPerLine))
        
        // Numbers row (every 50 dots)
        val numbers = StringBuilder()
        for (i in 0..totalDots step 50) {
            numbers.append(i.toString().padEnd(5))
        }
        line(numbers.toString())
        
        // Ticks row (every 10 dots)
        val ticks = StringBuilder()
        for (i in 0..totalDots step 10) {
            ticks.append(if (i % 50 == 0) "|" else ".")
        }
        line(ticks.toString())
        
        line("-".repeat(config.charsPerLine))
        line("Max Dots Configured: $totalDots")
        feed(3)
        return this
    }

    /** Sends the ESC @ command (printer reset/initialize). */
    fun initialize(): ESCPosCommandBuilder {
        writeRaw(0x1B, 0x40) // Reset
        setLeftMargin(config.leftMargin)
        setPrintableAreaWidth(config.paperWidthDots) // Align hardware to paper size
        return this
    }

    /**
     * Sets the left margin of the printable area. (GS L nL nH)
     * n = nL + nH * 256
     */
    fun setLeftMargin(dots: Int): ESCPosCommandBuilder {
        val nL = dots % 256
        val nH = dots / 256
        writeRaw(0x1D, 0x4C, nL, nH)
        return this
    }

    /**
     * Sets the width of the printable area. (GS W nL nH)
     * n = nL + nH * 256
     */
    fun setPrintableAreaWidth(dots: Int): ESCPosCommandBuilder {
        val nL = dots % 256
        val nH = dots / 256
        writeRaw(0x1D, 0x57, nL, nH)
        return this
    }

    /**
     * Cuts the paper (full or partial, if supported).
     */
    fun cut(full: Boolean = true): ESCPosCommandBuilder {
        val mode: Int = if (full) 0x00 else 0x01
        writeRaw(0x1D, 0x56, mode)
        return this
    }

    /**
     * Pulse command to kick the cash drawer.
     * Usually pin 2 or pin 5 (Default pin 2).
     */
    fun openCashDrawer(pin: Int = 0): ESCPosCommandBuilder {
        val p = if (pin == 0) 0x00 else 0x01
        writeRaw(0x1B, 0x70, p, 0x32, 0xFF)
        return this
    }

    /**
     * Sets line spacing in dots (n/180 inch or n/203 inch depending on printer).
     * ESC 3 n
     */
    fun setLineSpacing(dots: Int): ESCPosCommandBuilder {
        writeRaw(0x1B, 0x33, dots.coerceIn(0, 255))
        return this
    }

    /**
     * Resets line spacing to default (1/6 inch).
     * ESC 2
     */
    fun resetLineSpacing(): ESCPosCommandBuilder {
        writeRaw(0x1B, 0x32)
        return this
    }

    /**
     * Selects a code page for the printer (ESC t n).
     *
     * @param page Code page index (check your printer's manual, e.g., 0x00 for PC437, 0x10 for WPC1252).
     */
    fun selectCodePage(page: Byte): ESCPosCommandBuilder {
        writeRaw(0x1B, 0x74, page.toInt())
        return this
    }

    /* ------------------------------------------------------------
     * Low-level ESC/POS byte operations
     * ------------------------------------------------------------ */

    fun writeRaw(vararg bytes: Int): ESCPosCommandBuilder {
        bytes.forEach { buffer.add(it.toByte()) }
        return this
    }

    fun writeBytes(bytes: ByteArray): ESCPosCommandBuilder {
        buffer.addAll(bytes.asList())
        return this
    }

    private fun writeText(text: String) {
        if (text.isNotEmpty()) {
            val bytes = ngga.ring.printer.util.platform.encodeString(text, config.charset)
            writeBytes(bytes)
        }
    }

    private fun writeLF() {
        buffer.add(0x0A) // Line feed
    }

    /* ------------------------------------------------------------
     * ESC/POS Styling (Internal)
     * ------------------------------------------------------------ */

    private fun boldOn() = writeRaw(0x1B, 0x45, 0x01)
    private fun boldOff() = writeRaw(0x1B, 0x45, 0x00)

    private fun underlineOn() = writeRaw(0x1B, 0x2D, 0x01)
    private fun underlineOff() = writeRaw(0x1B, 0x2D, 0x00)

    private fun invertOn() = writeRaw(0x1D, 0x42, 0x01)
    private fun invertOff() = writeRaw(0x1D, 0x42, 0x00)

    fun setAlignment(align: TextAlignment): ESCPosCommandBuilder {
        this.currentAlignment = align
        val mode = when (align) {
            TextAlignment.LEFT -> 0
            TextAlignment.CENTER -> 1
            TextAlignment.RIGHT -> 2
        }
        writeRaw(0x1B, 0x61, mode)
        return this
    }

    private fun setTextSize(width: Int, height: Int) {
        val w = width.coerceIn(1, 8) - 1
        val h = height.coerceIn(1, 8) - 1
        currentWidthMultiplier = width.coerceIn(1, 8)
        currentHeightMultiplier = height.coerceIn(1, 8)
        val size = (w shl 4) or h
        writeRaw(0x1D, 0x21, size)
        
        // RE-APPLY ALIGNMENT:
        // Some printers reset alignment to LEFT when GS ! n (set size) is called.
        // We re-send the current alignment command to ensure consistency.
        val alignMode = when (currentAlignment) {
            TextAlignment.LEFT -> 0
            TextAlignment.CENTER -> 1
            TextAlignment.RIGHT -> 2
        }
        writeRaw(0x1B, 0x61, alignMode)
    }
}

/**
 * ESC/POS alignment modes.
 */
enum class TextAlignment {
    LEFT,
    CENTER,
    RIGHT
}
