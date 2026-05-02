package com.faster.note.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Blue700,
    onPrimary = NearWhite,
    primaryContainer = Blue100,
    onPrimaryContainer = BlueDark,
    secondary = GreySecondary,
    secondaryContainer = BlueGrey100,
    onSecondaryContainer = BlueDark,
    background = NearWhite,
    onBackground = DarkSurface,
    surface = NearWhite,
    onSurface = DarkSurface,
    error = ErrorRed,
)

private val DarkColorScheme = darkColorScheme(
    primary = BlueLight,
    onPrimary = BlueDark,
    primaryContainer = BlueDarkContainer,
    onPrimaryContainer = Blue100,
    secondary = GreySecondaryDark,
    secondaryContainer = GreySecondaryContainer,
    onSecondaryContainer = BlueGrey100,
    background = DarkSurface,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    error = ErrorRedDark,
)

@Composable
fun ScheduleAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val context = view.context
            if (context is Activity) {
                val window = context.window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
