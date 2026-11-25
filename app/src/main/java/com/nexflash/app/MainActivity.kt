package com.nexflash.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import com.nexflash.app.ui.NexFlashRoot
import com.nexflash.app.ui.theme.NexFlashTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NexFlashTheme {
                Surface { NexFlashRoot() }
            }
        }
    }
}
