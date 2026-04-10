package ngga.ring.printer.manager

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-independent utility for managing printer-related permissions.
 * Handles Bluetooth, Location, and USB permission states.
 */
expect class PrinterPermissionManager {
    constructor()
    
    /**
     * Checks if all necessary permissions are granted for the specified connection type.
     */
    fun hasPermissions(connectionType: String): Boolean
    
    /**
     * Requests the necessary permissions for discovery and printing.
     * @param onResult Callback with the final permission state.
     */
    fun requestPermissions(connectionType: String, onResult: (Boolean) -> Unit)
}
