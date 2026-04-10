package ngga.ring.printer.manager

import android.content.Context

object PrinterInitializer {
    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    fun getContext(): Context {
        return context ?: throw IllegalStateException("PrinterInitializer not initialized with context")
    }
}
