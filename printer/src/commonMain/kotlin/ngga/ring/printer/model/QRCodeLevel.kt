package ngga.ring.printer.model

/**
 * QR Code Error Correction Levels.
 */
enum class QRCodeLevel(val value: Int) {
    L(48), // Level L (7%)
    M(49), // Level M (15%)
    Q(50), // Level Q (25%)
    H(51)  // Level H (30%)
}
