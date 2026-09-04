package com.lagradost.cloudstream3.ui.game

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.lagradost.cloudstream3.BuildConfig
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

object GameRewardedAdManager {
    private const val TAG = "GameRewardedAd"

    // Sample AdMob App ID & Test Rewarded Ad Unit ID
    const val APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private val isInitialized = AtomicBoolean(false)
    private val isPreloadStarted = AtomicBoolean(false)
    private var isPreloaded = false
    private var appContext: Context? = null

    // Track games unlocked during this app session
    private val unlockedGameKeys = Collections.synchronizedSet(mutableSetOf<String>())

    private val preloadCallback = object : PreloadCallback {
        override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
            isPreloaded = true
            Log.d(TAG, "Rewarded ad preloaded successfully for $preloadId.")
            if (BuildConfig.DEBUG) showToast("Rewarded ad preloaded ✅", Toast.LENGTH_SHORT)
        }

        override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
            isPreloaded = false
            Log.w(TAG, "Rewarded ad failed to preload: ${adError.message}")
        }

        override fun onAdsExhausted(preloadId: String) {
            isPreloaded = false
            Log.d(TAG, "Rewarded ads exhausted for $preloadId.")
        }
    }

    /**
     * Initialize MobileAds SDK and start preloading rewarded ads for games.
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        if (!isInitialized.compareAndSet(false, true)) return

        try {
            val initConfig = InitializationConfig.Builder(APP_ID).build()
            MobileAds.initialize(context, initConfig) {
                Log.d(TAG, "GMA MobileAds initialized successfully.")
                startPreload(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize GMA MobileAds", e)
        }
    }

    /**
     * Start preloading rewarded ads using RewardedAdPreloader.
     */
    fun startPreload(context: Context, force: Boolean = false) {
        appContext = context.applicationContext
        if (!isPreloadStarted.compareAndSet(false, true) && !force) return
        try {
            val adRequest = AdRequest.Builder(AD_UNIT_ID).build()
            val preloadConfig = PreloadConfiguration(adRequest)
            RewardedAdPreloader.start(AD_UNIT_ID, preloadConfig, preloadCallback)
            Log.d(TAG, "RewardedAdPreloader started.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start RewardedAdPreloader", e)
        }
    }

    /**
     * Check if a preloaded rewarded ad is available.
     */
    fun isAdAvailable(): Boolean {
        return try {
            RewardedAdPreloader.isAdAvailable(AD_UNIT_ID) || isPreloaded
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if a specific game is locked.
     * Locked if inGamePurchases is "Yes" and not yet unlocked in current session.
     */
    fun isGameLocked(game: GameModel): Boolean {
        val hasLock = game.inGamePurchases?.trim()?.equals("Yes", ignoreCase = true) == true
        if (!hasLock) return false
        val key = getGameKey(game)
        return !unlockedGameKeys.contains(key)
    }

    /**
     * Mark a game as unlocked for the session.
     */
    fun unlockGame(game: GameModel) {
        val key = getGameKey(game)
        if (key.isNotBlank()) {
            unlockedGameKeys.add(key)
        }
    }

    private fun getGameKey(game: GameModel): String {
        return game.getPlayUrl().ifBlank { game.title }
    }

    /**
     * Show Rewarded Ad.
     * Tries preloaded ad first; falls back to on-demand single load.
     * On reward earned, executes [onRewardEarned] callback on the UI thread.
     */
    fun showRewardedAd(
        activity: Activity,
        game: GameModel,
        onRewardEarned: () -> Unit,
        onAdDismissedWithoutReward: (() -> Unit)? = null
    ) {
        // If already unlocked, allow instant play
        if (!isGameLocked(game)) {
            activity.runOnUiThread { onRewardEarned() }
            return
        }

        // Show loading toast immediately until ad launches
        activity.runOnUiThread {
            if (BuildConfig.DEBUG) showToast(activity, "Video ad loading...", Toast.LENGTH_SHORT)
        }

        try {
            if (RewardedAdPreloader.isAdAvailable(AD_UNIT_ID)) {
                val ad = RewardedAdPreloader.pollAd(AD_UNIT_ID)
                if (ad != null) {
                    activity.runOnUiThread {
                        showLoadedAd(activity, ad, game, onRewardEarned, onAdDismissedWithoutReward)
                    }
                    return
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Preloaded pollAd failed, falling back to single load", e)
        }

        // Fallback: On-demand Single Load
        val adRequest = AdRequest.Builder(AD_UNIT_ID).build()

        RewardedAd.load(
            adRequest,
            object : AdLoadCallback<RewardedAd> {
                override fun onAdLoaded(ad: RewardedAd) {
                    activity.runOnUiThread {
                        showLoadedAd(activity, ad, game, onRewardEarned, onAdDismissedWithoutReward)
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.w(TAG, "RewardedAd on-demand load failed: ${adError.message}")
                    // Graceful fallback on main thread: unlock game and notify user so they aren't blocked
                    activity.runOnUiThread {
                        if (BuildConfig.DEBUG) showToast(activity, "Ad not ready, unlocking game for you! 🎮", Toast.LENGTH_SHORT)
                        unlockGame(game)
                        onRewardEarned()
                    }
                }
            }
        )
    }

    private fun showLoadedAd(
        activity: Activity,
        ad: RewardedAd,
        game: GameModel,
        onRewardEarned: () -> Unit,
        onAdDismissedWithoutReward: (() -> Unit)?
    ) {
        activity.runOnUiThread {
            AppOpenAdManager.isOtherAdShowing = true
            ad.adEventCallback = object : RewardedAdEventCallback {
                override fun onAdImpression() {
                    Log.d(TAG, "Rewarded ad recorded an impression.")
                    appContext?.let { ctx ->
                        Thread {
                            startPreload(ctx, force = true)
                        }.start()
                    }
                }

                override fun onAdPaid(value: AdValue) {
                    Log.d(TAG, "Rewarded ad onPaid: ${value.valueMicros} ${value.currencyCode}")
                }

                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                    Log.w(TAG, "Rewarded ad failed to show: $fullScreenContentError")
                    AppOpenAdManager.isOtherAdShowing = false
                    AppOpenAdManager.lastOtherAdDismissedTime = System.currentTimeMillis()
                    activity.runOnUiThread {
                        if (BuildConfig.DEBUG) showToast(activity, "Ad failed to show, unlocking game for you! 🎮", Toast.LENGTH_SHORT)
                        unlockGame(game)
                        onRewardEarned()
                    }
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad dismissed.")
                    AppOpenAdManager.isOtherAdShowing = false
                    AppOpenAdManager.lastOtherAdDismissedTime = System.currentTimeMillis()
                    activity.runOnUiThread {
                        onAdDismissedWithoutReward?.invoke()
                    }
                }
            }

            ad.show(activity) { reward ->
                unlockGame(game)
                activity.runOnUiThread {
                    if (BuildConfig.DEBUG) showToast(activity, "🎉 Reward earned! Game unlocked.", Toast.LENGTH_SHORT)
                    onRewardEarned()
                }
            }
        }
    }
}
