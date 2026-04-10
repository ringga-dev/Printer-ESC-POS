package ngga.ring.printer.helper

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.koin.java.KoinJavaComponent.inject

/**
 * A transparent activity used to request Bluetooth enablement from the user.
 * This is used by [PrinterBluetoothHelper] to handle the asynchronous nature of 
 * Android's Bluetooth enablement request in a clean, suspend-friendly way.
 */
class BluetoothEnableContainerActivity : AppCompatActivity() {

    private val bluetoothHelper: PrinterBluetoothHelper by inject(PrinterBluetoothHelper::class.java)

    private val enableBluetoothActivityForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val identifier = intent.getLongExtra("identifier", 0)
        bluetoothHelper.turnOnRequestMap[identifier]?.invoke()
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            enableBluetoothActivityForResult.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } catch (e: SecurityException) {
            val identifier = intent.getLongExtra("identifier", 0)
            bluetoothHelper.turnOnRequestMap[identifier]?.invoke()
            finish()
        }
    }
}
