package com.example.usbapp

import android.app.PendingIntent
import android.content.*
import android.hardware.usb.*
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import android.content.Context

class MainActivity : ComponentActivity() {

    private lateinit var usbManager: UsbManager
    private lateinit var permissionIntent: PendingIntent
    private val ACTION_USB_PERMISSION = "com.example.usbapp.USB_PERMISSION"

    private lateinit var textView: TextView

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {

                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device: UsbDevice? =
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                        if (intent.getBooleanExtra(
                                UsbManager.EXTRA_PERMISSION_GRANTED, false
                            )
                        ) {
                            device?.let {
                                textView.text = "Permiso concedido:\n${it.deviceName}"
                                conectarDispositivo(it)
                            }
                        } else {
                            textView.text = "Permiso denegado"
                        }
                    }
                }

                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device: UsbDevice? =
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                    device?.let {
                        textView.text = "USB conectado:\n${it.deviceName}"
                        pedirPermiso(it)
                    }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    textView.text = "USB desconectado"
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textView = TextView(this)
        textView.text = "Esperando USB..."
        setContentView(textView)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        permissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE
        )

        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(usbReceiver, filter)
        }
        detectarDispositivos()
    }

    private fun detectarDispositivos() {
        val deviceList = usbManager.deviceList

        if (deviceList.isEmpty()) {
            textView.text = "No hay dispositivos USB"
        } else {
            for (device in deviceList.values) {
                textView.text = "USB encontrado:\n${device.deviceName}"
                pedirPermiso(device)
                break
            }
        }
    }

    private fun pedirPermiso(device: UsbDevice) {
        usbManager.requestPermission(device, permissionIntent)
    }

    private fun conectarDispositivo(device: UsbDevice) {
        val connection = usbManager.openDevice(device)

        if (connection != null) {
            textView.append("\nConectado correctamente")

            val intf = device.getInterface(0)
            val endpoint = intf.getEndpoint(0)

            connection.claimInterface(intf, true)

            val data = "Hola USB".toByteArray()
            connection.bulkTransfer(endpoint, data, data.size, 0)

        } else {
            textView.append("\nError al conectar")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
    }
}