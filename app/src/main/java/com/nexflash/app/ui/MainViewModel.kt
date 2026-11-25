package com.nexflash.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexflash.app.adb.Device
import com.nexflash.app.adb.DeviceRepo
import com.nexflash.app.adb.UsbBridge
import com.nexflash.app.util.Bootstrap
import com.nexflash.app.util.Shell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class UiState(
    val adbDevices: List<Device> = emptyList(),
    val fbDevices: List<Device> = emptyList(),
    val selectedAdb: String? = null,
    val selectedFb: String? = null,
    val log: String = "NexFlash listo.\n",
    val busy: Boolean = false,
    val accent: Int = 0,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx get() = getApplication<Application>()
    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()
    private val bridge = UsbBridge(ctx)

    init {
        viewModelScope.launch {
            Bootstrap.ensure(ctx)
            bridge.start()
            refresh()
        }
    }

    override fun onCleared() { bridge.stop(); super.onCleared() }

    fun setAccent(i: Int) = _state.update { it.copy(accent = i) }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(busy = true) }
        val adb = runCatching { DeviceRepo.listAdb(ctx) }.getOrDefault(emptyList())
        val fb = runCatching { DeviceRepo.listFastboot(ctx) }.getOrDefault(emptyList())
        _state.update { s ->
            s.copy(adbDevices = adb, fbDevices = fb,
                selectedAdb = s.selectedAdb?.takeIf { id -> adb.any { it.serial == id } } ?: adb.firstOrNull()?.serial,
                selectedFb = s.selectedFb?.takeIf { id -> fb.any { it.serial == id } } ?: fb.firstOrNull()?.serial,
                busy = false)
        }
        log("[refresh] adb=${adb.size} fastboot=${fb.size}")
    }

    fun selectAdb(s: String) = _state.update { it.copy(selectedAdb = s) }
    fun selectFb(s: String) = _state.update { it.copy(selectedFb = s) }

    fun runAdb(args: List<String>) = exec(Shell.adb(ctx, args, state.value.selectedAdb))
    fun runFastboot(args: List<String>) = exec(Shell.fastboot(ctx, args, state.value.selectedFb))
    fun runRaw(cmd: String) = viewModelScope.launch {
        val parts = cmd.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return@launch
        val bin = File(Bootstrap.bin(ctx), parts[0])
        val full = if (bin.exists()) listOf(bin.absolutePath) + parts.drop(1) else parts
        exec(full)
    }

    private fun exec(cmd: List<String>) = viewModelScope.launch {
        _state.update { it.copy(busy = true) }
        log("$ " + cmd.joinToString(" "))
        val r = runCatching { Shell.run(ctx, cmd) }.getOrElse { com.nexflash.app.util.CmdResult(1, "", it.message ?: "error") }
        if (r.out.isNotBlank()) log(r.out)
        if (r.err.isNotBlank()) log("ERR: ${r.err}")
        log("[exit ${r.code}]")
        _state.update { it.copy(busy = false) }
        refresh()
    }

    fun sideload(uri: Uri) = viewModelScope.launch {
        val f = copyUri(uri, "sideload.zip") ?: return@launch
        exec(Shell.adb(ctx, listOf("sideload", f.absolutePath), state.value.selectedAdb))
    }

    fun flash(partition: String, uri: Uri) = viewModelScope.launch {
        val ext = (uri.lastPathSegment ?: "img").substringAfterLast('.', "img")
        val f = copyUri(uri, "flash.$ext") ?: return@launch
        exec(Shell.fastboot(ctx, listOf("flash", partition, f.absolutePath), state.value.selectedFb))
    }

    private suspend fun copyUri(uri: Uri, name: String): File? = runCatching {
        val f = File(ctx.cacheDir, name)
        ctx.contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { o -> i.copyTo(o) } } ?: return null
        f
    }.onFailure { log("ERR: ${it.message}") }.getOrNull()

    fun clearLog() = _state.update { it.copy(log = "") }
    private fun log(s: String) = _state.update { it.copy(log = it.log + s + "\n") }
}
