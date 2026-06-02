package com.lagradost.cloudstream3.ui.flow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import com.lagradost.cloudstream3.ui.theme.CloudStreamComposeTheme
import com.lagradost.cloudstream3.ui.theme.isAppInDarkTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.aedev.flow.ui.FlowApp
import io.github.aedev.flow.ui.theme.CustomThemeColors
import io.github.aedev.flow.ui.theme.ThemeMode

@UnstableApi
@AndroidEntryPoint
class FlowFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CloudStreamComposeTheme {
                    val isDark = isAppInDarkTheme()
                    FlowApp(
                        currentTheme = if (isDark) ThemeMode.OLED else ThemeMode.LIGHT,
                        customThemeColors = CustomThemeColors.default(),
                        systemLightThemeMode = ThemeMode.LIGHT,
                        systemDarkThemeMode = ThemeMode.DARK,
                        onThemeChange = {},
                        onCustomThemeColorsChange = {},
                        onSystemLightThemeChange = {},
                        onSystemDarkThemeChange = {}
                    )
                }
            }
        }
    }
}
