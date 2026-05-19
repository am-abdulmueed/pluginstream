package com.lagradost.cloudstream3.utils

import android.app.Activity
import android.content.Context

object AdsManager {
    private const val ENABLE_ADS = false

    fun initialize(context: Context) {
        // Ads removed to reduce app size
    }

    fun loadAndShowAdWithRetry(
        activity: Activity,
        placementId: String = "Rewarded_Android",
        loadingText: String,
        onAdFinished: (() -> Unit)? = null
    ) {
        onAdFinished?.invoke()
    }

    fun showInterstitialAd(activity: Activity, onAdFinished: (() -> Unit)? = null) {
        onAdFinished?.invoke()
    }

    fun showRewardedAd(activity: Activity, onAdShown: (() -> Unit)? = null) {
        onAdShown?.invoke()
    }

    fun initBannerAd(activity: Activity, bannerContainer: android.view.ViewGroup) {
        bannerContainer.visibility = android.view.View.GONE
    }

    fun destroyBannerAd() {
        // No-op
    }
}

