package com.nexflash.app.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

data class CmdResult(val code: Int, val out: String, val err: String)

object Shell {
    suspend fun run(ctx: Context, cmd: List<String>, timeoutSec: Long = 120): CmdResult = withContext(Dispatchers.IO) {
        val env = Bootstrap.env(ctx)
        val pb = ProcessBuilder(cmd)
        pb.directory(File(env["HOME"]!!))
        val e = pb.environment()
        e.putAll(env)
        val p = pb.start()
        val stdout = p.inputStream.bufferedReader().readText()
        val stderr = p.errorStream.bufferedReader().readText()
        val done = p.waitFor(timeoutSec, TimeUnit.SECONDS)
        if (!done) { p.destroyForcibly(); return@withContext CmdResult(124, stdout, stderr + "\nTimeout") }
        CmdResult(p.exitValue(), stdout.trim(), stderr.trim())
    }

    fun adb(ctx: Context, args: List<String>, serial: String?) = buildList {
        add(Bootstrap.bin(ctx, "adb").absolutePath)
        if (!serial.isNullOrBlank()) { add("-s"); add(serial) }
        addAll(args)
    }

    fun fastboot(ctx: Context, args: List<String>, serial: String?) = buildList {
        add(Bootstrap.bin(ctx, "fastboot").absolutePath)
        if (!serial.isNullOrBlank()) { add("-s"); add(serial) }
        addAll(args)
    }
}
