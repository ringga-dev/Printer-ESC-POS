package ngga.ring.printer.manager

import android.content.Context
import androidx.activity.ComponentActivity
import java.lang.ref.WeakReference

object PrinterInitializer {
    private var context: Context? = null
    private var activityRef: WeakReference<ComponentActivity>? = null

    fun initialize(activity: ComponentActivity) {
        this.context = activity.applicationContext
        this.activityRef = WeakReference(activity)
    }

    fun getContext(): Context {
        return context ?: throw IllegalStateException("PrinterInitializer not initialized with context")
    }
    
    fun getActivity(): ComponentActivity? {
        return activityRef?.get()
    }
}
