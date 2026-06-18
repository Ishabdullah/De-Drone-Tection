package com.dedroneTECTION.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.dedroneTECTION.model.SDRStatus

class USBDeviceReceiver : BroadcastReceiver() {

    companion object {
        private const val ACTION_USB_PERMISSION = "com.dedroneTECTION.USB_PERMISSION"

        private val RTL_SDR_VENDOR_IDS = intArrayOf(0x0BDA, 0x1546)

        fun handleUSBDevice(context: Context, device: UsbDevice): SDRStatus {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            val isRTLSdr = RTL_SDR_VENDOR_IDS.contains(device.vendorId)

            return if (isRTLSdr) {
                val hasPermission = usbManager?.hasPermission(device) ?: false
                if (!hasPermission) {
                    val flags = if (android.os.Build.VERSION.SDK_INT >= 31) {
                        PendingIntent.FLAG_MUTABLE
                    } else {
                        0
                    }
                    val permissionIntent = PendingIntent.getBroadcast(
                        context, 0, Intent(ACTION_USB_PERMISSION), flags
                    )
                    usbManager?.requestPermission(device, permissionIntent)
                }
                SDRStatus(
                    connected = true,
                    deviceName = device.deviceName,
                    vendorId = device.vendorId,
                    productId = device.productId
                )
            } else {
                SDRStatus(connected = false)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                val device = if (android.os.Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                }

                device?.let {
                    val status = handleUSBDevice(context, it)
                    val broadcastIntent = Intent("com.dedroneTECTION.SDR_STATUS_CHANGED")
                    broadcastIntent.putExtra("connected", status.connected)
                    broadcastIntent.putExtra("device_name", status.deviceName)
                    broadcastIntent.putExtra("vendor_id", status.vendorId)
                    broadcastIntent.putExtra("product_id", status.productId)
                    context.sendBroadcast(broadcastIntent)
                }
            }

            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                val broadcastIntent = Intent("com.dedroneTECTION.SDR_STATUS_CHANGED")
                broadcastIntent.putExtra("connected", false)
                broadcastIntent.putExtra("device_name", "")
                context.sendBroadcast(broadcastIntent)
            }

            ACTION_USB_PERMISSION -> {
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                if (granted) {
                    val device = if (android.os.Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    device?.let {
                        val status = handleUSBDevice(context, it)
                        val broadcastIntent = Intent("com.dedroneTECTION.SDR_STATUS_CHANGED")
                        broadcastIntent.putExtra("connected", status.connected)
                        broadcastIntent.putExtra("device_name", status.deviceName)
                        context.sendBroadcast(broadcastIntent)
                    }
                }
            }
        }
    }
}
