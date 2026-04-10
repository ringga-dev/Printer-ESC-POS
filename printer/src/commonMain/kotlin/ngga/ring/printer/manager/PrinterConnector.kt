package ngga.ring.printer.manager

import ngga.ring.data.model.PrinterConfigEntity

/**
 * Common interface for printer connection and communication across platforms.
 */
interface PrinterConnector {
    /**
     * Attempts to connect to the printer using the provided configuration.
     */
    suspend fun connect(config: PrinterConfigEntity): Boolean

    /**
     * Sends raw byte data (ESC/POS commands) to the printer.
     */
    suspend fun sendData(data: ByteArray): Boolean

    /**
     * Closes the connection to the printer.
     */
    suspend fun disconnect()

    /**
     * Checks if the printer is currently connected.
     */
    fun isConnected(): Boolean
}
