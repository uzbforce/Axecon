package com.axecon.uzbforce.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.axecon.uzbforce.model.AppColorProfile
import com.axecon.uzbforce.model.ThemeMode

// Ocean Blue (Default)
private val DarkOceanColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF64748B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFF1F5F9),
    tertiary = Color(0xFFD97706),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFEF4444)
)

private val LightOceanColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEFF6FF),
    onPrimaryContainer = Color(0xFF1E40AF),
    secondary = Color(0xFF64748B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF1F5F9),
    onSecondaryContainer = Color(0xFF334155),
    tertiary = Color(0xFFD97706),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFE2E8F0),
    outlineVariant = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626)
)

// Emerald Green
private val DarkEmeraldColorScheme = darkColorScheme(
    primary = Color(0xFF10B981),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFF64748B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFF059669),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFF061A14),
    onBackground = Color(0xFFF0FDF4),
    surface = Color(0xFF0F291E),
    onSurface = Color(0xFFF0FDF4),
    surfaceVariant = Color(0xFF134E4A),
    onSurfaceVariant = Color(0xFF6EE7B7),
    outline = Color(0xFF047857),
    error = Color(0xFFEF4444)
)

private val LightEmeraldColorScheme = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = Color(0xFF0D9488),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),
    background = Color(0xFFF0FDF4),
    onBackground = Color(0xFF064E3B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF064E3B),
    surfaceVariant = Color(0xFFE6F4EA),
    onSurfaceVariant = Color(0xFF047857),
    outline = Color(0xFFA7F3D0),
    error = Color(0xFFDC2626)
)

// Neon Pink / Rose
private val DarkPinkColorScheme = darkColorScheme(
    primary = Color(0xFFF43F5E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF881337),
    onPrimaryContainer = Color(0xFFFFE4E6),
    secondary = Color(0xFFEC4899),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF831843),
    onSecondaryContainer = Color(0xFFFCE7F3),
    background = Color(0xFF1A0A12),
    onBackground = Color(0xFFFFF1F2),
    surface = Color(0xFF2A101D),
    onSurface = Color(0xFFFFF1F2),
    surfaceVariant = Color(0xFF4C1D36),
    onSurfaceVariant = Color(0xFFFDA4AF),
    outline = Color(0xFF9F1239),
    error = Color(0xFFEF4444)
)

private val LightPinkColorScheme = lightColorScheme(
    primary = Color(0xFFE11D48),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE4E6),
    onPrimaryContainer = Color(0xFF9F1239),
    secondary = Color(0xFFDB2777),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFCE7F3),
    onSecondaryContainer = Color(0xFF831843),
    background = Color(0xFFFFF1F2),
    onBackground = Color(0xFF4C0519),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF4C0519),
    surfaceVariant = Color(0xFFFFE4E6),
    onSurfaceVariant = Color(0xFFBE123C),
    outline = Color(0xFFFECDD3),
    error = Color(0xFFDC2626)
)

// Cyber Purple
private val DarkPurpleColorScheme = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF4C1D95),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFFA855F7),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF581C87),
    onSecondaryContainer = Color(0xFFF3E8FF),
    background = Color(0xFF0F0720),
    onBackground = Color(0xFFF5F3FF),
    surface = Color(0xFF1E1035),
    onSurface = Color(0xFFF5F3FF),
    surfaceVariant = Color(0xFF3B2064),
    onSurfaceVariant = Color(0xFFC4B5FD),
    outline = Color(0xFF6D28D9),
    error = Color(0xFFEF4444)
)

private val LightPurpleColorScheme = lightColorScheme(
    primary = Color(0xFF7C3AED),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF5B21B6),
    secondary = Color(0xFF9333EA),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF3E8FF),
    onSecondaryContainer = Color(0xFF6B21A8),
    background = Color(0xFFFAF5FF),
    onBackground = Color(0xFF2E1065),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2E1065),
    surfaceVariant = Color(0xFFF3E8FF),
    onSurfaceVariant = Color(0xFF7E22CE),
    outline = Color(0xFFDDD6FE),
    error = Color(0xFFDC2626)
)

// Sunset Amber
private val DarkAmberColorScheme = darkColorScheme(
    primary = Color(0xFFF59E0B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF78350F),
    onPrimaryContainer = Color(0xFFFEF3C7),
    secondary = Color(0xFFF97316),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF7C2D12),
    onSecondaryContainer = Color(0xFFFFEDD5),
    background = Color(0xFF180E03),
    onBackground = Color(0xFFFFFBEB),
    surface = Color(0xFF2A1907),
    onSurface = Color(0xFFFFFBEB),
    surfaceVariant = Color(0xFF4A2B0E),
    onSurfaceVariant = Color(0xFFFCD34D),
    outline = Color(0xFFB45309),
    error = Color(0xFFEF4444)
)

private val LightAmberColorScheme = lightColorScheme(
    primary = Color(0xFFD97706),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF92400E),
    secondary = Color(0xFFEA580C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFEDD5),
    onSecondaryContainer = Color(0xFF9A3412),
    background = Color(0xFFFFFBEB),
    onBackground = Color(0xFF451A03),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF451A03),
    surfaceVariant = Color(0xFFFEF3C7),
    onSurfaceVariant = Color(0xFFB45309),
    outline = Color(0xFFFDE68A),
    error = Color(0xFFDC2626)
)

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorProfile: AppColorProfile = AppColorProfile.OCEAN_BLUE,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        colorProfile.isDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            try {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } catch (e: Exception) {
                if (isDark) DarkOceanColorScheme else LightOceanColorScheme
            }
        }
        colorProfile == AppColorProfile.EMERALD_GREEN -> {
            if (isDark) DarkEmeraldColorScheme else LightEmeraldColorScheme
        }
        colorProfile == AppColorProfile.NEON_PINK -> {
            if (isDark) DarkPinkColorScheme else LightPinkColorScheme
        }
        colorProfile == AppColorProfile.CYBER_PURPLE -> {
            if (isDark) DarkPurpleColorScheme else LightPurpleColorScheme
        }
        colorProfile == AppColorProfile.SUNSET_AMBER -> {
            if (isDark) DarkAmberColorScheme else LightAmberColorScheme
        }
        else -> {
            if (isDark) DarkOceanColorScheme else LightOceanColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

