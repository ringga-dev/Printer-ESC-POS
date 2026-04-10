package ngga.ring.printer.util

/**
 * Representation of a printer's current connection state.
 */
sealed class ConnectionState {
    /** No active connection. */
    object Disconnected : ConnectionState()

    /** Attempting to establish a connection. */
    object Connecting : ConnectionState()

    /** Successfully connected to a device. */
    data class Connected(
        val name: String?,
        val address: String?
    ) : ConnectionState()

    /** Connection failed or was lost. */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : ConnectionState()
}
