package com.lagradost.cloudstream3.ui.more

import android.os.Bundle
import android.view.View
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentMoreBinding
import com.lagradost.cloudstream3.ui.BaseFragment
import com.lagradost.cloudstream3.ui.settings.Globals.isLandscape
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.EMULATOR
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding

class MoreFragment : BaseFragment<FragmentMoreBinding>(
    BaseFragment.BindingCreator.Inflate(FragmentMoreBinding::inflate)
) {
    override fun fixLayout(view: View) {
        fixSystemBarsPadding(
            view,
            padBottom = isLandscape(),
            padLeft = isLayout(TV or EMULATOR)
        )
    }

    override fun onBindingCreated(binding: FragmentMoreBinding) {
        // Offers click
        binding.btnOffers.setOnClickListener {
            val navOptions = androidx.navigation.NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setEnterAnim(R.anim.enter_anim)
                .setExitAnim(R.anim.exit_anim)
                .setPopEnterAnim(R.anim.pop_enter)
                .setPopExitAnim(R.anim.pop_exit)
                .setPopUpTo(R.id.navigation_home, inclusive = true, saveState = true)
                .build()
            findNavController().navigate(R.id.navigation_offers, null, navOptions)
        }

        // Library click
        binding.btnLibrary.setOnClickListener {
            val navOptions = androidx.navigation.NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setEnterAnim(R.anim.enter_anim)
                .setExitAnim(R.anim.exit_anim)
                .setPopEnterAnim(R.anim.pop_enter)
                .setPopExitAnim(R.anim.pop_exit)
                .setPopUpTo(R.id.navigation_home, inclusive = true, saveState = true)
                .build()
            findNavController().navigate(R.id.navigation_library, null, navOptions)
        }

        // Downloads click
        binding.btnDownloads.setOnClickListener {
            val navOptions = androidx.navigation.NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setEnterAnim(R.anim.enter_anim)
                .setExitAnim(R.anim.exit_anim)
                .setPopEnterAnim(R.anim.pop_enter)
                .setPopExitAnim(R.anim.pop_exit)
                .setPopUpTo(R.id.navigation_home, inclusive = true, saveState = true)
                .build()
            findNavController().navigate(R.id.navigation_downloads, null, navOptions)
        }

        // Settings click
        binding.btnSettings.setOnClickListener {
            val navOptions = androidx.navigation.NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setEnterAnim(R.anim.enter_anim)
                .setExitAnim(R.anim.exit_anim)
                .setPopEnterAnim(R.anim.pop_enter)
                .setPopExitAnim(R.anim.pop_exit)
                .setPopUpTo(R.id.navigation_home, inclusive = true, saveState = true)
                .build()
            findNavController().navigate(R.id.navigation_settings, null, navOptions)
        }
    }

    override fun onResume() {
        super.onResume()
        // If we're navigating back to MoreFragment from a sub-fragment,
        // make sure we're at the top of the More stack
        val currentDestination = findNavController().currentDestination?.id
        if (currentDestination != R.id.navigation_more) {
            // Pop back to MoreFragment if we're in a sub-fragment
            findNavController().popBackStack(R.id.navigation_more, false)
        }
    }
}
