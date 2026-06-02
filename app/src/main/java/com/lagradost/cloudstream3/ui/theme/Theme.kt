package com.lagradost.cloudstream3.ui.theme

import android.app.Activity
import android.os.Build
import android.util.TypedValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getActivity
import com.lagradost.cloudstream3.R
import io.github.aedev.flow.ui.theme.ExtendedColors
import io.github.aedev.flow.ui.theme.LocalExtendedColors

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
    val colorBackground = resolveColor(R.attr.primaryBlackBackground, if (darkTheme) Color.Black else Color.White)
    val colorSurfaceVariant = resolveColor(R.attr.boxItemBackground, if (darkTheme) Color(0xFF49454F) else Color(0xFFE7E0EC))
    val colorOnSurface = resolveColor(R.attr.textColor, if (darkTheme) Color.White else Color.Black)
    val colorGrayText = resolveColor(R.attr.grayTextColor, if (darkTheme) Color.Gray else Color.DarkGray)
    val colorDivider = resolveColor(R.attr.dividerColor, if (darkTheme) Color(0x1AFFFFFF) else Color(0x1A000000))

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme.copy(
            primary = colorPrimary,
            onPrimary = colorOnPrimary,
            surface = colorSurface,
            onSurface = colorOnSurface,
            background = colorBackground,
            onBackground = colorOnSurface,
            surfaceVariant = colorSurfaceVariant,
            onSurfaceVariant = colorGrayText,
            outline = colorDivider,
            outlineVariant = colorDivider
        )
        else -> LightColorScheme.copy(
            primary = colorPrimary,
            onPrimary = colorOnPrimary,
            surface = colorSurface,
            onSurface = colorOnSurface,
            background = colorBackground,
            onBackground = colorOnSurface,
            surfaceVariant = colorSurfaceVariant,
            onSurfaceVariant = colorGrayText,
            outline = colorDivider,
            outlineVariant = colorDivider
        )
    }

    val extendedColors = ExtendedColors(
        textSecondary = colorGrayText,
        border = colorDivider,
        success = Color(0xFF4CAF50) // Default green success color
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.getActivity()?.window
            if (window != null) {
                window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}