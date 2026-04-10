package ngga.ring.printer.util.escpos

/**
 * Helper utilities for formatting plain text for ESC/POS printers.
 * Works entirely with pure Kotlin String manipulation to remain KMP-safe.
 */
object ESCPosTextLayout {

    /**
     * Produces a single-line text segment with a left label and a right value.
     * Example:
     *   max = 32
     *   left = "Subtotal"
     *   right = "10.000"
     * Output:
     *   "Subtotal                10.000"
     *
     * If [left] is too long, it will be truncated and suffixed with "...".
     */
    /**
     * Produces a single-line text segment with a left label and a right value.
     * Handles wrapping/truncation gracefully to ensure the right value is ALWAYS visible.
     */
    fun segmentedText(
        left: String,
        right: String,
        maxCharsPerLine: Int
    ): String {
        if (maxCharsPerLine <= 0) return "$left $right"

        val rightPart = " $right"
        val maxLeft = (maxCharsPerLine - rightPart.length).coerceAtLeast(0)
        
        val trimmedLeft = if (left.length > maxLeft) {
            if (maxLeft > 3) left.take(maxLeft - 3) + "..."
            else left.take(maxLeft)
        } else {
            left
        }

        val spaces = (maxCharsPerLine - trimmedLeft.length - right.length)
            .coerceAtLeast(1)

        return buildString {
            append(trimmedLeft)
            repeat(spaces) { append(' ') }
            append(right)
        }
    }

    /**
     * Centers a text string into the given line width.
     */
    fun centeredText(
        text: String,
        maxCharsPerLine: Int
    ): String {
        if (maxCharsPerLine <= 0) return text
        if (text.length >= maxCharsPerLine) return text.take(maxCharsPerLine)

        val totalPadding = maxCharsPerLine - text.length
        val leftPad = totalPadding / 2
        val rightPad = totalPadding - leftPad

        return " ".repeat(leftPad) + text + " ".repeat(rightPad)
    }

    /**
     * Centers text and splits into multiple lines if needed (wrapping).
     */
    fun centerText(maxCharsPerLine: Int, text: String, maxLine: Int = 5): String {
        if (text.isBlank()) return ""
        
        val chunks = text.chunked(maxCharsPerLine)
        val finalLines = if (chunks.size > maxLine) {
            chunks.take(maxLine - 1) + (chunks[maxLine - 1].take((maxCharsPerLine - 3).coerceAtLeast(0)) + "...")
        } else {
            chunks
        }

        return finalLines.joinToString("\n") { line ->
            centeredText(line.trim(), maxCharsPerLine)
        }
    }
}
