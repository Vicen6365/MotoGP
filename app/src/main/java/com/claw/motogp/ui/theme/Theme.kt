package com.claw.motogp.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// MotoGP Official Brand Colors (from motogp.com)
val MotoGPRed = Color(0xFFC80502)
val MotoGPBg = Color(0xFF0A0A0A)
val MotoGPSurface = Color(0xFF1A1A1A)
val MotoGPSurfaceVariant = Color(0xFF2A2A2A)
val MotoGPCard = Color(0xFF222222)
val MotoGPTextPrimary = Color(0xFFFFFFFF)
val MotoGPTextSecondary = Color(0xFFB0B0B0)
val MotoGPTextMuted = Color(0xFF707070)
val MotoGPAccent = Color(0xFFFF3B30)
val MotoGPSuccess = Color(0xFF00E676)
val MotoGPGold = Color(0xFFFFD700)
val MotoGPSilver = Color(0xFFC0C0C0)
val MotoGPBronze = Color(0xFFCD7F32)
val MotoGPProgressBg = Color(0xFF333333)

// Session type colors
val ColorPractice = Color(0xFF4FC3F7)
val ColorQualifying = Color(0xFFFF9800)
val ColorSprint = Color(0xFFE040FB)
val ColorRace = Color(0xFFC80502)
val ColorWarmUp = Color(0xFF81C784)

val MotoGPColorScheme = darkColorScheme(
    primary = MotoGPRed,
    onPrimary = Color.White,
    primaryContainer = MotoGPRed.copy(alpha = 0.2f),
    onPrimaryContainer = MotoGPRed,
    secondary = MotoGPSuccess,
    onSecondary = Color.Black,
    background = MotoGPBg,
    onBackground = MotoGPTextPrimary,
    surface = MotoGPSurface,
    onSurface = MotoGPTextPrimary,
    surfaceVariant = MotoGPSurfaceVariant,
    onSurfaceVariant = MotoGPTextSecondary,
    outline = MotoGPTextMuted
)
