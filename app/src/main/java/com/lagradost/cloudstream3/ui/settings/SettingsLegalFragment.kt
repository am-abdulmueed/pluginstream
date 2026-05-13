package com.lagradost.cloudstream3.ui.settings

import android.os.Bundle
import android.view.View
import com.lagradost.cloudstream3.databinding.FragmentLegalBinding
import com.lagradost.cloudstream3.ui.BaseFragment
import io.noties.markwon.Markwon
import io.noties.markwon.linkify.LinkifyPlugin

import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.setSystemBarsPadding

class SettingsLegalFragment : BaseFragment<FragmentLegalBinding>(
    BaseFragment.BindingCreator.Inflate(FragmentLegalBinding::inflate)
) {
    override fun onBindingCreated(binding: FragmentLegalBinding) {
        val type = arguments?.getString("type") ?: "privacy"
        
        binding.legalToolbar.setNavigationOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        val markwon = Markwon.builder(requireContext())
            .usePlugin(LinkifyPlugin.create())
            .build()

        if (type == "privacy") {
            binding.legalToolbar.title = "Privacy Policy"
            markwon.setMarkdown(binding.legalText, getPrivacyPolicyMarkdown())
        } else {
            binding.legalToolbar.title = "Terms & Conditions"
            markwon.setMarkdown(binding.legalText, getTermsMarkdown())
        }

        binding.legalFooter.text = "Last Edited: May 13, 2026\n© 2026 PluginStream"
    }

    override fun fixLayout(view: View) {
        setSystemBarsPadding()
    }

    private fun getPrivacyPolicyMarkdown(): String {
        return """
# Privacy Policy

**Effective Date:** May 12, 2026

At **PluginStream**, we take your privacy seriously. This policy explains how we handle your information.

### 1. Data Collection
PluginStream is designed with a **Privacy-First** approach:
- **No Personal Data:** We do not collect names, email addresses, or phone numbers.
- **No Data Selling:** Since we don't collect your personal data, we have nothing to sell to third parties.

### 2. Third-Party Services
We use a few trusted third-party services for app functionality and maintenance:
- **Advertising (OGADS, CPALEAD, CPAGRIP):** These services may use your **IP Address** to show relevant offers. No other personal data is shared.
- **Analytics & Stability (Firebase):** We use Firebase to monitor **Daily Active Users (DAU)** and receive **Crash Reports** to keep the app stable.
- **Games (PLAYGAMA):** Our games section is powered by PlayGama.
- **Video Content (YouTube):** ProTube features fetch content directly from YouTube.

### 3. Third-Party Links
For more details, you can visit the privacy policies of our partners:
- [CPALead Privacy](https://www.cpalead.com/en/policy)
- [OGAds Privacy](https://ogads.com/privacy-policy)
- [CPAGrip Privacy](https://www.cpagrip.com/privacy.php)
- [PlayGama Confidentiality](https://playgama.com/confidential)

### 4. Contact Us
If you have any questions about this policy, feel free to contact us via the support channels in Settings.
        """.trimIndent()
    }

    private fun getTermsMarkdown(): String {
        return """
# Terms & Conditions

**Effective Date:** May 12, 2026

By using **PluginStream**, you agree to the following terms and conditions.

### 1. Content Disclaimer
- **Third-Party Plugins:** PluginStream is a modular media player. All content is fetched from original websites through community-developed plugins.
- **Copyright:** We do not host or upload any media. Content availability depends on the source sites. Users are responsible for complying with local laws regarding the use of such content.
- **ProTube:** ProTube fetches content directly from YouTube's website for playback.

### 2. Use of Third-Party Services
By using certain features, you agree to the terms of our partners:
- [CPALead Terms](https://www.cpalead.com/en/terms)
- [OGAds Terms](https://tracking.ogmobi.com/terms)
- [CPAGrip Terms](https://www.cpagrip.com/terms.php)
- [PlayGama Terms of Use](https://playgama.com/termsofuse)

### 3. Limitation of Liability
PluginStream is provided "as is" without any warranties. We are not responsible for the content provided by third-party plugins or external websites.

### 4. Changes to Terms
We reserve the right to update these terms at any time. Continued use of the app constitutes acceptance of the new terms.
        """.trimIndent()
    }
}
