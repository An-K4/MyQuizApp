package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Primary color từ logo: #7B61FF (purple/violet)
 * 
 * Color scheme theo yêu cầu:
 * - Primary: #7B61FF
 * - OnPrimary: White (luôn trắng)
 * - Background: White (light) / Black (dark)
 * - OnBackground: Black (light) / White (dark)
 */

// Primary colors
val Primary = Color(0xFF7B61FF)
val OnPrimary = Color.White

// Light mode colors
val LightBackground = Color.White
val LightOnBackground = Color.Black

// Dark mode colors
val DarkBackground = Color.Black
val DarkOnBackground = Color.White

// Supporting colors (Material3 defaults)
val PrimaryContainer = Color(0xFFEADDFF)
val OnPrimaryContainer = Color(0xFF21005D)

val Secondary = Color(0xFF625B71)
val OnSecondary = Color.White
val SecondaryContainer = Color(0xFFE8DEF8)
val OnSecondaryContainer = Color(0xFF1D192B)

val Tertiary = Color(0xFF7D5260)
val OnTertiary = Color.White
val TertiaryContainer = Color(0xFFFFD8E4)
val OnTertiaryContainer = Color(0xFF31111D)

val Error = Color(0xFFB3261E)
val OnError = Color.White
val ErrorContainer = Color(0xFFF9DEDC)
val OnErrorContainer = Color(0xFF410E0B)

val Surface = Color(0xFFFFFBFE)
val OnSurface = Color(0xFF1C1B1F)
val SurfaceVariant = Color(0xFFE7E0EC)
val OnSurfaceVariant = Color(0xFF49454F)

val Outline = Color(0xFF79747E)
val OutlineVariant = Color(0xFFCAC4D0)

// Google button background
val GoogleButtonGray = Color(0xFFF5F5F5)  // Light gray for Google button
val GoogleButtonGrayDark = Color(0xFF2C2C2C)  // Dark gray for Google button in dark mode
