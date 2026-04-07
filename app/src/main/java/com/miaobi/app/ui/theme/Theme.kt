package com.miaobi.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Typewriter Light Color Scheme ────────────────────────────────────────────
private val TypewriterLightColorScheme = lightColorScheme(
    primary = TypewriterAmber,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF5E6B8),
    onPrimaryContainer = TypewriterInk,

    secondary = TypewriterGold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8D5A0),
    onSecondaryContainer = TypewriterInk,

    tertiary = TypewriterRust,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD4),
    onTertiaryContainer = TypewriterInk,

    background = TypewriterCream,
    onBackground = TypewriterInk,

    surface = TypewriterPaper,
    onSurface = TypewriterInk,
    surfaceVariant = TypewriterSurface,
    onSurfaceVariant = TypewriterFaded,

    outline = TypewriterHint,
    outlineVariant = Color(0xFFD4C9B8),

    error = Error,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    // Extra for typewriter aesthetic
    inverseSurface = TypewriterNight,
    inverseOnSurface = TypewriterInkLight,
    inversePrimary = TypewriterAmberDark,
    surfaceTint = TypewriterAmber,
)

// ─── Typewriter Dark Color Scheme ─────────────────────────────────────────────
private val TypewriterDarkColorScheme = darkColorScheme(
    primary = TypewriterAmberDark,
    onPrimary = Color(0xFF3D2E00),
    primaryContainer = Color(0xFF584400),
    onPrimaryContainer = Color(0xFFE8C84A),

    secondary = Color(0xFFD4B96A),
    onSecondary = Color(0xFF3D3000),
    secondaryContainer = Color(0xFF574600),
    onSecondaryContainer = Color(0xFFF2E2A0),

    tertiary = Color(0xFFFFB4A9),
    onTertiary = Color(0xFF5F1111),
    tertiaryContainer = Color(0xFF7E2721),
    onTertiaryContainer = Color(0xFFFFDAD4),

    background = TypewriterNight,
    onBackground = TypewriterInkLight,

    surface = TypewriterNightPaper,
    onSurface = TypewriterInkLight,
    surfaceVariant = TypewriterNightSurface,
    onSurfaceVariant = Color(0xFFD0C5B0),

    outline = Color(0xFF9A9080),
    outlineVariant = Color(0xFF4E4840),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690003),
    errorContainer = Color(0xFF930006),
    onErrorContainer = Color(0xFFFFDAD6),

    inverseSurface = TypewriterCream,
    inverseOnSurface = TypewriterInk,
    inversePrimary = TypewriterAmber,
    surfaceTint = TypewriterAmberDark,
)

// ─── Theme ─────────────────────────────────────────────────────────────────────

/**
 * 妙笔写作主题 — 打字机复古美学
 * 所有界面统一使用打字机风格色板
 */
@Composable
fun MiaobiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> TypewriterDarkColorScheme
        else -> TypewriterLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use primary amber color for status bar
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TypewriterTypography,
        content = content
    )
}
