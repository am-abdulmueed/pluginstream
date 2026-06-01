package com.lagradost.cloudstream3.ui.flow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
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
                FlowApp(
                    currentTheme = ThemeMode.SYSTEM,
                    customThemeColors = CustomThemeColors.default(),
                    onThemeChange = {},
                    onCustomThemeColorsChange = {}
                )
            }
        }
    }
}
