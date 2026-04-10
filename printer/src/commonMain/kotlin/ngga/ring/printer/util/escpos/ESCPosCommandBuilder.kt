package ngga.ring.printer.util.escpos

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

    private val buffer = mutableListOf<Byte>()
    private var currentWidthMultiplier = 1
    private var currentAlignment = TextAlignment.LEFT

    /**
     * Returns all collected ESC/POS bytes as a ByteArray.
     * Typically this is sent to USB/Bluetooth/TCP printers.
     */
    fun build(): ByteArray = buffer.toByteArray()

    /* ------------------------------------------------------------
     * High-level text helpers
     * ------------------------------------------------------------ */

    /** Writes a text line followed by a line feed (LF). */
    fun line(text: String = ""): ESCPosCommandBuilder {
        writeText(text)
        writeLF()
        return this
    }

    /** Writes plain text (no LF). */
    fun text(text: String): ESCPosCommandBuilder {
        writeText(text)
        return this
    }

    /** Writes an empty line separator. */
    fun breakLine(): ESCPosCommandBuilder = line()

    fun segmentedLine(
        left: String,
        right: String,
        maxCharsPerLine: Int = config.charsPerLine / currentWidthMultiplier
    ): ESCPosCommandBuilder {
        writeText(ESCPosTextLayout.segmentedText(left, right, maxCharsPerLine))
        writeLF()
        return this
    }

    /** Centers the given text (string only, no ESC alignment). */
    fun centerText(text: String): ESCPosCommandBuilder {
        writeText(ESCPosTextLayout.centeredText(text, config.charsPerLine / currentWidthMultiplier))
        writeLF()
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

    /** Prints a divider line using a specific character. */
    fun divider(char: Char = '-'): ESCPosCommandBuilder {
        line(char.toString().repeat(config.charsPerLine / currentWidthMultiplier))
        return this
    }

    /** Prints a sub-divider line using a specific character. */
    fun subDivider(char: Char = '-'): ESCPosCommandBuilder {
        line(char.toString().repeat(config.charsPerLine / currentWidthMultiplier))
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
        repeat(lines.coerceAtLeast(0)) { writeLF() }
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
    fun image(bytes: ByteArray, width: Int, height: Int): ESCPosCommandBuilder {
        val widthBytes = (width + 7) / 8
        val xL = widthBytes % 256
        val xH = widthBytes / 256
        val yL = height % 256
        val yH = height / 256

        writeRaw(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH)
        writeBytes(bytes)
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
        type: Int = 73,
        height: Int = 162,
        width: Int = 3
    ): ESCPosCommandBuilder {
        // Set height
        writeRaw(0x1D, 0x68, height.coerceIn(1, 255))
        // Set width
        writeRaw(0x1D, 0x77, width.coerceIn(2, 6))
        
        // Print barcode (System B)
        val bytes = data.encodeToByteArray()
        writeRaw(0x1D, 0x6B, type, bytes.size)
        writeBytes(bytes)
        return this
    }

    /**
     * Prints a QR Code using the standard function sequence (GS ( k).
     * 
     * @param data QR code content.
     * @param size Module size 1..16 (Default 8).
     */
    fun qrCode(data: String, size: Int = 8): ESCPosCommandBuilder {
        val bytes = data.encodeToByteArray()
        val numBytes = bytes.size + 3
        val pL = numBytes % 256
        val pH = numBytes / 256

        // 1. Set model (Model 2)
        writeRaw(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00)
        
        // 2. Set module size
        writeRaw(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, size.coerceIn(1, 16))
        
        // 3. Set error correction (Level L = 48, M = 49, Q = 50, H = 51)
        writeRaw(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 48)

        // 4. Store data
        writeRaw(0x1D, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30)
        writeBytes(bytes)

        // 5. Print
        writeRaw(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30)
        
        return this
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
        if (enabled) boldOn() else boldOff()
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

    /** Sends the ESC @ command (printer reset/initialize). */
    fun initialize(): ESCPosCommandBuilder {
        writeRaw(0x1B, 0x40)
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
        if (text.isNotEmpty())
            writeBytes(text.encodeToByteArray())
    }

    private fun writeLF() {
        buffer.add(0x0A) // Line feed
    }

    /* ------------------------------------------------------------
     * ESC/POS Styling (Internal)
     * ------------------------------------------------------------ */

    private fun boldOn() = writeRaw(0x1B, 0x45, 0x01)
    private fun boldOff() = writeRaw(0x1B, 0x45, 0x00)

    private fun setAlignment(align: TextAlignment) {
        this.currentAlignment = align
        val mode = when (align) {
            TextAlignment.LEFT -> 0
            TextAlignment.CENTER -> 1
            TextAlignment.RIGHT -> 2
        }
        writeRaw(0x1B, 0x61, mode)
    }

    private fun setTextSize(width: Int, height: Int) {
        val w = width.coerceIn(1, 8) - 1
        val h = height.coerceIn(1, 8) - 1
        currentWidthMultiplier = width.coerceIn(1, 8)
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
