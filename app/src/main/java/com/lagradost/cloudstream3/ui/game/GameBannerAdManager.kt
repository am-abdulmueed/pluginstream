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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reusable banner ad manager that owns an independent preload pool for a single
 * [adUnitId]. Each instance keeps its own state so unrelated screens (e.g. Game
 * WebView vs Video Player) never contend for the same cached banner.
 *
 * Usage:
 *  1. Create one instance per ad-unit via a singleton wrapper (see below).
 *  2. Call [init] once from Application.onCreate().
 *  3. In the target Fragment, call [attachToContainer] to pop the preloaded banner
 *     into the screen's FrameLayout.
 */
class BannerAdManager(
    private val adUnitId: String,
    private val logTag: String,
) {
    private val preloadLock = Any()
    private var preloadedAdView: AdView? = null
    private var preloadedBannerAd: BannerAd? = null
    private val isLoading = AtomicBoolean(false)
    private var appContext: Context? = null

    /** Call once in Application.onCreate() to kick off the very first preload. */
    fun init(context: Context) {
        appContext = context.applicationContext
        preload(context)
    }

    /**
     * Preloads a banner using the current screen width. Safe to call multiple
     * times — skips if already loading or preloaded (unless [force] is true).
     */
    fun preload(context: Context, force: Boolean = false) {
        if (isLoading.get()) return
        if (preloadedBannerAd != null && !force) {
            Log.d(logTag, "Banner already preloaded — skipping.")
            return
        }

        synchronized(preloadLock) {
            if (isLoading.get()) return
            if (preloadedBannerAd != null && !force) return
            if (!isLoading.compareAndSet(false, true)) return

            preloadedAdView?.destroy()
            preloadedAdView = null
            preloadedBannerAd = null

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

                val bannerRequest = BannerAdRequest.Builder(adUnitId, adSize).build()

                adView.loadAd(
                    bannerRequest,
                    object : AdLoadCallback<BannerAd> {
                        override fun onAdLoaded(ad: BannerAd) {
                            isLoading.set(false)
                            preloadedBannerAd = ad
                            Log.d(logTag, "Banner ad preloaded successfully ✅")
                            if (BuildConfig.DEBUG) {
                                showToast("Banner ad preloaded ✅", Toast.LENGTH_SHORT)
                            }

                            ad.adEventCallback = object : BannerAdEventCallback {
                                override fun onAdImpression() {
                                    Log.d(logTag, "Banner impression recorded.")
                                    appContext?.let { preload(it, force = true) }
                                }
                                override fun onAdClicked() {
                                    Log.d(logTag, "Banner ad clicked.")
                                }
                            }

                            ad.bannerAdRefreshCallback = object : BannerAdRefreshCallback {
                                override fun onAdRefreshed() {
                                    Log.d(logTag, "Banner ad refreshed.")
                                    if (BuildConfig.DEBUG) {
                                        showToast("Banner ad refreshed", Toast.LENGTH_SHORT)
                                    }
                                }
                                override fun onAdFailedToRefresh(adError: LoadAdError) {
                                    Log.w(logTag, "Banner failed to refresh: ${adError.message}")
                                }
                            }
                        }

                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            isLoading.set(false)
                            preloadedBannerAd = null
                            Log.w(logTag, "Banner ad failed to preload: ${adError.message}")
                        }
                    }
                )
            } catch (e: Exception) {
                isLoading.set(false)
                Log.e(logTag, "Exception during banner preload", e)
            }
        }
    }

    /**
     * Attaches the preloaded AdView+BannerAd to the given FrameLayout container.
     * If no preloaded ad is ready, falls back to loading directly into a new
     * AdView. After consumption the cached state is cleared so the next screen
     * starts from a fresh preload pass.
     *
     * @param container FrameLayout in the screen layout where the banner renders.
     * @param context   Fragment/Activity context used for fallback loading.
     */
    fun attachToContainer(container: FrameLayout, context: Context) {
        val (ad, adView, ctxForPreload) = synchronized(preloadLock) {
            val a = preloadedBannerAd
            val v = preloadedAdView
            preloadedBannerAd = null
            preloadedAdView = null
            Triple(a, v, appContext)
        }

        if (ad != null && adView != null) {
            (adView.parent as? FrameLayout)?.removeView(adView)

            container.removeAllViews()
            container.addView(adView)
            container.visibility = View.VISIBLE

            Log.d(logTag, "Preloaded banner attached to container ✅")
            if (BuildConfig.DEBUG) {
                showToast("Banner ad shown ✅", Toast.LENGTH_SHORT)
            }

            ctxForPreload?.let { preload(it, force = true) }
        } else {
            Log.d(logTag, "No preloaded banner — falling back to direct load.")
            loadDirectlyInto(container, context)
        }
    }

    /** Fallback: load a banner ad directly into a container without preloading. */
    private fun loadDirectlyInto(container: FrameLayout, context: Context) {
        try {
            val displayMetrics = context.resources.displayMetrics
            val adWidth = (displayMetrics.widthPixels / displayMetrics.density).toInt()
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
            val adView = AdView(context)
            val bannerRequest = BannerAdRequest.Builder(adUnitId, adSize).build()

            adView.loadAd(
                bannerRequest,
                object : AdLoadCallback<BannerAd> {
                    override fun onAdLoaded(ad: BannerAd) {
                        container.post {
                            container.removeAllViews()
                            container.addView(adView)
                            container.visibility = View.VISIBLE
                            if (BuildConfig.DEBUG) {
                                showToast("Banner ad loaded ✅", Toast.LENGTH_SHORT)
                            }
                            Log.d(logTag, "Fallback banner loaded.")
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
                        Log.w(logTag, "Fallback banner failed to load: ${adError.message}")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(logTag, "Fallback banner exception", e)
        }
    }
}

/**
 * Singleton wrapper for the **Games** (WebView) banner slot.
 * Ad unit: ca-app-pub-3940256099942544/9214589741 (GMA test adaptive banner).
 *
 * Backward-compatible: old callers that used `GameBannerAdManager.init(...)`
 * and `GameBannerAdManager.attachToContainer(...)` continue to work unchanged.
 */
object GameBannerAdManager {
    private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
    private val delegate = BannerAdManager(BANNER_AD_UNIT_ID, "GameBannerAd")

    fun init(context: Context) = delegate.init(context)
    fun preload(context: Context, force: Boolean = false) = delegate.preload(context, force)
    fun attachToContainer(container: FrameLayout, context: Context) =
        delegate.attachToContainer(container, context)
}

/**
 * Singleton wrapper for the **Video Player** banner slot.
 * Uses a separate ad unit ID and an independent preload pool so that opening
 * the video player never steals the preloaded banner reserved for the games
 * screen (and vice-versa).
 *
 * Ad unit: ca-app-pub-3940256099942544/6300978111 (GMA test banner — swap with a
 * real production ad-unit ID when releasing).
 */
object VideoBannerAdManager {
    private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    private val delegate = BannerAdManager(BANNER_AD_UNIT_ID, "VideoBannerAd")

    fun init(context: Context) = delegate.init(context)
    fun preload(context: Context, force: Boolean = false) = delegate.preload(context, force)
    fun attachToContainer(container: FrameLayout, context: Context) =
        delegate.attachToContainer(container, context)
}
