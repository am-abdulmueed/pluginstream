package com.lagradost.cloudstream3.ui.offers

import android.os.Bundle
import android.view.View
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentOffersBinding
import com.lagradost.cloudstream3.ui.BaseFragment
import com.lagradost.cloudstream3.ui.settings.Globals.isLandscape
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.EMULATOR
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding

class OffersFragment : BaseFragment<FragmentOffersBinding>(
    BaseFragment.BindingCreator.Inflate(FragmentOffersBinding::inflate)
) {
    override fun fixLayout(view: View) {
        fixSystemBarsPadding(
            view,
            padBottom = isLandscape(),
            padLeft = isLayout(TV or EMULATOR)
        )
    }

    override fun onBindingCreated(binding: FragmentOffersBinding) {
        // Empty offers screen - ready for future implementation
    }
}
