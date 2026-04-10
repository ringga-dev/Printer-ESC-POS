package ngga.ring.printer.di

import android.content.Context
import ngga.ring.printer.helper.PrinterBluetoothHelper
import ngga.ring.printer.helper.PrinterTcpHelper
import ngga.ring.printer.helper.PrinterUsbHelper
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android implementation of the printer Koin module.
 * Provides platform-specific helpers for Bluetooth, TCP, and USB.
 */
actual val printerModule: Module = module {
    // Initialize the PrinterInitializer with the Android context at startup
    // This ensures that non-Koin-aware components can still access the context
    single(createdAtStart = true) { 
        ngga.ring.printer.manager.PrinterInitializer.initialize(get())
        true 
    }

    // Bluetooth: Requires Context for adapter access
    single { PrinterBluetoothHelper(get()) }
    
    // TCP: Pure Kotlin socket interaction
    single { PrinterTcpHelper() }
    
    // USB: Requires Context for USB Service
    single { PrinterUsbHelper(get()) }
}
