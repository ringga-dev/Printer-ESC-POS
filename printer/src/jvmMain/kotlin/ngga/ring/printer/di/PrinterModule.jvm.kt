package ngga.ring.printer.di

import ngga.ring.printer.helper.PrinterBluetoothHelper
import ngga.ring.printer.helper.PrinterTcpHelper
import ngga.ring.printer.helper.PrinterUsbHelper
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * JVM implementation of the printer Koin module.
 */
actual val printerModule: Module = module {
    single { PrinterBluetoothHelper() }
    single { PrinterTcpHelper() }
    single { PrinterUsbHelper() }
}
