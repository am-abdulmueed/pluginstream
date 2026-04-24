package com.lagradost.cloudstream3.ui.game

import android.os.Bundle
import android.view.View
import com.lagradost.cloudstream3.databinding.FragmentGameBinding
import com.lagradost.cloudstream3.ui.BaseFragment
import com.lagradost.cloudstream3.ui.settings.Globals.isLandscape
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.EMULATOR
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding

class GameFragment : BaseFragment<FragmentGameBinding>(
    BaseFragment.BindingCreator.Inflate(FragmentGameBinding::inflate)
) {
    override fun fixLayout(view: View) {
        fixSystemBarsPadding(
            view,
            padBottom = isLandscape(),
            padLeft = isLayout(TV or EMULATOR)
        )
    }

    override fun onBindingCreated(binding: FragmentGameBinding) {
        // Placeholder for game logic
    }
}