package com.lagradost.cloudstream3.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.annotation.StringRes
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.lagradost.cloudstream3.BuildConfig
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.MainSettingsBinding
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.mvvm.safe
import com.lagradost.cloudstream3.syncproviders.AccountManager
import com.lagradost.cloudstream3.syncproviders.AuthRepo
import com.lagradost.cloudstream3.ui.BaseFragment
import com.lagradost.cloudstream3.ui.home.HomeFragment.Companion.errorProfilePic
import com.lagradost.cloudstream3.ui.settings.Globals.EMULATOR
import com.lagradost.cloudstream3.ui.settings.Globals.PHONE
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.isLandscape
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.lagradost.cloudstream3.utils.ImageLoader.loadImage
import com.lagradost.cloudstream3.utils.UIHelper.clipboardHelper
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding
import com.lagradost.cloudstream3.utils.UIHelper.navigate
import com.lagradost.cloudstream3.utils.UIHelper.toPx
import com.lagradost.cloudstream3.utils.getImageFromDrawable
import com.lagradost.cloudstream3.utils.txt
import com.lagradost.cloudstream3.ui.dialog.ContactDeveloperDialog
import android.app.Dialog
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SettingsFragment : BaseFragment<MainSettingsBinding>(
    BaseFragment.BindingCreator.Inflate(MainSettingsBinding::inflate)
) {
    companion object {
        fun PreferenceFragmentCompat?.getPref(id: Int): Preference? {
            if (this == null) return null
            return try {
                findPreference(getString(id))
            } catch (e: Exception) {
                logError(e)
                null
            }
        }

        /**
         * Hide many Preferences on selected layouts.
         **/
        fun PreferenceFragmentCompat?.hidePrefs(ids: List<Int>, layoutFlags: Int) {
            if (this == null) return

            try {
                ids.forEach {
                    getPref(it)?.isVisible = !isLayout(layoutFlags)
                }
            } catch (e: Exception) {
                logError(e)
            }
        }

        /**
         * Hide the [Preference] on selected layouts.
         * @return [Preference] if visible otherwise null.
         *
         * [hideOn] is usually followed by some actions on the preference which are mostly
         * unnecessary when the preference is disabled for the said layout thus returning null.
         **/
        fun Preference?.hideOn(layoutFlags: Int): Preference? {
            if (this == null) return null
            this.isVisible = !isLayout(layoutFlags)
            return if(this.isVisible) this else null
        }

        /**
         * On TV you cannot properly scroll to the bottom of settings, this fixes that.
         * */
        fun PreferenceFragmentCompat.setPaddingBottom() {
            if (isLayout(TV or EMULATOR)) {
                listView?.setPadding(0, 0, 0, 100.toPx)
            }
        }

        fun PreferenceFragmentCompat.setToolBarScrollFlags() {
            if (isLayout(TV or EMULATOR)) {
                val settingsAppbar = view?.findViewById<MaterialToolbar>(R.id.settings_toolbar)

                settingsAppbar?.updateLayoutParams<AppBarLayout.LayoutParams> {
                    scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
                }
            }
        }

        fun Fragment?.setToolBarScrollFlags() {
            if (isLayout(TV or EMULATOR)) {
                val settingsAppbar = this?.view?.findViewById<MaterialToolbar>(R.id.settings_toolbar)

                settingsAppbar?.updateLayoutParams<AppBarLayout.LayoutParams> {
                    scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
                }
            }
        }

        fun Fragment?.setUpToolbar(title: String) {
            if (this == null) return
            val settingsToolbar = view?.findViewById<MaterialToolbar>(R.id.settings_toolbar) ?: return

            settingsToolbar.apply {
                setTitle(title)
                if (isLayout(PHONE or EMULATOR)) {
                    setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
                    setNavigationOnClickListener {
                        activity?.onBackPressedDispatcher?.onBackPressed()
                    }
                }
            }
        }

        fun Fragment?.setUpToolbar(@StringRes title: Int) {
            if (this == null) return
            val settingsToolbar = view?.findViewById<MaterialToolbar>(R.id.settings_toolbar) ?: return

            settingsToolbar.apply {
                setTitle(title)
                if (isLayout(PHONE or EMULATOR)) {
                    setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
                    children.firstOrNull { it is ImageView }?.tag = getString(R.string.tv_no_focus_tag)
                    setNavigationOnClickListener {
                        safe { activity?.onBackPressedDispatcher?.onBackPressed() }
                    }
                }
            }
        }

        fun Fragment.setSystemBarsPadding() {
            view?.let {
                fixSystemBarsPadding(
                    it,
                    padLeft = isLayout(TV or EMULATOR),
                    padBottom = isLandscape()
                )
            }
        }

        fun getFolderSize(dir: File): Long {
            var size: Long = 0
            dir.listFiles()?.let {
                for (file in it) {
                    size += if (file.isFile) {
                        // System.out.println(file.getName() + " " + file.length());
                        file.length()
                    } else getFolderSize(file)
                }
            }

            return size
        }
    }

    override fun fixLayout(view: View) {
        fixSystemBarsPadding(
            view,
            padBottom = isLandscape(),
            padLeft = isLayout(TV or EMULATOR)
        )
    }

    override fun onBindingCreated(binding: MainSettingsBinding) {
        fun navigate(id: Int) {
            activity?.navigate(id, Bundle())
        }



        /** used to debug leaks
        showToast(activity,"${VideoDownloadManager.downloadStatusEvent.size} :
        ${VideoDownloadManager.downloadProgressEvent.size}") **/

        fun hasProfilePictureFromAccountManagers(accountManagers: Array<AuthRepo>): Boolean {
            for (syncApi in accountManagers) {
                val login = syncApi.authUser()
                val pic = login?.profilePicture ?: continue

                binding.settingsProfilePic.let { imageView ->
                    imageView.loadImage(pic) {
                        // Fallback to random error drawable
                        error { getImageFromDrawable(context ?: return@error null, errorProfilePic) }
                    }
                }
                binding.settingsProfileText.text = login.name
                return true // sync profile exists
            }
            return false // not syncing
        }

        // display local account information if not syncing
        if (!hasProfilePictureFromAccountManagers(AccountManager.allApis)) {
            val activity = activity ?: return
            val currentAccount = try {
                DataStoreHelper.accounts.firstOrNull {
                    it.keyIndex == DataStoreHelper.selectedKeyIndex
                } ?: activity.let { DataStoreHelper.getDefaultAccount(activity) }

            } catch (t: IllegalStateException) {
                Log.e("AccountManager", "Activity not found", t)
                null
            }

            binding.settingsProfilePic.loadImage(currentAccount?.image)
            binding.settingsProfileText.text = currentAccount?.name
        }

        binding.apply {
            listOf(
                settingsGeneral to R.id.action_navigation_global_to_navigation_settings_general,
                settingsPlayer to R.id.action_navigation_global_to_navigation_settings_player,
                settingsCredits to R.id.action_navigation_global_to_navigation_settings_account,
                settingsUi to R.id.action_navigation_global_to_navigation_settings_ui,
                settingsProviders to R.id.action_navigation_global_to_navigation_settings_providers,
                settingsUpdates to R.id.action_navigation_global_to_navigation_settings_updates,
                settingsExtensions to R.id.action_navigation_global_to_navigation_settings_extensions,
            ).forEach { (view, navigationId) ->
                view.apply {
                    setOnClickListener {
                        navigate(navigationId)
                    }
                    if (isLayout(TV)) {
                        isFocusable = true
                        isFocusableInTouchMode = true
                    }
                }
            }

            settingsRateApp.setOnClickListener {
                showReviewDialog()
            }

            settingsFaq.setOnClickListener {
                navigate(R.id.action_navigation_global_to_navigation_faq)
            }

            settingsGithub.setOnClickListener {
                try {
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = Uri.parse("https://github.com/am-abdulmueed")
                    startActivity(i)
                } catch (e: Exception) {
                    logError(e)
                }
            }

            settingsTelegram.setOnClickListener {
                try {
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = Uri.parse("https://t.me/pluginstreamofficial")
                    startActivity(i)
                } catch (e: Exception) {
                    logError(e)
                }
            }

            settingsInstagram.setOnClickListener {
                try {
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = Uri.parse("https://instagram.com/am.abdul.mueed")
                    startActivity(i)
                } catch (e: Exception) {
                    logError(e)
                }
            }

            settingsDevWebsite.setOnClickListener {
                try {
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = Uri.parse("https://am-abdulmueed.vercel.app")
                    startActivity(i)
                } catch (e: Exception) {
                    logError(e)
                }
            }

            settingsEmail.setOnClickListener {
                ContactDeveloperDialog().show(childFragmentManager, "ContactDeveloperDialog")
            }

            settingsShare.setOnClickListener {
                try {
                    val i = Intent(Intent.ACTION_SEND)
                    i.type = "text/plain"
                    i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_plugin))
                    i.putExtra(
                        Intent.EXTRA_TEXT,
                        "🚀 *Stream Unlimited Movies & Shows – Ad-Free!*\n\nExperience the next-gen *PluginStream Max.* No subscriptions, no hidden fees—just pure entertainment.\n\n🌐 *Download Now:* https://pluginstream.pages.dev\n🛡 *Join Official Community:* https://t.me/pluginstreamofficial (Official APK available here for easy download)\n\nEnjoy high-speed streaming without limits.\n\n❎ ~Netflix | Prime | HBO | Disney+~\n✅ *PluginStream Max* (Everything in ONE place!)"
                    )
                    startActivity(Intent.createChooser(i, getString(R.string.share_plugin)))
                } catch (e: Exception) {
                    logError(e)
                }
            }

            settingsPrivacyPolicy.setOnClickListener {
                  try {
                      val bundle = Bundle()
                      bundle.putString("type", "privacy")
                      activity?.navigate(R.id.action_navigation_global_to_navigation_legal, bundle)
                  } catch (e: Exception) {
                      logError(e)
                  }
              }
  
              settingsTermsConditions.setOnClickListener {
                  try {
                      val bundle = Bundle()
                      bundle.putString("type", "terms")
                      activity?.navigate(R.id.action_navigation_global_to_navigation_legal, bundle)
                  } catch (e: Exception) {
                      logError(e)
                  }
              }

            if (isLayout(TV)) {
                listOf(settingsGithub, settingsTelegram, settingsInstagram, settingsDevWebsite, settingsEmail, settingsShare, settingsPrivacyPolicy, settingsTermsConditions).forEach {
                    it.isFocusable = true
                    it.isFocusableInTouchMode = true
                }
            }

            // Default focus on TV
            if (isLayout(TV)) {
                settingsGeneral.requestFocus()
            }
        }

        val appVersion = BuildConfig.VERSION_NAME
        val commitInfo = getString(R.string.commit_hash)
        val buildTimestamp = SimpleDateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG,
            Locale.getDefault()
        ).apply { timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(BuildConfig.BUILD_DATE)).replace("UTC", "")

        binding.appVersion.text = appVersion
        binding.buildDate.text = buildTimestamp
        binding.appVersionInfo.setOnLongClickListener {
            clipboardHelper(txt(R.string.extension_version), "$appVersion $commitInfo $buildTimestamp")
            true
        }
    }

    private fun showReviewDialog() {
        val dialog = Dialog(requireContext(), R.style.DialogHalfFullscreen)
        val dialogView = layoutInflater.inflate(R.layout.dialog_review, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(true)

        val closeButton = dialogView.findViewById<ImageView>(R.id.closeButton)
        val apkPureButton = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.apkPureButton)
        val githubButton = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.githubButton)
        val productHuntButton = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.productHuntButton)
        val sourceForgeButton = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.sourceForgeButton)
        val productCoolButton = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.productCoolButton)

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        apkPureButton.setOnClickListener {
            openUrl("https://apkpure.com/reviews/com.betapix.pluginstream")
            dialog.dismiss()
        }

        githubButton.setOnClickListener {
            openUrl("https://github.com/am-abdulmueed/pluginstream")
            dialog.dismiss()
        }

        productHuntButton.setOnClickListener {
            openUrl("https://www.producthunt.com/p/pluginstream/pluginstream")
            dialog.dismiss()
        }

        sourceForgeButton.setOnClickListener {
            openUrl("https://sourceforge.net/projects/pluginstream/reviews/new?stars=5")
            dialog.dismiss()
        }

        productCoolButton.setOnClickListener {
            openUrl("https://www.productcool.com/product/pluginstream")
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            logError(e)
        }
    }
}