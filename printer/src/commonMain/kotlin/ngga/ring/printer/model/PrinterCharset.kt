package ngga.ring.printer.model

/**
 * Common charsets supported by thermal printers.
 */
enum class PrinterCharset(val value: String) {
    UTF8("UTF-8"),
    GBK("GBK"),
    CP437("CP437"),
    ISO8859_1("ISO-8859-1"),
    WINDOWS_1252("WINDOWS-1252"),
    BIG5("BIG5");

    companion object {
        fun fromValue(value: String): PrinterCharset {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: UTF8
        }
    }
}
