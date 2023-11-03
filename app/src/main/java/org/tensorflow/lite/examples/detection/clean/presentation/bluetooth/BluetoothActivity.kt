package org.tensorflow.lite.examples.detection.clean.presentation.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.harrysoft.androidbluetoothserial.BluetoothManager
import com.harrysoft.androidbluetoothserial.BluetoothSerialDevice
import com.harrysoft.androidbluetoothserial.SimpleBluetoothDeviceInterface
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.tensorflow.lite.examples.detection.clean.data.utils.log
import org.tensorflow.lite.examples.detection.databinding.ActivityBluetoothBinding

class BluetoothActivity : AppCompatActivity() {
    var LedIsOn = false

    lateinit var bluetoothManager: BluetoothManager
    lateinit var views: ActivityBluetoothBinding
    val macAddress = "98:D3:61:F6:A6:69"
    var bluetoothCommand = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityBluetoothBinding.inflate(layoutInflater)
        setContentView(views.root)
        //    getPermissions()
//        setup()
        views.btnLED.setOnClickListener {
            toggleLED()
        }
    }

    fun toggleLED() {
        "LED clicked".log("bluetooth_")
        bluetoothCommand = if (LedIsOn) "LED_OFF" else "LED_ON"
        LedIsOn = !LedIsOn
        setup()
    }

    fun setup() {
        // Setup our BluetoothManager
        // Setup our BluetoothManager
        bluetoothManager = BluetoothManager.getInstance()
//        if (bluetoothManager == null) {
//            // Bluetooth unavailable on this device :( tell the user
//            Toast.makeText(this, "Bluetooth not available.", Toast.LENGTH_LONG)
//                .show() // Replace context with your context instance.
//            finish()
//            return
//        }
//
//        val pairedDevices: Collection<BluetoothDevice> = bluetoothManager.pairedDevicesList
//        for (device in pairedDevices) {
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.BLUETOOTH_CONNECT
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                // TODO: Consider calling
//                //    ActivityCompat#requestPermissions
//                // here to request the missing permissions, and then overriding
//                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                //                                          int[] grantResults)
//                // to handle the case where the user grants the permission. See the documentation
//                // for ActivityCompat#requestPermissions for more details.
//                return
//            }
//            Log.d(
//                "My Bluetooth App", "Device name: " + device.name
//            )
//            Log.d("My Bluetooth App", "Device MAC Address: " + device.address)

            connectDevice(macAddress)
//        }
    }

    private var deviceInterface: SimpleBluetoothDeviceInterface? = null

    private fun connectDevice(mac: String) {
        bluetoothManager.openSerialDevice(mac)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ connectedDevice: BluetoothSerialDevice -> onConnected(connectedDevice) }) { error: Throwable ->
                onError(
                    error
                )
            }
    }

    private fun onConnected(connectedDevice: BluetoothSerialDevice) {
        // You are now connected to this device!
        // Here you may want to retain an instance to your device:
        "Connected".toString().log("bluetooth_")
        deviceInterface = connectedDevice.toSimpleDeviceInterface()

        // Listen to bluetooth events
        deviceInterface?.setListeners(
            { message: String -> onMessageReceived(message) },
            { message: String -> onMessageSent(message) },
            { error: Throwable -> onError(error) })
            deviceInterface?.sendMessage(bluetoothCommand)
        // Let's send a message:
//        Thread {
//            for (i in 1..9999) {
//                deviceInterface?.sendMessage("1")
//                Thread.sleep(10);
//            }
//        }.start()
//
//        deviceInterface?.sendMessage("1")
    }

    private fun onMessageSent(message: String) {
        "Sent a message! Message was: $message".log("bluetooth_")
        bluetoothManager.close();
    }

    private fun onMessageReceived(message: String) {
        "Received a message! Message was: $message".log("bluetooth_")
        bluetoothManager.close();
    }

    private fun onError(error: Throwable) {
        error.message.toString().log("bluetooth_ error")
        bluetoothManager.close();
    }

    fun getPermissions() {
        Dexter.withContext(this)
            .withPermissions(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                }
            })
            .check()
    }
}