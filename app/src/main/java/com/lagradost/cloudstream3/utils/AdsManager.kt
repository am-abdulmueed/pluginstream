package com.lagradost.cloudstream3.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.DialogAdLoadingBinding

object AdsManager {
    private const val UNITY_GAME_ID = "6072815"
    private const val REWARDED_AD_PLACEMENT_ID = "Rewarded_Android"
    private const val BANNER_AD_PLACEMENT_ID = "Banner_Android"
    private const val INTERSTITIAL_AD_PLACEMENT_ID = "Interstitial_Android"
    private const val TEST_MODE = false

    private var bannerView: BannerView? = null
    private var pendingBannerRequest: (() -> Unit)? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private const val BANNER_REFRESH_INTERVAL = 30 * 1000L // 30 seconds

    private var currentBannerContainer: android.view.ViewGroup? = null
    private var currentActivity: Activity? = null

    private val refreshRunnable = object : Runnable {
        override fun run() {
            currentActivity?.let { activity ->
                currentBannerContainer?.let { container ->
                    loadBannerAd(activity, container)
                }
            }
            handler.postDelayed(this, BANNER_REFRESH_INTERVAL)
        }
    }

    fun initialize(context: Context) {
        UnityAds.initialize(context, UNITY_GAME_ID, TEST_MODE, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                // Preload rewarded and interstitial ads (doesn't need Activity context for load)
                preloadRewardedAd()
                preloadInterstitialAd()
                pendingBannerRequest?.invoke()
                pendingBannerRequest = null
            }

            override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
                // Handle initialization error
                pendingBannerRequest = null
            }
        })
    }

    private fun preloadRewardedAd() {
        UnityAds.load(REWARDED_AD_PLACEMENT_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {}
            override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {}
        })
    }

    private fun preloadInterstitialAd() {
        UnityAds.load(INTERSTITIAL_AD_PLACEMENT_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {}
            override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {}
        })
    }

    fun loadAndShowAdWithRetry(
        activity: Activity,
        placementId: String = REWARDED_AD_PLACEMENT_ID,
        loadingText: String,
        onAdFinished: (() -> Unit)? = null
    ) {
        // First check if ad is already ready to show
        // UnityAds.isReady is deprecated in newer SDKs, we use UnityAds.show directly or check state
        // For simplicity and 100% attempt, we show dialog and try to load/show.

        val dialog = Dialog(activity, R.style.CustomDialog)
        val dialogBinding = DialogAdLoadingBinding.inflate(activity.layoutInflater)
        dialogBinding.loadingText.text = loadingText
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(false)
        dialog.show()

        var retryCount = 0
        val maxRetries = 3

        // Use a single recursive function to handle load/show logic
        fun processAd(shouldLoad: Boolean) {
            if (shouldLoad) {
                UnityAds.load(placementId, object : IUnityAdsLoadListener {
                    override fun onUnityAdsAdLoaded(placementId: String?) {
                        processAd(false) // Try to show after load
                    }

                    override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                        retryCount++
                        if (retryCount < maxRetries) {
                            handler.postDelayed({ processAd(true) }, 1000)
                        } else {
                            activity.runOnUiThread {
                                dialog.dismiss()
                                onAdFinished?.invoke() // Fallback after 3 tries
                            }
                        }
                    }
                })
            } else {
                UnityAds.show(activity, placementId, object : IUnityAdsShowListener {
                    override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                        activity.runOnUiThread {
                            dialog.dismiss()
                            onAdFinished?.invoke()
                            // Preload for next time
                            if (placementId == REWARDED_AD_PLACEMENT_ID) preloadRewardedAd() else preloadInterstitialAd()
                        }
                    }

                    override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                        // If show fails, try to load it
                        processAd(true)
                    }

                    override fun onUnityAdsShowStart(placementId: String?) {}
                    override fun onUnityAdsShowClick(placementId: String?) {}
                })
            }
        }

        // Start by trying to show directly (if cached)
        processAd(false)
    }

    fun showInterstitialAd(activity: Activity, onAdFinished: (() -> Unit)? = null) {
        loadAndShowAdWithRetry(
            activity,
            INTERSTITIAL_AD_PLACEMENT_ID,
            loadingText = "Please wait, watch a short ad & continue",
            onAdFinished = onAdFinished
        )
    }

    fun showRewardedAd(activity: Activity, onAdShown: (() -> Unit)? = null) {
        loadAndShowAdWithRetry(
            activity,
            REWARDED_AD_PLACEMENT_ID,
            loadingText = "Please wait, watch a short ad & continue",
            onAdFinished = onAdShown
        )
    }

    fun initBannerAd(activity: Activity, bannerContainer: android.view.ViewGroup) {
        if (!UnityAds.isInitialized) {
            pendingBannerRequest = {
                activity.runOnUiThread {
                    initBannerAd(activity, bannerContainer)
                }
            }
            return
        }
        
        currentActivity = activity
        currentBannerContainer = bannerContainer
        
        handler.removeCallbacks(refreshRunnable)
        handler.post(refreshRunnable)
    }

    private fun loadBannerAd(activity: Activity, bannerContainer: android.view.ViewGroup) {
        if (bannerView == null) {
            bannerView = BannerView(activity, BANNER_AD_PLACEMENT_ID, UnityBannerSize.getDynamicSize(activity))
            val loadListener = object : BannerView.IListener {
                override fun onBannerLoaded(bannerAdView: BannerView?) {
                    bannerContainer.removeAllViews()
                    val params = android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                        android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.BOTTOM
                    )
                    bannerContainer.addView(bannerAdView, params)
                }
                override fun onBannerClick(bannerAdView: BannerView?) {}
                override fun onBannerFailedToLoad(bannerAdView: BannerView?, errorInfo: BannerErrorInfo?) {}
                override fun onBannerShown(bannerAdView: BannerView?) {}
                override fun onBannerLeftApplication(bannerView: BannerView?) {}
            }
            bannerView?.listener = loadListener
        }
        bannerView?.load()
    }

    fun destroyBannerAd() {
        handler.removeCallbacks(refreshRunnable)
        bannerView?.destroy()
        bannerView = null
        currentActivity = null
        currentBannerContainer = null
    }
}