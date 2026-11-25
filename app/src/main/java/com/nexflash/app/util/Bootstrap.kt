package com.nexflash.app.util

import android.content.Context
import android.os.Build
import java.io.File
import java.util.Properties
import java.util.zip.ZipInputStream

object Bootstrap {
    fun files(ctx: Context): File = ctx.filesDir
    fun usr(ctx: Context) = File(files(ctx), "usr")
    fun bin(ctx: Context) = File(usr(ctx), "bin")
    fun lib(ctx: Context) = File(usr(ctx), "lib")
    fun tmp(ctx: Context) = File(usr(ctx), "tmp")
    fun home(ctx: Context) = File(files(ctx), "home")

    fun ensure(ctx: Context) {
        val marker = File(usr(ctx), ".nexflash_bootstrap_v1")
        if (marker.exists()) return
        synchronized(this) {
            if (marker.exists()) return
            usr(ctx).mkdirs(); home(ctx).mkdirs(); tmp(ctx).mkdirs(); bin(ctx).mkdirs(); lib(ctx).mkdirs()
            extractBootstrap(ctx)
            makeSymlinks(ctx)
            marker.writeText(Build.FINGERPRINT)
        }
    }

    private fun extractBootstrap(ctx: Context) {
        ctx.assets.open("bootstrap.zip").use { input ->
            ZipInputStream(input).use { zis ->
                var e = zis.nextEntry
                while (e != null) {
                    val out = File(usr(ctx), e.name)
                    if (e.isDirectory) out.mkdirs() else {
                        out.parentFile?.mkdirs()
                        out.outputStream().use { zis.copyTo(it) }
                    }
                    zis.closeEntry()
                    e = zis.nextEntry
                }
            }
        }
    }

    private fun makeSymlinks(ctx: Context) {
        val props = Properties()
        ctx.assets.open("lib_symlinks.properties").use { props.load(it) }
        val native = File(ctx.applicationInfo.nativeLibraryDir)
        for ((k, v) in props) {
            val src = File(native, k.toString())
            if (!src.exists()) continue
            val targets = v.toString().split(',').map { it.trim() }.filter { it.isNotEmpty() }
            for (t in targets) {
                val dst = File(usr(ctx), t)
                dst.parentFile?.mkdirs()
                if (dst.exists()) dst.delete()
                try { Os.symlink(src.absolutePath, dst.absolutePath) } catch (_: Throwable) {
                    src.copyTo(dst, overwrite = true)
                }
                dst.setExecutable(true, false)
                dst.setReadable(true, false)
            }
        }
    }

    fun env(ctx: Context): Map<String, String> {
        val u = usr(ctx).absolutePath
        val native = ctx.applicationInfo.nativeLibraryDir
        val path = "$u/bin:$native:" + (System.getenv("PATH") ?: "")
        return mapOf(
            "HOME" to home(ctx).absolutePath,
            "PREFIX" to u,
            "TMPDIR" to tmp(ctx).absolutePath,
            "PATH" to path,
            "LD_LIBRARY_PATH" to "$u/lib:$native",
            "TERM" to "xterm-256color",
            "LANG" to "en_US.UTF-8",
            "ANDROID_ROOT" to "/system",
            "ANDROID_DATA" to "/data",
            "ADB_LIBUSB" to "1",
            "ADB_MDNS_OPENSCREEN" to "0"
        )
    }

    fun bin(ctx: Context, name: String): File = File(bin(ctx), name)
}

object Os {
    fun symlink(target: String, link: String) {
        android.system.Os.symlink(target, link)
    }
}
