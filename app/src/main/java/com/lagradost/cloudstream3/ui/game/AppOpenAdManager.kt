package com.lagradost.cloudstream3.ui.game

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.lagradost.cloudstream3.BuildConfig
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import android.app.Application
import android.os.Bundle
import java.util.Date

/**
 * Interface definition for a callback to be invoked when an app open ad is complete.
 */
fun interface OnShowAdCompleteListener {
    fun onShowAdComplete()
}

/**
 * Singleton object that manages Google Mobile Ads (GMA) NextGen App Open Ads.
 */
object AppOpenAdManager : Application.ActivityLifecycleCallbacks {
    private const val TAG = "AppOpenAdManager"

    // Sample AdMob App ID & App Open Ad unit ID from Google Mobile Ads NextGen SDK
    const val APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"

    private var currentActivity: Activity? = null
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    var isShowingAd = false
        private set
    var isOtherAdShowing = false
    var lastOtherAdDismissedTime: Long = 0
    private var startedActivitiesCount = 0
    private var showOnFirstLoad = true

    /** Keep track of the time an app open ad was loaded to ensure it doesn't expire (4 hours). */
    private var loadTime: Long = 0

    private fun isAdActivity(activity: Activity): Boolean {
        val name = activity.javaClass.name
        return name.contains("AdActivity", ignoreCase = true) || name.contains("com.google.android.gms.ads", ignoreCase = true)
    }

    /**
     * Initialize AppOpenAdManager and start listening to activity lifecycles.
     */
    fun init(app: Application) {
        app.registerActivityLifecycleCallbacks(this)
        try {
            val initConfig = InitializationConfig.Builder(APP_ID).build()
            MobileAds.initialize(app, initConfig) {
                Log.d(TAG, "MobileAds initialized for AppOpenAd.")
                loadAd(app.applicationContext)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MobileAds in AppOpenAdManager", e)
            loadAd(app.applicationContext)
        }
    }

    /**
     * Load an App Open Ad.
     */
    fun loadAd(context: Context) {
        if (isLoadingAd || isAdAvailable()) {
            Log.d(TAG, "App open ad is either loading or already loaded.")
            return
        }

        isLoadingAd = true
        val adRequest = AdRequest.Builder(AD_UNIT_ID).build()

        AppOpenAd.load(
            adRequest,
            object : AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    Log.d(TAG, "App open ad loaded successfully.")
                    if (BuildConfig.DEBUG) showToast("App open ad loaded ✅", Toast.LENGTH_SHORT)

                    // If loaded on cold start, show immediately on current active non-ad activity
                    if (showOnFirstLoad) {
                        showOnFirstLoad = false
                        currentActivity?.let { act ->
                            if (!isAdActivity(act) && !isOtherAdShowing) {
                                showAdIfAvailable(act)
                            }
                        }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    Log.w(TAG, "App open ad failed to load: $loadAdError")
                    if (BuildConfig.DEBUG) showToast("App open ad failed to load", Toast.LENGTH_SHORT)
                }
            }
        )
    }

    /** Check if ad was loaded less than 4 hours ago. */
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour = 3600000L
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    /** Check if ad exists and is valid. */
    fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    /**
     * Show the app open ad if available.
     *
     * @param activity The activity showing the ad.
     * @param onShowAdCompleteListener Callback when ad is finished or fails to show.
     */
    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener? = null) {
        if (isAdActivity(activity)) {
            Log.d(TAG, "Cannot show App Open ad on an AdActivity.")
            onShowAdCompleteListener?.onShowAdComplete()
            return
        }

        if (isShowingAd) {
            Log.d(TAG, "App open ad is already showing.")
            onShowAdCompleteListener?.onShowAdComplete()
            return
        }

        val now = System.currentTimeMillis()
        if (isOtherAdShowing || (now - lastOtherAdDismissedTime < 8000L)) {
            Log.d(TAG, "Skipping App Open ad because another ad is active or was recently dismissed.")
            onShowAdCompleteListener?.onShowAdComplete()
            return
        }

        if (!isAdAvailable()) {
            Log.d(TAG, "App open ad is not ready yet.")
            onShowAdCompleteListener?.onShowAdComplete()
            loadAd(activity.applicationContext)
            return
        }

        activity.runOnUiThread {
            appOpenAd?.adEventCallback = object : AppOpenAdEventCallback {
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "App open ad showed.")
                    if (BuildConfig.DEBUG) showToast("App open ad shown", Toast.LENGTH_SHORT)
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "App open ad dismissed.")
                    appOpenAd = null
                    isShowingAd = false
                    if (BuildConfig.DEBUG) showToast("App open ad dismissed", Toast.LENGTH_SHORT)
                    onShowAdCompleteListener?.onShowAdComplete()
                    loadAd(activity.applicationContext)
                }

                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                    Log.w(TAG, "App open ad failed to show: $fullScreenContentError")
                    appOpenAd = null
                    isShowingAd = false
                    if (BuildConfig.DEBUG) showToast("App open ad failed to show", Toast.LENGTH_SHORT)
                    onShowAdCompleteListener?.onShowAdComplete()
                    loadAd(activity.applicationContext)
                }

                override fun onAdImpression() {
                    Log.d(TAG, "App open ad recorded an impression.")
                }

                override fun onAdClicked() {
                    Log.d(TAG, "App open ad recorded a click.")
                }
            }

            isShowingAd = true
            appOpenAd?.show(activity)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (isAdActivity(activity)) return
        currentActivity = activity

        // Only trigger when entire app moves from background (0 activities) to foreground (1 activity)
        if (startedActivitiesCount == 0) {
            val now = System.currentTimeMillis()
            val timeSinceOtherAd = now - lastOtherAdDismissedTime
            if (!isShowingAd && !isOtherAdShowing && timeSinceOtherAd > 8000L && isAdAvailable()) {
                showAdIfAvailable(activity)
            }
        }
        startedActivitiesCount++
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {
        if (!isAdActivity(activity)) {
            currentActivity = activity
        }
    }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {
        if (isAdActivity(activity)) return
        startedActivitiesCount = (startedActivitiesCount - 1).coerceAtLeast(0)
    }
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
}
