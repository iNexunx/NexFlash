package com.nexflash.app

import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.material3.Surface
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexflash.app.ui.MainViewModel
import com.nexflash.app.ui.NexFlashRoot
import com.nexflash.app.ui.theme.NexFlashTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ui by vm.state.collectAsStateWithLifecycle()
            NexFlashTheme(accent = ui.accent) {
                Surface { NexFlashRoot(vm) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED || intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED) vm.refresh()
    }
}
