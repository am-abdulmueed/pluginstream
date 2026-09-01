package com.lagradost.cloudstream3.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.lagradost.cloudstream3.R
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

        val content = if (type == "privacy") {
            binding.legalToolbar.title = "Privacy Policy"
            getPrivacyPolicyMarkdown()
        } else {
            binding.legalToolbar.title = "Terms & Conditions"
            getTermsMarkdown()
        }

        val markwon = Markwon.builder(requireContext())
            .usePlugin(LinkifyPlugin.create())
            .build()

        markwon.setMarkdown(binding.legalText, content)

        binding.legalToolbar.setNavigationOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        binding.legalToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_copy -> {
                    try {
                        val ctx = requireContext()
                        val clip = ClipData.newPlainText(
                            binding.legalToolbar.title?.toString() ?: "Legal",
                            content
                        )
                        (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                            ?.setPrimaryClip(clip)

                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                            val copied = ctx.getString(R.string.toast_copied)
                            val label = binding.legalToolbar.title?.toString() ?: ""
                            Toast.makeText(ctx, "$label $copied", Toast.LENGTH_SHORT).show()
                        }
                    } catch (t: Throwable) {
                        when (t) {
                            is android.os.TransactionTooLargeException -> {
                                Toast.makeText(requireContext(), R.string.clipboard_too_large, Toast.LENGTH_SHORT).show()
                            }
                            is SecurityException -> {
                                Toast.makeText(requireContext(), R.string.clipboard_permission_error, Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                Toast.makeText(requireContext(), R.string.clipboard_unknown_error, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    true
                }
                else -> false
            }
        }

        binding.legalFooter.text = "Last Edited: May 13, 2026\n© 2026 PluginStream"

        // Subtle fade-in as the document appears
        binding.legalText.alpha = 0f
        binding.legalText.animate().alpha(1f).setDuration(260).start()

        binding.legalFooter.alpha = 0f
        binding.legalFooter.animate().alpha(1f).setDuration(260).setStartDelay(80).start()
    }

    override fun fixLayout(view: View) {
        setSystemBarsPadding()
    }

    private fun getPrivacyPolicyMarkdown(): String {
        return """
# Privacy Policy

**Effective Date:** May 12, 2026

At **PluginStream**, accessible from https://pluginstream.pages.dev, protecting the privacy of our users is one of our main priorities. This Privacy Policy document outlines the types of information that is collected and recorded by PluginStream and how we use it.

### 1. Information We Collect
PluginStream is designed with a Privacy-First approach:
- **No Direct Personal Data:** We do not collect names, phone numbers, or physical addresses.
- **Account & Registration:** You are not required to create an account to use the main functionality of the application.
- **Local Device Storage:** All custom settings, module imports, and configuration data remain locally stored on your Android device.

### 2. Automatically Collected Usage Data & Diagnostics
To ensure application stability, monitor performance, and analyze usage trends, we utilize standard diagnostic infrastructure:
- **Firebase Analytics & Crash Reporting:** We collect anonymous device diagnostic data, operating system versions, and automated crash logs to fix bugs and optimize app performance.

### 3. Integrated Third-Party Services
PluginStream integrates trusted third-party SDKs for built-in utilities and gaming content:
- **PlayGama:** Powers the built-in mini-games section.
- **Google Firebase:** Used for crash diagnostics and operational performance monitoring.

We encourage users to review the respective privacy policies of these third-party services:
- [PlayGama Privacy Policy](https://playgama.com/confidential)

### 4. External Web Links & Modules
Our framework allows users to view external web content and custom modules. Please note that when you interact with third-party web services, their own terms and privacy policies will apply. PluginStream has no control over external third-party content.

### 5. Children's Privacy Protection
PluginStream does not knowingly collect any Personal Identifiable Information from children under the age of 13. If you believe your child provided such information, please contact us immediately, and we will promptly remove it.

### 6. Updates to This Policy
We may update our Privacy Policy from time to time. Any updates will be published directly on this page with an updated effective date.

### 7. Contact Us
If you have questions or require more information about our Privacy Policy, feel free to contact us:
- **Email:** am.abdulmueed1@gmail.com
- **Website Support:** https://pluginstream.pages.dev/contact
        """.trimIndent()
    }

    private fun getTermsMarkdown(): String {
        return """
# Terms & Conditions

**Effective Date:** May 12, 2026

By downloading or using **PluginStream** (https://pluginstream.pages.dev), you automatically agree to the following terms and conditions.

### 1. Disclaimer of Warranties & Software Usage
PluginStream operates strictly as a modular web utility framework and game dashboard. The software is provided "as is" without warranties of any kind, express or implied.

### 2. Third-Party Content & External Resources
- **No Media Hosting:** PluginStream does not host, upload, manage, or store digital media files on its servers.
- **User Responsibility:** Users are solely responsible for any external web modules or configurations they add into the app, and must comply with local copyright and data laws governing their region.

### 3. Third-Party Partners
By using built-in interactive features such as mini-games, you agree to comply with the terms of our feature providers:
- [PlayGama Terms of Use](https://playgama.com/termsofuse)

### 4. Limitation of Liability
In no event shall PluginStream, its developers, or affiliates be liable for any direct, indirect, incidental, or consequential damages resulting from the use or inability to use the application or external links.

### 5. Contact Information
For legal inquiries, feedback, or support regarding our terms, please reach out to us:
- **Email:** am.abdulmueed1@gmail.com
- **Support Portal:** https://pluginstream.pages.dev/contact
        """.trimIndent()
    }
}