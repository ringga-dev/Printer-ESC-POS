package ngga.ring.printer.manager

actual class PrinterPermissionManager {
    actual constructor()

    actual fun hasPermissions(connectionType: String): Boolean = true
    actual fun requestPermissions(connectionType: String, onResult: (Boolean) -> Unit) {
        onResult(true)
    }
}
