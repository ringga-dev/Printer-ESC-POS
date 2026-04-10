package ngga.ring.printer.manager

import ngga.ring.printer.model.PrinterConfig

interface PrinterConnector {
    suspend fun connect(config: PrinterConfig): Boolean
    suspend fun sendData(data: ByteArray): Boolean
    suspend fun disconnect()
    fun isConnected(): Boolean
}
