package com.autoquizdz.app.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdUnitIds {
    // Official Google AdMob TEST IDs.
    // Replace these 4 IDs with your real AdMob Ad Unit IDs ONLY right before Google Play release.
    const val BANNER = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    const val APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
    const val REWARDED = "ca-app-pub-3940256099942544/5224354917"
}

/**
 * Policy guard: ensures ads NEVER appear during exams, active question answering,
 * or before an exam result is displayed.
 */
object AdPolicyState {
    @Volatile
    var isProtectedFlowActive: Boolean = false
}

@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    if (AdPolicyState.isProtectedFlowActive) return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdUnitIds.BANNER
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

object InterstitialAdHelper {
    private var interstitialAd: InterstitialAd? = null
    private var lastShownMs: Long = 0L

    fun load(context: Context) {
        if (interstitialAd != null) return
        InterstitialAd.load(
            context.applicationContext,
            AdUnitIds.INTERSTITIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    /**
     * Shows interstitial only on safe transitions (e.g., exiting Signs Library)
     * with a 2-minute cooldown and never during protected learning/exam flows.
     */
    fun showIfAllowed(activity: Activity, onFinished: () -> Unit = {}) {
        val now = System.currentTimeMillis()
        val ad = interstitialAd
        if (AdPolicyState.isProtectedFlowActive || ad == null || (now - lastShownMs) < 120_000L) {
            onFinished()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                lastShownMs = System.currentTimeMillis()
                load(activity)
                onFinished()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                onFinished()
            }
        }
        ad.show(activity)
    }
}

object RewardedAdHelper {
    private var rewardedAd: RewardedAd? = null

    fun load(context: Context) {
        if (rewardedAd != null) return
        RewardedAd.load(
            context.applicationContext,
            AdUnitIds.REWARDED,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                }
            }
        )
    }

    fun isReady(): Boolean = rewardedAd != null

    /**
     * Opt-in rewarded ad (e.g., earn +50 Bonus XP on Profile/Home).
     */
    fun show(activity: Activity, onRewardEarned: () -> Unit, onUnavailable: () -> Unit = {}) {
        if (AdPolicyState.isProtectedFlowActive) {
            onUnavailable()
            return
        }
        val ad = rewardedAd
        if (ad == null) {
            load(activity)
            onUnavailable()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                load(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                load(activity)
            }
        }
        ad.show(activity) {
            onRewardEarned()
        }
    }
}

class AppOpenAdManager(private val application: Application) :
    DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTimeMs: Long = 0L
    private var currentActivity: Activity? = null

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun isAdAvailable(): Boolean {
        val hoursSinceLoad = (System.currentTimeMillis() - loadTimeMs) / 3_600_000L
        return appOpenAd != null && hoursSinceLoad < 4L
    }

    fun loadAd(context: Context) {
        if (isLoadingAd || isAdAvailable()) return
        isLoadingAd = true
        AppOpenAd.load(
            context.applicationContext,
            AdUnitIds.APP_OPEN,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTimeMs = System.currentTimeMillis()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                }
            }
        )
    }

    override fun onStart(owner: LifecycleOwner) {
        val activity = currentActivity ?: return
        if (isShowingAd || AdPolicyState.isProtectedFlowActive) return
        if (!isAdAvailable()) {
            loadAd(activity)
            return
        }
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                loadAd(activity)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                loadAd(activity)
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
            }
        }
        isShowingAd = true
        appOpenAd?.show(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        if (!isShowingAd) currentActivity = activity
    }
    override fun onActivityResumed(activity: Activity) {
        if (!isShowingAd) currentActivity = activity
    }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) currentActivity = null
    }
}
