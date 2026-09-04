package com.lagradost.cloudstream3.ui.game

import android.app.Activity
import android.util.Log
import com.lagradost.cloudstream3.BuildConfig
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import android.widget.Toast
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages interstitial ads for unlocked (non-locked) games.
 * Shows an interstitial every [COOLDOWN_PLAYS] game launches.
 */
object GameInterstitialAdManager {
    private const val TAG = "GameInterstitialAd"

    // Test interstitial ad unit ID
    const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    // How many game plays before showing an interstitial
    private const val COOLDOWN_PLAYS = 3

    private val playCount = AtomicInteger(0)
    private val preloadStarted = AtomicBoolean(false)

    private val preloadCallback = object : PreloadCallback {
        override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
            Log.d(TAG, "Interstitial ad preloaded for $preloadId.")
            if (BuildConfig.DEBUG) showToast("Interstitial ad preloaded ✅", Toast.LENGTH_SHORT)
        }

        override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
            Log.w(TAG, "Interstitial ad failed to preload: ${adError.message}")
        }

        override fun onAdsExhausted(preloadId: String) {
            Log.d(TAG, "Interstitial ads exhausted for $preloadId.")
        }
    }

    /**
     * Start preloading interstitial ads. Call once on game screen entry or after an ad is shown.
     */
    fun startPreload(force: Boolean = false) {
        if (!preloadStarted.compareAndSet(false, true) && !force) return
        try {
            val adRequest = AdRequest.Builder(AD_UNIT_ID).build()
            val preloadConfig = PreloadConfiguration(adRequest)
            InterstitialAdPreloader.start(AD_UNIT_ID, preloadConfig, preloadCallback)
            Log.d(TAG, "InterstitialAdPreloader started.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start InterstitialAdPreloader", e)
        }
    }

    /**
     * Call this when a non-locked game is launched.
     * Every [COOLDOWN_PLAYS] calls will trigger an interstitial ad.
     */
    fun recordPlayAndShowIfDue(activity: Activity) {
        val count = playCount.incrementAndGet()
        Log.d(TAG, "Game play count: $count")
        if (count % COOLDOWN_PLAYS == 0) {
            showInterstitial(activity)
        }
    }

    private fun showInterstitial(activity: Activity) {
        activity.runOnUiThread {
            if (BuildConfig.DEBUG) showToast("Interstitial ad loading...", Toast.LENGTH_SHORT)
            try {
                if (InterstitialAdPreloader.isAdAvailable(AD_UNIT_ID)) {
                    val ad = InterstitialAdPreloader.pollAd(AD_UNIT_ID)
                    if (ad != null) {
                        AppOpenAdManager.isOtherAdShowing = true
                        ad.adEventCallback = object : InterstitialAdEventCallback {
                            override fun onAdImpression() {
                                Log.d(TAG, "Interstitial ad recorded an impression.")
                                Thread { startPreload(force = true) }.start()
                            }

                            override fun onAdPaid(value: AdValue) {
                                Log.d(TAG, "Interstitial onPaid: ${value.valueMicros} ${value.currencyCode}")
                            }

                            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                                Log.w(TAG, "Interstitial failed to show: $error")
                                AppOpenAdManager.isOtherAdShowing = false
                                AppOpenAdManager.lastOtherAdDismissedTime = System.currentTimeMillis()
                                if (BuildConfig.DEBUG) showToast("Interstitial ad failed to show", Toast.LENGTH_SHORT)
                            }

                            override fun onAdDismissedFullScreenContent() {
                                Log.d(TAG, "Interstitial dismissed.")
                                AppOpenAdManager.isOtherAdShowing = false
                                AppOpenAdManager.lastOtherAdDismissedTime = System.currentTimeMillis()
                                if (BuildConfig.DEBUG) showToast("Interstitial ad dismissed", Toast.LENGTH_SHORT)
                            }
                        }
                        ad.show(activity)
                        return@runOnUiThread
                    }
                }
                // No preloaded ad available — try on-demand load
                loadAndShowOnDemand(activity)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show interstitial", e)
            }
        }
    }

    private fun loadAndShowOnDemand(activity: Activity) {
        try {
            val adRequest = AdRequest.Builder(AD_UNIT_ID).build()
            InterstitialAd.load(
                adRequest,
                object : com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback<InterstitialAd> {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        activity.runOnUiThread {
                            AppOpenAdManager.isOtherAdShowing = true
                            ad.adEventCallback = object : InterstitialAdEventCallback {
                                override fun onAdImpression() {
                                    Log.d(TAG, "On-demand interstitial impression.")
                                    Thread { startPreload(force = true) }.start()
                                }
                                override fun onAdPaid(value: AdValue) {}
                                override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                                    Log.w(TAG, "On-demand interstitial failed to show: $error")
                                    AppOpenAdManager.isOtherAdShowing = false
                                    AppOpenAdManager.lastOtherAdDismissedTime = System.currentTimeMillis()
                                    if (BuildConfig.DEBUG) showToast("Interstitial ad failed to show", Toast.LENGTH_SHORT)
                                }
                                override fun onAdDismissedFullScreenContent() {
                                    Log.d(TAG, "On-demand interstitial dismissed.")
                                    AppOpenAdManager.isOtherAdShowing = false
                                    AppOpenAdManager.lastOtherAdDismissedTime = System.currentTimeMillis()
                                    if (BuildConfig.DEBUG) showToast("Interstitial ad dismissed", Toast.LENGTH_SHORT)
                                }
                            }
                            ad.show(activity)
                        }
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.w(TAG, "On-demand interstitial failed to load: ${adError.message}")
                        if (BuildConfig.DEBUG) showToast("Interstitial ad not ready", Toast.LENGTH_SHORT)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load on-demand interstitial", e)
        }
    }
}
