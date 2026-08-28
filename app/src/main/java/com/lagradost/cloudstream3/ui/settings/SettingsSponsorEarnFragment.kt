package com.lagradost.cloudstream3.ui.settings

import android.content.Intent
import android.net.Uri
import android.view.View
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentSponsorEarnBinding
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.ui.BaseFragment
import com.lagradost.cloudstream3.ui.settings.Globals.EMULATOR
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.isLandscape
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.setSystemBarsPadding
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.setUpToolbar
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.linkify.LinkifyPlugin

class SettingsSponsorEarnFragment : BaseFragment<FragmentSponsorEarnBinding>(
    BaseFragment.BindingCreator.Inflate(FragmentSponsorEarnBinding::inflate)
) {

    override fun fixLayout(view: View) {
        fixSystemBarsPadding(
            view,
            padBottom = true,
            padLeft = isLayout(TV or EMULATOR)
        )
    }

    override fun onBindingCreated(binding: FragmentSponsorEarnBinding) {
        setUpToolbar(R.string.sponsor_earn_title)
        setSystemBarsPadding()

        val markwon = Markwon.builder(requireContext())
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(StrikethroughPlugin.create())
            .build()

        val markdownText = """
            ### 🎁 Pawns.app Referral Program Summary

            #### 👤 What YOU Get (Referrer)
            * 💵 **Total $3 Cash Bonus:**
              * **$1** when your friend makes their **1st payout**.
              * **$1** when your friend makes their **2nd payout**.
              * **$1** when your friend makes their **3rd payout**.

            * ♾️ **10% Lifetime Commission:** You earn **10%** of every single payout your friend makes in the future.

            ---

            #### 👥 What YOUR FRIEND Gets (Referred User)
            * 💵 **Total $3 Cash Bonus:**
              * **$1** — Instant signup bonus when joining through your link.
              * **$1** — Extra bonus on their **1st payout**.
              * **$1** — Extra bonus on their **2nd payout**.

            ---

            ### 📌 Quick Summary
            > **Both of you get $3 total in bonuses**, and **you get an extra 10% commission** on all of your friend's payouts forever!
        """.trimIndent()

        markwon.setMarkdown(binding.txtHowItWorksMarkdown, markdownText)

        binding.btnClaimNow.setOnClickListener {
            try {
                val referralUrl = "https://pawns.app/?r=20373004"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(referralUrl))
                startActivity(intent)
            } catch (e: Exception) {
                logError(e)
            }
        }
    }
}
