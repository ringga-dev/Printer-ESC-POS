package ngga.ring.printer.manager

import platform.CoreBluetooth.*

actual class PrinterPermissionManager {
    actual constructor()

    actual fun hasPermissions(connectionType: String): Boolean {
        return if (connectionType == "BLUETOOTH") {
            CBCentralManager.authorization == CBManagerAuthorizationAllowedAlways
        } else {
            true
        }
    }

    actual fun requestPermissions(connectionType: String, onResult: (Boolean) -> Unit) {
        // iOS requests permission automatically when CBCentralManager is instantiated
        // or when a scan is started.
        onResult(true)
    }
}
