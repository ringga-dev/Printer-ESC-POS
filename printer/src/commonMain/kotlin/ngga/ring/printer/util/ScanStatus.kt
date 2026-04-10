package ngga.ring.printer.util

/**
 * Status of the printer discovery process.
 */
enum class ScanStatus {
    /** No scanning is happening. */
    Idle,

    /** Active scanning for devices. */
    Scanning,

    /** An error occurred during scanning. */
    Error
}
