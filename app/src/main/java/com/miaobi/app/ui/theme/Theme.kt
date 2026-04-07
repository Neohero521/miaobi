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
    primary = CaiYunPrimary,  // 彩云橙
    onPrimary = CaiYunOnPrimary,
    primaryContainer = Color(0xFFFFF3DB),
    onPrimaryContainer = CaiYunTextPrimary,

    secondary = CaiYunSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E8E8),
    onSecondaryContainer = CaiYunTextPrimary,

    tertiary = CaiYunAccent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE5E3),
    onTertiaryContainer = CaiYunTextPrimary,

    background = CaiYunBackground,
    onBackground = CaiYunTextPrimary,

    surface = CaiYunSurface,
    onSurface = CaiYunTextPrimary,
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = CaiYunTextSecondary,

    outline = Color(0xFFCCCCCC),
    outlineVariant = Color(0xFFE0E0E0),

    error = Error,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    // Extra for typewriter aesthetic
    inverseSurface = TypewriterNight,
    inverseOnSurface = TypewriterInkLight,
    inversePrimary = CaiYunPrimaryDark,
    surfaceTint = CaiYunPrimary,
)

// ─── Typewriter Dark Color Scheme ─────────────────────────────────────────────
private val TypewriterDarkColorScheme = darkColorScheme(
    primary = CaiYunPrimary,
    onPrimary = Color.White,
    primaryContainer = CaiYunPrimaryDark,
    onPrimaryContainer = Color.White,

    secondary = Color(0xFF2A2A2A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3A3A3A),
    onSecondaryContainer = Color.White,

    tertiary = CaiYunAccent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF8B2323),
    onTertiaryContainer = Color.White,

    background = Color(0xFF121212),
    onBackground = Color.White,

    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFAAAAAA),

    outline = Color(0xFF555555),
    outlineVariant = Color(0xFF3A3A3A),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690003),
    errorContainer = Color(0xFF930006),
    onErrorContainer = Color(0xFFFFDAD6),

    inverseSurface = Color(0xFFE0E0E0),
    inverseOnSurface = Color(0xFF1E1E1E),
    inversePrimary = CaiYunPrimaryDark,
    surfaceTint = CaiYunPrimary,
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
