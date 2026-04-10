package ngga.ring.printer.util.escpos

/**
 * Configuration for ESC/POS command generation.
 */
data class ESCPosConfig(
    /** Maximum characters per line for the printer (e.g., 32 for 58mm, 42 for 80mm). */
    val charsPerLine: Int = 32,
    
    /** Charset for encoding text. ESC/POS typically uses CP437, but UTF-8 is often supported. */
    val charset: String = "UTF-8"
)
