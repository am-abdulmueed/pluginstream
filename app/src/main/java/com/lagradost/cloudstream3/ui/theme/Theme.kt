package com.lagradost.cloudstream3.ui.theme

import android.app.Activity
import android.os.Build
import android.util.TypedValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.R

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun isAppInDarkTheme(): Boolean {
    val context = LocalContext.current
    val settingsManager = PreferenceManager.getDefaultSharedPreferences(context)
    val theme = settingsManager.getString(context.getString(R.string.app_theme_key), "AmoledLight")
    return when (theme) {
        "Black", "Amoled", "AmoledLight", "Dracula", "SilentBlue", "Monet" -> true
        "Light", "Lavender" -> false
        "System" -> isSystemInDarkTheme()
        else -> true // Default to dark
    }
}

@Composable
fun CloudStreamComposeTheme(
    darkTheme: Boolean = isAppInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Default to false to respect app themes
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // Extract colors from XML theme to match app theme
    val typedValue = TypedValue()
    val theme = context.theme
    
    fun resolveColor(attr: Int, default: Color): Color {
        return if (theme.resolveAttribute(attr, typedValue, true)) {
            Color(typedValue.data)
        } else {
            default
        }
    }

    val colorPrimary = resolveColor(R.attr.colorPrimary, if (darkTheme) Purple80 else Purple40)
    val colorOnPrimary = resolveColor(context.resources.getIdentifier("colorOnPrimary", "attr", context.packageName), if (darkTheme) Color.Black else Color.White)
    val colorSurface = resolveColor(R.attr.primaryGrayBackground, if (darkTheme) Color(0xFF1C1B1F) else Color(0xFFFFFBFE))
    val colorSurfaceVariant = resolveColor(R.attr.boxItemBackground, if (darkTheme) Color(0xFF49454F) else Color(0xFFE7E0EC))
    val colorOnSurface = resolveColor(R.attr.textColor, if (darkTheme) Color.White else Color.Black)

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme.copy(
            primary = colorPrimary,
            onPrimary = colorOnPrimary,
            surface = colorSurface,
            onSurface = colorOnSurface,
            surfaceVariant = colorSurfaceVariant,
            onSurfaceVariant = colorOnSurface
        )
        else -> LightColorScheme.copy(
            primary = colorPrimary,
            onPrimary = colorOnPrimary,
            surface = colorSurface,
            onSurface = colorOnSurface,
            surfaceVariant = colorSurfaceVariant,
            onSurfaceVariant = colorOnSurface
        )
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}