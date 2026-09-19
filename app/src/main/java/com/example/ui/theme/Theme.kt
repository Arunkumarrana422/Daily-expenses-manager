package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
  primary = IndigoPrimary,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE8EAF6),
  onPrimaryContainer = IndigoPrimaryDark,
  secondary = TealSecondary,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFE0F2F1),
  onSecondaryContainer = Color(0xFF004D40),
  tertiary = FinanceSuccess,
  onTertiary = Color.White,
  error = FinanceError,
  onError = Color.White,
  errorContainer = Color(0xFFFFEBEE),
  onErrorContainer = Color(0xFFB71C1C),
  background = LightBackground,
  onBackground = LightPrimaryText,
  surface = LightSurface,
  onSurface = LightPrimaryText,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightSecondaryText,
  outline = LightDivider
)

private val DarkColorScheme = darkColorScheme(
  primary = DarkPrimary,
  onPrimary = Color(0xFF1A237E),
  primaryContainer = Color(0xFF283593),
  onPrimaryContainer = Color(0xFFC5CAE9),
  secondary = DarkSecondary,
  onSecondary = Color(0xFF004D40),
  secondaryContainer = Color(0xFF00695C),
  onSecondaryContainer = Color(0xFFB2DFDB),
  tertiary = DarkSuccess,
  onTertiary = Color(0xFF1B5E20),
  error = DarkError,
  onError = Color(0xFFB71C1C),
  errorContainer = Color(0xFF7F0000),
  onErrorContainer = Color(0xFFFFCDD2),
  background = DarkBackground,
  onBackground = DarkPrimaryText,
  surface = DarkSurface,
  onSurface = DarkPrimaryText,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkSecondaryText,
  outline = DarkDivider
)

@Composable
fun ExpenseManagerTheme(
  themeMode: String = "SYSTEM", // "LIGHT", "DARK", "SYSTEM"
  fontFamily: String = "DEFAULT",
  content: @Composable () -> Unit
) {
  val isDark = when (themeMode) {
    "LIGHT" -> false
    "DARK" -> true
    else -> isSystemInDarkTheme()
  }

  val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

