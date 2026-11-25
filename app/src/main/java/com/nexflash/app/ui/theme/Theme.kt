package com.nexflash.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class Accent(val label: String, val seed: Color) {
    Dynamic("Dynamic", Color(0xFF6750A4)),
    Violet("Violeta", Color(0xFF6750A4)),
    Blue("Azul", Color(0xFF0061A4)),
    Cyan("Cian", Color(0xFF006874)),
    Green("Verde", Color(0xFF386A20)),
    Orange("Naranja", Color(0xFF8B5000)),
    Pink("Rosa", Color(0xFF984061)),
}

@Composable
fun NexFlashTheme(accent: Accent = Accent.Dynamic, dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val scheme = when {
        accent == Accent.Dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme(primary = accent.seed, secondary = accent.seed, tertiary = accent.seed)
        else -> lightColorScheme(primary = accent.seed, secondary = accent.seed, tertiary = accent.seed)
    }
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
