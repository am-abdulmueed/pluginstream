package com.lagradost.cloudstream3.ui.game

import android.content.Context
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import android.view.View
import com.lagradost.cloudstream3.BuildConfig
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

/**
 * Singleton that preloads a Banner Ad at app startup so it is
 * instantly available when the game WebView (GamePlayerFragment) opens.
 *
 * Usage:
 *  1. Call [GameBannerAdManager.init(context)] once from Application.onCreate().
 *  2. In GamePlayerFragment.loadAdaptiveBanner(), call [GameBannerAdManager.attachToContainer(container, context)].
 */
object GameBannerAdManager {
    private const val TAG = "GameBannerAd"
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"

    private var preloadedAdView: AdView? = null
    private var preloadedBannerAd: BannerAd? = null
    private var isLoading = false
    private var appContext: Context? = null

    /**
     * Call once in Application.onCreate() to start preloading immediately.
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        preload(context)
    }

    /**
     * Preloads a banner ad using the current screen width.
     * Safe to call multiple times — skips if already loading or preloaded.
     */
    fun preload(context: Context, force: Boolean = false) {
        if (isLoading && !force) return
        if (preloadedBannerAd != null && !force) {
            Log.d(TAG, "Banner already preloaded — skipping.")
            return
        }

        // Destroy old AdView before creating a new one
        preloadedAdView?.destroy()
        preloadedAdView = null
        preloadedBannerAd = null

        isLoading = true
        appContext = context.applicationContext

        try {
            val ctx = context.applicationContext
            val displayMetrics = ctx.resources.displayMetrics
            val adWidthPixels = displayMetrics.widthPixels
            val density = displayMetrics.density
            val adWidth = (adWidthPixels / density).toInt()

            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidth)
            val adView = AdView(ctx)
            preloadedAdView = adView

            val bannerRequest = BannerAdRequest.Builder(BANNER_AD_UNIT_ID, adSize).build()

            adView.loadAd(
                bannerRequest,
                object : AdLoadCallback<BannerAd> {
                    override fun onAdLoaded(ad: BannerAd) {
                        isLoading = false
                        preloadedBannerAd = ad
                        Log.d(TAG, "Banner ad preloaded successfully ✅")
                        if (BuildConfig.DEBUG) showToast("Banner ad preloaded ✅", Toast.LENGTH_SHORT)

                        ad.adEventCallback = object : BannerAdEventCallback {
                            override fun onAdImpression() {
                                Log.d(TAG, "Banner impression recorded.")
                                // Preload the next banner in background after impression
                                appContext?.let { preload(it, force = true) }
                            }
                            override fun onAdClicked() {
                                Log.d(TAG, "Banner ad clicked.")
                            }
                        }

                        ad.bannerAdRefreshCallback = object : BannerAdRefreshCallback {
                            override fun onAdRefreshed() {
                                Log.d(TAG, "Banner ad refreshed.")
                                if (BuildConfig.DEBUG) showToast("Banner ad refreshed", Toast.LENGTH_SHORT)
                            }
                            override fun onAdFailedToRefresh(adError: LoadAdError) {
                                Log.w(TAG, "Banner failed to refresh: ${adError.message}")
                            }
                        }
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        isLoading = false
                        preloadedBannerAd = null
                        Log.w(TAG, "Banner ad failed to preload: ${adError.message}")
                    }
                }
            )
        } catch (e: Exception) {
            isLoading = false
            Log.e(TAG, "Exception during banner preload", e)
        }
    }

    /**
     * Attaches the preloaded AdView+BannerAd to the given FrameLayout container.
     * If no preloaded ad is ready, falls back to loading directly into a new AdView.
     *
     * @param container The FrameLayout in fragment_game_player.xml where the banner will be shown.
     * @param context   Fragment context for fallback loading.
     */
    fun attachToContainer(container: FrameLayout, context: Context) {
        val ad = preloadedBannerAd
        val adView = preloadedAdView

        if (ad != null && adView != null) {
            // Detach from old parent if any
            (adView.parent as? FrameLayout)?.removeView(adView)

            container.removeAllViews()
            container.addView(adView)
            container.visibility = View.VISIBLE

            Log.d(TAG, "Preloaded banner attached to container ✅")
            if (BuildConfig.DEBUG) showToast("Banner ad shown ✅", Toast.LENGTH_SHORT)

            // Mark consumed so next open triggers a fresh preload
            preloadedBannerAd = null
            preloadedAdView = null

            // Start preloading the next banner immediately
            appContext?.let { preload(it, force = true) }
        } else {
            // Fallback: load fresh directly into container
            Log.d(TAG, "No preloaded banner — falling back to direct load.")
            loadDirectlyInto(container, context)
        }
    }

    /**
     * Fallback: load a banner ad directly into a container without preloading.
     */
    private fun loadDirectlyInto(container: FrameLayout, context: Context) {
        try {
            val displayMetrics = context.resources.displayMetrics
            val adWidth = (displayMetrics.widthPixels / displayMetrics.density).toInt()
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
            val adView = AdView(context)
            val bannerRequest = BannerAdRequest.Builder(BANNER_AD_UNIT_ID, adSize).build()

            adView.loadAd(
                bannerRequest,
                object : AdLoadCallback<BannerAd> {
                    override fun onAdLoaded(ad: BannerAd) {
                        container.post {
                            container.removeAllViews()
                            container.addView(adView)
                            container.visibility = View.VISIBLE
                            if (BuildConfig.DEBUG) showToast("Banner ad loaded ✅", Toast.LENGTH_SHORT)
                            Log.d(TAG, "Fallback banner loaded.")
                        }
                        ad.adEventCallback = object : BannerAdEventCallback {
                            override fun onAdImpression() {
                                appContext?.let { preload(it, force = true) }
                            }
                            override fun onAdClicked() {}
                        }
                    }
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        container.post { container.visibility = View.GONE }
                        Log.w(TAG, "Fallback banner failed to load: ${adError.message}")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Fallback banner exception", e)
        }
    }
}
