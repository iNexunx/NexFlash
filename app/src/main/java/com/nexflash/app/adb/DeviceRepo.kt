package com.nexflash.app.adb

import android.content.Context
import com.nexflash.app.util.Shell

data class Device(val serial: String, val state: String, val detail: String, val kind: Kind) {
    enum class Kind { ADB, FASTBOOT }
}

object DeviceRepo {
    data class Scan(val devices: List<Device>, val raw: String, val err: String, val code: Int)
    suspend fun listAdb(ctx: Context): Scan {
        val r = Shell.run(ctx, Shell.adb(ctx, listOf("devices","-l"), null))
        return Scan(parse(r.out, Device.Kind.ADB), r.out, r.err, r.code)
    }
    suspend fun listFastboot(ctx: Context): Scan {
        val r = Shell.run(ctx, Shell.fastboot(ctx, listOf("devices","-l"), null))
        return Scan(parse(r.out, Device.Kind.FASTBOOT), r.out, r.err, r.code)
    }
    fun usbHostDevices(ctx: Context): List<String> {
        val usb = ctx.getSystemService(Context.USB_SERVICE) as android.hardware.usb.UsbManager
        return usb.deviceList.values.map { d ->
            "${d.deviceName} vid=${d.vendorId} pid=${d.productId} prod=${d.productName ?: ""} serial=${d.serialNumber ?: ""} cls=${d.deviceClass}/${d.deviceSubclass} ifaces=${d.interfaceCount}"
        }
    }

    private fun parse(out: String, kind: Device.Kind): List<Device> = out.lines().mapNotNull { line ->
        val t = line.trim()
        if (t.isEmpty() || t.startsWith("List of") || t.startsWith("*")) return@mapNotNull null
        val parts = t.split(Regex("\\s+"))
        if (parts.size < 2) return@mapNotNull null
        val serial = parts[0]
        val state = parts[1]
        Device(serial, state, parts.drop(2).joinToString(" "), kind)
    }
}
