package ngga.ring.printer.manager

/**
 * WASM Implementation of PrinterPermissionManager.
 */
actual class PrinterPermissionManager actual constructor() {
    actual fun hasPermissions(connectionType: String): Boolean = true
    actual fun requestPermissions(connectionType: String, onResult: (Boolean) -> Unit) {
        onResult(true)
    }
}
