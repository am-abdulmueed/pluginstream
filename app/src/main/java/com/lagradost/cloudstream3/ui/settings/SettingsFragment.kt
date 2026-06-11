package com.lagradost.cloudstream3.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.graphics.Typeface
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
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
import com.lagradost.cloudstream3.utils.GitInfo.currentCommitHash
import com.lagradost.cloudstream3.utils.ImageLoader.loadImage
import com.lagradost.cloudstream3.utils.UIHelper.clipboardHelper
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding
import com.lagradost.cloudstream3.utils.UIHelper.navigate
import com.lagradost.cloudstream3.utils.UIHelper.toPx
import com.lagradost.cloudstream3.utils.getImageFromDrawable
import com.lagradost.cloudstream3.utils.txt
import com.lagradost.cloudstream3.ui.dialog.ContactDeveloperDialog
import android.app.Dialog
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.widget.Toast
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
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
                settingsAccount to R.id.action_navigation_global_to_navigation_settings_account,
                settingsProviders to R.id.action_navigation_global_to_navigation_settings_providers,
                settingsExtensions to R.id.action_navigation_global_to_navigation_settings_extensions,
                settingsUi to R.id.action_navigation_global_to_navigation_settings_ui,
                settingsPlayer to R.id.action_navigation_global_to_navigation_settings_player,
                settingsUpdates to R.id.action_navigation_global_to_navigation_settings_updates,
                settingsAbout to R.id.action_navigation_global_to_navigation_developer,
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

            settingsBugReport.setOnClickListener {
                ContactDeveloperDialog().show(childFragmentManager, "ContactDeveloperDialog")
            }

            settingsFaq.setOnClickListener {
                activity?.navigate(R.id.action_navigation_global_to_navigation_faq)
            }

            settingsShare.setOnClickListener {
                try {
                    val i = Intent(Intent.ACTION_SEND)
                    i.type = "text/plain"
                    i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_plugin))
                    i.putExtra(
                        Intent.EXTRA_TEXT,
                        "🚀 *Stream Unlimited Movies & Shows – Ad-Free!*\n\nExperience the next-gen *PluginStream Max.* No subscriptions, no hidden fees—just pure entertainment.\n\n🌐 *Download Now:* https://PluginStream.pages.dev\n🛡 *Join Official Community:* https://t.me/PluginStreamofficial (Official APK available here for easy download)\n\nEnjoy high-speed streaming without limits.\n\n❎ ~Netflix | Prime | HBO | Disney+~\n✅ *PluginStream Max* (Everything in ONE place!)"
                    )
                    startActivity(Intent.createChooser(i, getString(R.string.share_plugin)))
                } catch (e: Exception) {
                    logError(e)
                }
            }

            settingsPrivacy.setOnClickListener {
                try {
                    val bundle = Bundle()
                    bundle.putString("type", "privacy")
                    activity?.navigate(R.id.action_navigation_global_to_navigation_legal, bundle)
                } catch (e: Exception) {
                    logError(e)
                }
            }

            settingsTerms.setOnClickListener {
                try {
                    val bundle = Bundle()
                    bundle.putString("type", "terms")
                    activity?.navigate(R.id.action_navigation_global_to_navigation_legal, bundle)
                } catch (e: Exception) {
                    logError(e)
                }
            }

            settingsRateApp.setOnClickListener {
                showReviewDialog()
            }

            if (isLayout(TV)) {
                listOf<View>(settingsRateApp).forEach {
                    it.isFocusable = true
                    it.isFocusableInTouchMode = true
                }
            }

            settingsSystemCompatibility.setOnClickListener {
                showSystemCompatibilityDialog()
            }

            settingsAppInfo.setOnClickListener {
                openAppSystemInfo()
            }

            // Default focus on TV
            if (isLayout(TV)) {
                settingsGeneral.requestFocus()
            }
        }
    }

    private fun showSystemCompatibilityDialog() {
        val context = context ?: return
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRamMb = memoryInfo.totalMem / (1024 * 1024)
        val availableRamMb = memoryInfo.availMem / (1024 * 1024)
        val path: File = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val availableBlocks = stat.availableBlocksLong
        val totalBlocks = stat.blockCountLong

        val totalStorageBytes = totalBlocks * blockSize
        val freeStorageBytes = availableBlocks * blockSize
        val deviceModel = Build.MODEL
        val androidVersion = Build.VERSION.RELEASE

        val hasMinTotalRam = totalRamMb >= 1500 // 1.5GB+ Total
        val hasMinAvailableRam = availableRamMb >= 200 // 200MB+ Available
        val hasMinStorage = freeStorageBytes >= 0.5 * 1024 * 1024 * 1024 // 500MB+ Free

        val isExcellent = totalRamMb >= 3500 && availableRamMb >= 1000 &&
            freeStorageBytes >= 2L * 1024 * 1024 * 1024

        val statusTitle = when {
            isExcellent -> "Excellent"
            hasMinTotalRam && hasMinAvailableRam && hasMinStorage -> "Compatible"
            else -> "Low Resources"
        }

        val statusMessage = when {
            isExcellent -> "Your device is powerful and should run the app very smoothly."
            hasMinTotalRam && hasMinAvailableRam && hasMinStorage -> "Your device meets the requirements and should run the app well."
            else -> "Low resources detected. You may notice lag or slower performance."
        }

        fun formatSize(bytes: Long): String {
            val gb = bytes / (1024.0 * 1024.0 * 1024.0)
            return if (gb >= 1) {
                String.format(Locale.US, "%.1f GB", gb)
            } else {
                val mb = bytes / (1024.0 * 1024.0)
                String.format(Locale.US, "%.0f MB", mb)
            }
        }

        fun dp(value: Int): Int =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value.toFloat(),
                context.resources.displayMetrics
            ).toInt()

        val cardBackground = MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorSurface,
            0
        )
        val dialogBackground = MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorSurface,
            0
        )
        val strokeColor = MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOutline,
            0
        )
        val titleColor = MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOnSurface,
            0
        )
        val bodyColor = MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOnSurface,
            0
        )
        val labelColor = MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOnSurfaceVariant,
            titleColor
        )

        fun createInfoCard(
            title: String,
            rows: List<Pair<String, String>>
        ): MaterialCardView {
            val contentLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }

            val titleView = TextView(context).apply {
                text = title
                textSize = 15f
                setTextColor(titleColor)
                setTypeface(typeface, Typeface.BOLD)
            }
            contentLayout.addView(titleView)

            rows.forEachIndexed { index, (label, value) ->
                if (index > 0) {
                    contentLayout.addView(View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            dp(1)
                        ).apply {
                            topMargin = dp(10)
                            bottomMargin = dp(10)
                        }
                        setBackgroundColor(strokeColor)
                        alpha = 0.12f
                    })
                } else {
                    titleView.layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(10)
                    }
                }

                contentLayout.addView(TextView(context).apply {
                    text = label
                    textSize = 11f
                    setTextColor(labelColor)
                    setTypeface(typeface, Typeface.BOLD)
                })

                contentLayout.addView(TextView(context).apply {
                    text = value
                    textSize = 14f
                    setTextColor(bodyColor)
                    setTypeface(typeface, Typeface.NORMAL)
                })
            }

            return MaterialCardView(context).apply {
                radius = dp(14).toFloat()
                strokeWidth = dp(1)
                setStrokeColor(strokeColor)
                setCardBackgroundColor(cardBackground)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(10)
                }
                setContentPadding(dp(14), dp(14), dp(14), dp(14))
                addView(contentLayout)
            }
        }

        val statusCard = MaterialCardView(context).apply {
            radius = dp(14).toFloat()
            strokeWidth = dp(1)
            setStrokeColor(strokeColor)
            setCardBackgroundColor(cardBackground)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setContentPadding(dp(14), dp(14), dp(14), dp(14))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(context).apply {
                    text = "Compatibility Status"
                    textSize = 15f
                    setTextColor(titleColor)
                    setTypeface(typeface, Typeface.BOLD)
                })
                addView(TextView(context).apply {
                    text = statusTitle
                    textSize = 16f
                    setTextColor(titleColor)
                    setTypeface(typeface, Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(10)
                    }
                })
                addView(TextView(context).apply {
                    text = statusMessage
                    textSize = 13f
                    setTextColor(bodyColor)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(6)
                    }
                })
                addView(TextView(context).apply {
                    text = "Recommended: 2 GB+ total RAM and 1 GB+ free space"
                    textSize = 11f
                    setTextColor(labelColor)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(10)
                    }
                })
            })
        }

        val dialogContent = ScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(dialogBackground)
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(2), dp(4), dp(2), dp(2))

                addView(
                    createInfoCard(
                        title = "Device Info",
                        rows = listOf(
                            "Model" to deviceModel,
                            "Android" to "OS $androidVersion"
                        )
                    )
                )

                addView(
                    createInfoCard(
                        title = "Memory and RAM",
                        rows = listOf(
                            "Total RAM" to formatSize(memoryInfo.totalMem),
                            "Available RAM" to formatSize(memoryInfo.availMem)
                        )
                    )
                )

                addView(
                    createInfoCard(
                        title = "Storage Info",
                        rows = listOf(
                            "Total Storage" to formatSize(totalStorageBytes),
                            "Free Space" to formatSize(freeStorageBytes)
                        )
                    )
                )

                addView(statusCard)
            })
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.system_compatibility)
            .setView(dialogContent)
            .setPositiveButton(R.string.close, null)
            .create()

        dialog.show()
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.84f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun openAppSystemInfo() {
        val context = context ?: return
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            showToast(activity, "Unable to open settings automatically.", Toast.LENGTH_SHORT)
        }
    }

    private fun showReviewDialog() {
        val dialog = Dialog(requireContext(), R.style.AlertDialogCustom)
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
        val saasHubButton = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.saasHubButton)

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
            openUrl("https://www.producthunt.com/products/pluginstream/reviews/new")
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

        saasHubButton.setOnClickListener {
            openUrl("https://www.saashub.com/pluginstream-reviews/new")
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
