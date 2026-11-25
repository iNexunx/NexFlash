package com.nexflash.app.adb

import android.content.Context
import com.nexflash.app.util.Shell

data class Device(val serial: String, val state: String, val detail: String, val kind: Kind) {
    enum class Kind { ADB, FASTBOOT }
}

object DeviceRepo {
    suspend fun listAdb(ctx: Context): List<Device> = parse(Shell.run(ctx, Shell.adb(ctx, listOf("devices","-l"), null)).out, Device.Kind.ADB)
    suspend fun listFastboot(ctx: Context): List<Device> = parse(Shell.run(ctx, Shell.fastboot(ctx, listOf("devices","-l"), null)).out, Device.Kind.FASTBOOT)

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
