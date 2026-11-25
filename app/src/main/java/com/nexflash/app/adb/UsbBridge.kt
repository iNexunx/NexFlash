package com.nexflash.app.adb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.os.Build
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.*
import kotlinx.coroutines.runBlocking
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileDescriptor
import java.util.concurrent.ConcurrentHashMap

class UsbBridge(private val ctx: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val usb = ctx.getSystemService(Context.USB_SERVICE) as UsbManager
    private val connections = ConcurrentHashMap<String, android.hardware.usb.UsbDeviceConnection>()
    @Volatile private var server: LocalServerSocket? = null
    @Volatile private var generation: Int = 0

    fun start() {
        if (server != null) return
        scope.launch {
            try { LocalServerSocket(SOCKET).close() } catch (_: Throwable) {}
            val ss = LocalServerSocket(SOCKET)
            server = ss
            while (isActive) {
                val client = try { ss.accept() } catch (_: Throwable) { break }
                launch { handle(client) }
            }
        }
    }

    fun restart() {
        generation++
        closeAll()
        try { server?.close() } catch (_: Throwable) {}
        server = null
        start()
    }

    fun stop() {
        try { server?.close() } catch (_: Throwable) {}
        server = null
        closeAll()
        scope.cancel()
    }

    fun closeAll() {
        connections.keys.toList().forEach { k ->
            runCatching { connections.remove(k)?.close() }
        }
    }

    private fun handle(sock: LocalSocket) {
        sock.use { s ->
            try {
                val ins = DataInputStream(s.inputStream)
                val out = DataOutputStream(s.outputStream)
                when (ins.readUnsignedShort()) {
                    0 -> {
                        val names = usb.deviceList.keys.joinToString(":")
                        if (names.isNotEmpty()) out.writeUTF(names)
                        Unit
                    }
                    1 -> {
                        val name = ins.readUTF()
                        val dev = usb.deviceList[name] ?: return
                        val ok = runBlocking { ensurePermission(dev) }
                        if (!ok) return
                        // close stale connection for same device before reopening
                        connections.remove(name)?.let { runCatching { it.close() } }
                        val conn = usb.openDevice(dev) ?: return
                        val fd = conn.fileDescriptor
                        if (fd < 0) { conn.close(); return }
                        connections[name] = conn
                        val pfd = dupFd(fd)
                        pfd.use {
                            s.setFileDescriptorsForSend(arrayOf(it.fileDescriptor))
                            s.outputStream.write(0)
                        }
                        // keep conn alive until restart/closeAll; adb/libusb owns the dup'd fd
                    }
                    2 -> {
                        val name = ins.readUTF()
                        val dev = usb.deviceList[name] ?: return
                        dev.serialNumber?.let { out.writeUTF(it) }
                    }
                    else -> Unit
                }
            } catch (_: Throwable) {}
        }
    }

    private suspend fun ensurePermission(dev: UsbDevice): Boolean {
        if (usb.hasPermission(dev)) return true
        val granted = CompletableDeferred<Boolean>()
        val action = ACTION_USB_PERMISSION + "." + dev.deviceName.hashCode()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                if (i.action != action) return
                try { ctx.unregisterReceiver(this) } catch (_: Throwable) {}
                granted.complete(i.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
            }
        }
        val flags = if (Build.VERSION.SDK_INT >= 33) Context.RECEIVER_NOT_EXPORTED else 0
        ctx.registerReceiver(receiver, IntentFilter(action), flags)
        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
        val pi = PendingIntent.getBroadcast(ctx, dev.deviceId, Intent(action).setPackage(ctx.packageName), piFlags)
        usb.requestPermission(dev, pi)
        return withTimeoutOrNull(30_000) { granted.await() } ?: false
    }


    private fun dupFd(fd: Int): ParcelFileDescriptor {
        val tmp = java.io.FileDescriptor()
        val f = java.io.FileDescriptor::class.java.getDeclaredField("descriptor")
        f.isAccessible = true
        f.setInt(tmp, fd)
        val dup = android.system.Os.dup(tmp)
        val out = f.getInt(dup)
        return ParcelFileDescriptor.adoptFd(out)
    }
    companion object {
        const val SOCKET = "adbify_usb_bridge"
        const val ACTION_USB_PERMISSION = "com.nexflash.app.USB_PERMISSION"
    }
}
