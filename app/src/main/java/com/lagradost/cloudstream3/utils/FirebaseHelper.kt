package com.lagradost.cloudstream3.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.lagradost.cloudstream3.BuildConfig

object FirebaseHelper {
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        if (BuildConfig.USE_FIREBASE) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        }
    }

    fun logEvent(eventName: String, params: Bundle? = null) {
        if (BuildConfig.USE_FIREBASE) {
            firebaseAnalytics?.logEvent(eventName, params)
        }
    }

    fun logAdImpression(adFormat: String, adUnitId: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.AD_FORMAT, adFormat)
            putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
        }
        logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, bundle)
    }

    fun logScreenView(screenName: String, screenClass: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    // Add more logging functions as needed for specific metrics
}
