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
import com.lagradost.cloudstream3.utils.FirebaseHelper

object AdsManager {
    private const val UNITY_GAME_ID = "6072815"
    private const val REWARDED_AD_PLACEMENT_ID = "Rewarded_Android"
    private const val BANNER_AD_PLACEMENT_ID = "Banner_Android"
    private const val INTERSTITIAL_AD_PLACEMENT_ID = "Interstitial_Android"
    private const val TEST_MODE = false
    private const val ENABLE_ADS = false

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private const val BANNER_REFRESH_INTERVAL = 20 * 1000L // Reduced to 20 seconds for more impressions
    private const val BANNER_RETRY_INTERVAL = 3000L // 3 seconds retry as requested
    private const val PRELOAD_RETRY_INTERVAL = 5000L // 5 seconds for full-screen ads preload retry

    private var bannerView: BannerView? = null
    private var isBannerLoading = false
    private var pendingBannerRequest: (() -> Unit)? = null
    
    private var currentBannerContainer: android.view.ViewGroup? = null
    private var currentActivity: Activity? = null

    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (currentActivity != null && currentBannerContainer != null && !isBannerLoading) {
                loadBannerAd(currentActivity!!, currentBannerContainer!!)
            }
            handler.postDelayed(this, BANNER_REFRESH_INTERVAL)
        }
    }

    private val bannerRetryRunnable = Runnable {
        if (currentActivity != null && currentBannerContainer != null && UnityAds.isInitialized) {
            loadBannerAd(currentActivity!!, currentBannerContainer!!)
        }
    }

    fun initialize(context: Context) {
        if (!ENABLE_ADS) return
        if (UnityAds.isInitialized) {
            preloadRewardedAd()
            preloadInterstitialAd()
            return
        }

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
        if (!ENABLE_ADS || !UnityAds.isInitialized) return
        UnityAds.load(REWARDED_AD_PLACEMENT_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                // Ad loaded successfully
            }
            override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                // Fail! Retry after 5 seconds to ensure we have an ad ready
                // Only retry if still initialized to avoid leaks/crashes
                if (ENABLE_ADS && UnityAds.isInitialized) {
                    handler.removeCallbacksAndMessages(REWARDED_AD_PLACEMENT_ID)
                    handler.postDelayed({ preloadRewardedAd() }, REWARDED_AD_PLACEMENT_ID, PRELOAD_RETRY_INTERVAL)
                }
            }
        })
    }

    private fun preloadInterstitialAd() {
        if (!ENABLE_ADS || !UnityAds.isInitialized) return
        UnityAds.load(INTERSTITIAL_AD_PLACEMENT_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                // Ad loaded successfully
            }
            override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                // Fail! Retry after 5 seconds to ensure we have an ad ready
                if (ENABLE_ADS && UnityAds.isInitialized) {
                    handler.removeCallbacksAndMessages(INTERSTITIAL_AD_PLACEMENT_ID)
                    handler.postDelayed({ preloadInterstitialAd() }, INTERSTITIAL_AD_PLACEMENT_ID, PRELOAD_RETRY_INTERVAL)
                }
            }
        })
    }

    fun loadAndShowAdWithRetry(
        activity: Activity,
        placementId: String = REWARDED_AD_PLACEMENT_ID,
        loadingText: String,
        onAdFinished: (() -> Unit)? = null
    ) {
        if (!ENABLE_ADS) {
            onAdFinished?.invoke()
            return
        }
        val dialog = Dialog(activity, R.style.CustomDialog)
        val dialogBinding = DialogAdLoadingBinding.inflate(activity.layoutInflater)
        dialogBinding.loadingText.text = loadingText
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(false)
        
        var isFinished = false
        fun finish(callback: (() -> Unit)? = null) {
            if (isFinished) return
            isFinished = true
            activity.runOnUiThread {
                try {
                    if (dialog.isShowing && !activity.isFinishing) {
                        dialog.dismiss()
                    }
                } catch (e: Exception) {
                    // Ignore dismiss errors
                }
                callback?.invoke()
            }
        }

        dialog.show()

        // Safety timeout: 10 seconds max for the whole process
        handler.postDelayed({
            finish { onAdFinished?.invoke() }
        }, 10000)

        var retryCount = 0
        val maxRetries = 3

        // Use a single recursive function to handle load/show logic
        fun processAd(shouldLoad: Boolean) {
            if (isFinished) return
            
            if (shouldLoad) {
                UnityAds.load(placementId, object : IUnityAdsLoadListener {
                    override fun onUnityAdsAdLoaded(placementId: String?) {
                        processAd(false) // Try to show after load
                    }

                    override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                        retryCount++
                        if (retryCount < maxRetries && !isFinished) {
                            handler.postDelayed({ processAd(true) }, 1000)
                        } else {
                            finish { onAdFinished?.invoke() } // Fallback after 3 tries
                        }
                    }
                })
            } else {
                UnityAds.show(activity, placementId, object : IUnityAdsShowListener {
                    override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                        FirebaseHelper.logAdImpression(
                            when (placementId) {
                                REWARDED_AD_PLACEMENT_ID -> "rewarded"
                                INTERSTITIAL_AD_PLACEMENT_ID -> "interstitial"
                                else -> "unknown"
                            },
                            placementId ?: "unknown_ad_unit"
                        )
                        finish {
                            onAdFinished?.invoke()
                            // Preload for next time
                            if (placementId == REWARDED_AD_PLACEMENT_ID) preloadRewardedAd() else preloadInterstitialAd()
                        }
                    }

                    override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                        // If show fails, try to load it, but check retry count
                        retryCount++
                        if (retryCount < maxRetries && !isFinished) {
                            processAd(true)
                        } else {
                            finish { onAdFinished?.invoke() }
                        }
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
        if (!ENABLE_ADS) return
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
        if (!ENABLE_ADS || isBannerLoading) return
        isBannerLoading = true

        if (bannerView == null) {
            bannerView = BannerView(activity, BANNER_AD_PLACEMENT_ID, UnityBannerSize.getDynamicSize(activity))
            val loadListener = object : BannerView.IListener {
                override fun onBannerLoaded(bannerAdView: BannerView?) {
                    isBannerLoading = false
                    FirebaseHelper.logAdImpression("banner", BANNER_AD_PLACEMENT_ID)
                    // Success! Remove from retry queue if it was there
                    handler.removeCallbacks(bannerRetryRunnable)
                    
                    if (currentBannerContainer != null && bannerView != null) {
                        currentBannerContainer?.removeAllViews()
                        currentBannerContainer?.addView(bannerView)
                        currentBannerContainer?.visibility = android.view.View.VISIBLE
                    }
                }

                override fun onBannerFailedToLoad(bannerAdView: BannerView?, errorInfo: BannerErrorInfo?) {
                    isBannerLoading = false
                    // Hide the container on failure
                    currentBannerContainer?.visibility = android.view.View.GONE
                    
                    // Retry logic: Only retry if the container is still active and we're not already waiting
                    if (currentBannerContainer != null && UnityAds.isInitialized) {
                        handler.removeCallbacks(bannerRetryRunnable)
                        handler.postDelayed(bannerRetryRunnable, BANNER_RETRY_INTERVAL)
                    }
                }

                override fun onBannerClick(bannerAdView: BannerView?) {}
                override fun onBannerShown(bannerAdView: BannerView?) {}
                override fun onBannerLeftApplication(bannerAdView: BannerView?) {}
            }
            bannerView?.listener = loadListener
        }
        
        bannerView?.load()
    }

    fun destroyBannerAd() {
        handler.removeCallbacks(refreshRunnable)
        handler.removeCallbacks(bannerRetryRunnable)
        bannerView?.destroy()
        bannerView = null
        currentActivity = null
        currentBannerContainer = null
    }
}
