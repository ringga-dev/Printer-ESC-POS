package ngga.ring.printer.escpos

object EscPosCommands {
    val INIT = byteArrayOf(0x1B, 0x40)
    
    val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x30)
    val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x31)
    val ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x32)
    
    val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    
    val FONT_NORMAL = byteArrayOf(0x1D, 0x21, 0x00)
    val FONT_DOUBLE_HEIGHT = byteArrayOf(0x1D, 0x21, 0x01)
    val FONT_DOUBLE_WIDTH = byteArrayOf(0x1D, 0x21, 0x10)
    val FONT_BIG = byteArrayOf(0x1D, 0x21, 0x11)
    
    val FEED_PAPER_AND_CUT = byteArrayOf(0x1D, 0x56, 0x42, 0x00)
    val FEED_LINE = byteArrayOf(0x0A)
    
    fun feedLines(lines: Int): ByteArray {
        return byteArrayOf(0x1B, 0x64, lines.toByte())
    }
}
