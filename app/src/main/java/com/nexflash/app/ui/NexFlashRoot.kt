@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.nexflash.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexflash.app.adb.Device
import com.nexflash.app.ui.theme.Accent
import com.nexflash.app.ui.theme.NexFlashTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NexFlashRoot(vm: MainViewModel = viewModel()) {
    val s by vm.state.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("ADB", "Fastboot", "Consola")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NexFlash") },
                actions = {
                    AccentMenu(selected = s.accent, onSelect = vm::setAccent)
                    IconButton(onClick = vm::refresh, enabled = !s.busy) { Icon(Icons.Default.Refresh, "Refrescar") }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (s.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            PrimaryTabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
            }
            when (tab) {
                0 -> AdbTab(s, vm)
                1 -> FastbootTab(s, vm)
                2 -> ConsoleTab(s, vm)
            }
        }
    }
}

@Composable
fun AccentMenu(selected: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Default.Palette, "Acento") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Accent.entries.forEachIndexed { i, a ->
                DropdownMenuItem(text = { Text(a.label) }, onClick = { onSelect(i); open = false },
                    leadingIcon = { if (i == selected) Icon(Icons.Default.Check, null) })
            }
        }
    }
}

@Composable
fun DevicePicker(devices: List<Device>, selected: String?, onSelect: (String) -> Unit, label: String) {
    var exp by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = it }) {
        OutlinedTextField(value = selected ?: "Sin dispositivos", onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(exp) },
            modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(24.dp))
        ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
            devices.forEach { d -> DropdownMenuItem(text = { Text("${d.serial}  [${d.state}] ${d.detail}") }, onClick = { onSelect(d.serial); exp = false }) }
        }
    }
}

@Composable
fun Pill(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(999.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
        Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(text)
    }
}

@Composable
fun AdbTab(s: UiState, vm: MainViewModel) {
    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> uri?.let(vm::sideload) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { DevicePicker(s.adbDevices, s.selectedAdb, vm::selectAdb, "Dispositivo ADB") }
        item { Text("Acciones rápidas", style = MaterialTheme.typography.titleMedium) }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Devices", Icons.Default.Usb, !s.busy) { vm.runAdb(listOf("devices","-l")) }
            Pill("Shell", Icons.Default.Terminal, !s.busy) { vm.runAdb(listOf("shell","getprop","ro.product.model")) }
            Pill("Reboot", Icons.Default.RestartAlt, !s.busy) { vm.runAdb(listOf("reboot")) }
            Pill("Recovery", Icons.Default.Healing, !s.busy) { vm.runAdb(listOf("reboot","recovery")) }
            Pill("Bootloader", Icons.Default.Build, !s.busy) { vm.runAdb(listOf("reboot","bootloader")) }
            Pill("Sideload mode", Icons.Default.SystemUpdate, !s.busy) { vm.runAdb(listOf("reboot","sideload")) }
        } }
        item { ElevatedCard(shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Sideload ZIP / ROM / GApps", style = MaterialTheme.typography.titleMedium)
            Text("Selecciona un .zip y NexFlash ejecuta adb sideload.", style = MaterialTheme.typography.bodySmall)
            Pill("Elegir ZIP y Sideload", Icons.Default.FolderZip, !s.busy) { zipPicker.launch(arrayOf("application/zip","*/*")) }
        } } }
        item { CmdCard("Comando ADB personalizado", "shell ls /sdcard", !s.busy) { vm.runRaw("adb $it") } }
        item { LogCard(s.log, vm::clearLog) }
    }
}

@Composable
fun FastbootTab(s: UiState, vm: MainViewModel) {
    var partition by remember { mutableStateOf("boot") }
    val imgPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> uri?.let { vm.flash(partition, it) } }
    val partitions = listOf("boot","init_boot","recovery","vendor_boot","dtbo","vbmeta","system","vendor","product","super","userdata")
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { DevicePicker(s.fbDevices, s.selectedFb, vm::selectFb, "Dispositivo Fastboot") }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Devices", Icons.Default.Usb, !s.busy) { vm.runFastboot(listOf("devices","-l")) }
            Pill("Getvar all", Icons.Default.Info, !s.busy) { vm.runFastboot(listOf("getvar","all")) }
            Pill("Reboot", Icons.Default.RestartAlt, !s.busy) { vm.runFastboot(listOf("reboot")) }
            Pill("Reboot bootloader", Icons.Default.Build, !s.busy) { vm.runFastboot(listOf("reboot-bootloader")) }
            Pill("Reboot recovery", Icons.Default.Healing, !s.busy) { vm.runFastboot(listOf("reboot","recovery")) }
        } }
        item { ElevatedCard(shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Flash partición", style = MaterialTheme.typography.titleMedium)
            var exp by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = it }) {
                OutlinedTextField(value = partition, onValueChange = { partition = it }, label = { Text("Partición") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(exp) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(24.dp))
                ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) { partitions.forEach { p -> DropdownMenuItem(text = { Text(p) }, onClick = { partition = p; exp = false }) } }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Elegir IMG y Flash", Icons.Default.FlashOn, !s.busy && partition.isNotBlank()) { imgPicker.launch(arrayOf("application/octet-stream","*/*")) }
                Pill("Erase", Icons.Default.DeleteForever, !s.busy && partition.isNotBlank()) { vm.runFastboot(listOf("erase", partition)) }
            }
        } } }
        item { CmdCard("Comando Fastboot personalizado", "getvar all", !s.busy) { vm.runRaw("fastboot $it") } }
        item { LogCard(s.log, vm::clearLog) }
    }
}

@Composable
fun ConsoleTab(s: UiState, vm: MainViewModel) {
    var cmd by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = cmd, onValueChange = { cmd = it }, label = { Text("Comando (adb/fastboot/...)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Ejecutar", Icons.Default.PlayArrow, !s.busy && cmd.isNotBlank()) { vm.runRaw(cmd) }
            Pill("Limpiar log", Icons.Default.Delete, true) { vm.clearLog() }
        }
        LogCard(s.log, vm::clearLog, Modifier.weight(1f))
    }
}

@Composable
fun CmdCard(title: String, hint: String, enabled: Boolean, onRun: (String) -> Unit) {
    var v by remember { mutableStateOf("") }
    ElevatedCard(shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = v, onValueChange = { v = it }, label = { Text(hint) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp))
        Pill("Ejecutar", Icons.Default.PlayArrow, enabled && v.isNotBlank()) { onRun(v) }
    } }
}

@Composable
fun LogCard(log: String, onClear: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Log", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onClear) { Text("Limpiar") }
            }
            Box(Modifier.heightIn(min = 120.dp, max = 320.dp).verticalScroll(rememberScrollState())) {
                Text(log.ifBlank { "Sin salida." }, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
