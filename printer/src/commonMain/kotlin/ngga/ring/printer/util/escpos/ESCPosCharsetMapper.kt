package ngga.ring.printer.util.escpos

/**
 * Maps common charsets to ESC/POS hardware code page indices (ESC t n).
 * This allows the library to automatically switch the printer's encoding 
 * based on the text being printed.
 */
object ESCPosCharsetMapper {
    /**
     * Common ESC/POS Code Pages.
     * Note: Exact indices can vary slightly by printer brand (Epson vs Chinese brands).
     */
    val CODE_PAGES = mapOf(
        "US" to 0x00,           // PC437 (USA)
        "MULTILINGUAL" to 0x02, // PC850 (Multilingual)
        "LATIN1" to 0x10,       // WPC1252 (Windows Latin 1)
        "LATIN2" to 0x12,       // PC852 (Latin 2)
        "EURO" to 0x13,         // PC858 (Euro)
        "THAI" to 0x1A,         // PC874 (Thai)
        "GREEK" to 0x11,        // PC851 (Greek)
        "HEBREW" to 0x15,       // PC862 (Hebrew)
        "ARABIC" to 0x16,       // PC864 (Arabic)
        "VIETNAMESE" to 0x1E    // TC1258 (Vietnamese)
    )

    /**
     * Heuristic to determine the best code page for a given string.
     */
    fun getBestCodePage(text: String): Byte {
        // Very basic check: if it contains Thai characters
        if (text.any { it in '\u0E00'..'\u0E7F' }) return 26 // Thai
        
        // If it contains Arabic
        if (text.any { it in '\u0600'..'\u06FF' }) return 22 // Arabic
        
        // Default to WPC1252 (Latin 1) as it covers most Western European languages
        return 0x10 
    }
}
