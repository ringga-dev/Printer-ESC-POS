package ngga.ring.printer.manager

import ngga.ring.printer.model.PrinterConfig

/**
 * Result from a printer discovery process.
 */
interface PrinterConnector {
    suspend fun connect(config: PrinterConfig): Boolean
    suspend fun sendData(data: ByteArray): Boolean
    
    /**
     * Reads data from the printer if supported by the connection type.
     * @param count Maximum bytes to read.
     * @param timeout Timeout in milliseconds.
     */
    suspend fun readData(count: Int, timeout: Long = 2000): ByteArray?

    suspend fun disconnect()
    fun isConnected(): Boolean
}
