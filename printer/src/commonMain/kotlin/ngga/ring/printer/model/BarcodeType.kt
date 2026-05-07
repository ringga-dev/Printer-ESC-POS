package ngga.ring.printer.model

/**
 * Supported Barcode Types for ESC/POS.
 * System B (GS k m n d1...dn) is used for better compatibility.
 */
enum class BarcodeType(val value: Int) {
    UPCA(65),
    UPCE(66),
    EAN13(67),
    EAN8(68),
    CODE39(69),
    ITF(70),
    NW7(71),
    CODE93(72),
    CODE128(73)
}
