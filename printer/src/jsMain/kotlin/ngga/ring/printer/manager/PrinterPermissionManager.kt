package ngga.ring.printer.manager

/**
 * JS Implementation of PrinterPermissionManager.
 * In Web, permissions are typically handled by browser dialogs during connection.
 */
actual class PrinterPermissionManager actual constructor() {
    actual fun hasPermissions(connectionType: String): Boolean = true
    actual fun requestPermissions(connectionType: String, onResult: (Boolean) -> Unit) {
        onResult(true)
    }
}
