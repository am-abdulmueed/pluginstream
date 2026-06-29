package com.lagradost.cloudstream3.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.lagradost.cloudstream3.BuildConfig
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentDeveloperBinding
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.ui.BaseFragment
import com.lagradost.cloudstream3.ui.dialog.ContactDeveloperDialog
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.setUpToolbar
import com.lagradost.cloudstream3.ui.settings.Globals.EMULATOR
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.isLandscape
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.utils.UIHelper.clipboardHelper
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding
import com.lagradost.cloudstream3.utils.UIHelper.navigate
import com.lagradost.cloudstream3.utils.txt
import com.lagradost.cloudstream3.utils.GitInfo.currentCommitHash
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

import java.time.LocalDate
import java.time.Period

class SettingsDeveloperFragment : BaseFragment<FragmentDeveloperBinding>(
    BaseFragment.BindingCreator.Inflate(FragmentDeveloperBinding::inflate)
) {
    private fun calculateAge(birthDateStr: String): Int {
        return try {
            val parts = birthDateStr.split("-")
            val day = parts[0].toInt()
            val month = parts[1].toInt()
            val year = parts[2].toInt()

            val birthDate = LocalDate.of(year, month, day)
            val currentDate = LocalDate.now()

            Period.between(birthDate, currentDate).years
        } catch (e: Exception) {
            19
        }
    }

    override fun fixLayout(view: View) {
        fixSystemBarsPadding(
            view,
            padBottom = isLandscape(),
            padLeft = isLayout(TV or EMULATOR)
        )
    }

    override fun onBindingCreated(binding: FragmentDeveloperBinding) {
        setUpToolbar(R.string.dev_profile)

        binding.apply {
            fun openUrl(url: String?) {
                if (url.isNullOrBlank()) return
                try {
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = Uri.parse(url.trim().removeSurrounding("`"))
                    startActivity(i)
                } catch (e: Exception) {
                    logError(e)
                }
            }

            CommonActivity.getSocialLinks { json ->
                val handles = json?.optJSONArray("social_handles")
                if (handles != null) {
                    for (i in 0 until handles.length()) {
                        val handle = handles.getJSONObject(i)
                        val platform = handle.optString("platform")
                        val url = handle.optString("url")
                        
                        when (platform.lowercase()) {
                            "instagram" -> settingsInstagram.setOnClickListener { openUrl(url) }
                            "telegram" -> settingsTelegram.setOnClickListener { openUrl(url) }
                        }
                    }
                } else {
                    // Fallbacks
                    settingsTelegram.setOnClickListener {
                        openUrl("https://t.me/pluginstreamofficial")
                    }
                    settingsInstagram.setOnClickListener {
                        openUrl("https://instagram.com/am.abdul.mueed")
                    }
                }
            }

            settingsGithub.setOnClickListener {
                openUrl("https://github.com/am-abdulmueed")
            }

            settingsDevWebsite.setOnClickListener {
                openUrl("https://am-abdulmueed.vercel.app")
            }

            settingsEmail.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_SENDTO)
                    intent.data = Uri.parse("mailto:am.abdulmueed3@gmail.com")
                    startActivity(intent)
                } catch (e: Exception) {
                    logError(e)
                }
            }

            // Update dynamic age in philosophy/bio with Bullet Points
            val age = calculateAge("21-07-2006")
            val bioPoints = "🚀 I'm a $age-year-old developer crafting high-performance, ad-free multimedia experiences.\n\n" +
                           "🧠 Architecting modular, extension-driven apps with seamless cross-platform scaling.\n\n" +
                           "⚡ Obsessed with optimization—replacing bloated frameworks with lightweight, lightning-fast tools."
            devPhilosophy.text = bioPoints
            devPhilosophy.setLineSpacing(8f, 1f) // Use setLineSpacing instead of property assignment

            val appVersionStr = BuildConfig.VERSION_NAME
            val commitInfo = activity?.currentCommitHash() ?: ""
            val buildTimestamp = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            ).apply { timeZone = TimeZone.getTimeZone("UTC") }
                .format(Date(BuildConfig.BUILD_DATE))

            appVersion.text = "Version $appVersionStr"
            commitHash.text = commitInfo
            buildDate.text = buildTimestamp
            appVersionInfo.setOnLongClickListener {
                clipboardHelper(
                    txt(R.string.extension_version),
                    "$appVersionStr $commitInfo $buildTimestamp"
                )
                true
            }
        }
    }
}