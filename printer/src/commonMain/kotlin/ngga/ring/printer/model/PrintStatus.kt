package ngga.ring.printer.model

sealed class PrintStatus {
    object Idle : PrintStatus()
    object Connecting : PrintStatus()
    object Processing : PrintStatus()
    object Sending : PrintStatus()
    object Success : PrintStatus()
    data class Error(val message: String) : PrintStatus()
}
