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
    fun segmentedText(
        left: String,
        right: String,
        maxCharsPerLine: Int
    ): String {
        if (maxCharsPerLine <= 0) return "$left $right"

        val maxLeft = (maxCharsPerLine - right.length - 1).coerceAtLeast(0)
        val trimmedLeft = when {
            maxLeft <= 0 -> ""
            left.length > maxLeft ->
                if (maxLeft > 3) left.take(maxLeft - 3) + "..."
                else left.take(maxLeft)
            else -> left
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
     * Centers a text string into the given line width by applying left/right spacing.
     * If the text is longer than [maxCharsPerLine], it will be truncated.
     */
    fun centeredText(
        text: String,
        maxCharsPerLine: Int
    ): String {
        if (maxCharsPerLine <= 0) return text

        val clipped = if (text.length > maxCharsPerLine) {
            text.take(maxCharsPerLine)
        } else text

        val totalPadding = maxCharsPerLine - clipped.length
        val leftPad = totalPadding / 2
        val rightPad = totalPadding - leftPad

        return buildString {
            repeat(leftPad) { append(' ') }
            append(clipped)
            repeat(rightPad) { append(' ') }
        }
    }

    /**
     * Centers text and splits into multiple lines if needed (wrapping).
     */
    fun centerText(maxCharsPerLine: Int, text: String, maxLine: Int = 3): String {
        val chunks = text.chunked(maxCharsPerLine)
        val finalLines = mutableListOf<String>()
        if (chunks.size > maxLine) {
            finalLines.add(chunks[0])
            finalLines.add(chunks[1])
            val startIndex = 2 * maxCharsPerLine
            val charCountToTake = (maxCharsPerLine - 3).coerceAtLeast(0)
            val thirdLineContent = if (startIndex < text.length) {
                text.substring(startIndex).take(charCountToTake)
            } else {
                ""
            }
            finalLines.add("$thirdLineContent...")
        } else {
            finalLines.addAll(chunks)
        }

        return finalLines.joinToString("\n") { line ->
            centerString(line, maxCharsPerLine)
        }
    }

    private fun centerString(s: String, width: Int): String {
        if (s.length >= width) return s

        val padding = width - s.length
        val padLeft = padding / 2
        val padRight = padding - padLeft

        return " ".repeat(padLeft) + s + " ".repeat(padRight)
    }
}
