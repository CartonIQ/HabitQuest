package com.habitquest.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/** All ad unit IDs below are Google's official TEST IDs. */
object AdManager {
    const val APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val BANNER_ID = "ca-app-pub-9771662959477951/6779464013"
    const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1032978111"
    const val REWARDED_ID = "ca-app-pub-9771662959477951/5310257642"

    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val appContext = context.applicationContext
        MobileAds.initialize(appContext) { }
        loadInterstitial(appContext)
        loadRewarded(appContext)
    }

    fun createBanner(context: Context): AdView {
        val adView = AdView(context)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = BANNER_ID
        adView.loadAd(AdRequest.Builder().build())
        return adView
    }

    private fun loadInterstitial(context: Context) {
        InterstitialAd.load(
            context,
            INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                }
            }
        )
    }

    private fun loadRewarded(context: Context) {
        RewardedAd.load(
            context,
            REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewarded = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewarded = null
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onDone: () -> Unit = {}) {
        val ad = interstitial
        val appContext = activity.applicationContext
        if (ad == null) {
            loadInterstitial(appContext)
            onDone()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                loadInterstitial(appContext)
                onDone()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null
                loadInterstitial(appContext)
                onDone()
            }
        }
        ad.show(activity)
    }

    /** Calls onReward when the user earns the reward, or onUnavailable if no ad is ready. */
    fun showRewarded(activity: Activity, onReward: () -> Unit, onUnavailable: () -> Unit) {
        val ad = rewarded
        val appContext = activity.applicationContext
        if (ad == null) {
            loadRewarded(appContext)
            onUnavailable()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewarded = null
                loadRewarded(appContext)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewarded = null
                loadRewarded(appContext)
                onUnavailable()
            }
        }
        ad.show(activity, OnUserEarnedRewardListener { _ -> onReward() })
    }
}
