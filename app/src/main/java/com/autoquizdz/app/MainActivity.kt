package com.autoquizdz.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autoquizdz.app.ads.AppOpenAdManager
import com.autoquizdz.app.ads.InterstitialAdHelper
import com.autoquizdz.app.ads.RewardedAdHelper
import com.autoquizdz.app.ui.AutoQuizRoot
import com.autoquizdz.app.ui.AutoQuizTheme
import com.autoquizdz.app.ui.MainViewModel
import com.google.android.gms.ads.MobileAds

class AutoQuizApplication : Application() {
    lateinit var appOpenAdManager: AppOpenAdManager
        private set

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
        appOpenAdManager = AppOpenAdManager(this)
        appOpenAdManager.loadAd(this)
        InterstitialAdHelper.load(this)
        RewardedAdHelper.load(this)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MainViewModel = viewModel()
            val direction = if (vm.isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                AutoQuizTheme {
                    AutoQuizRoot(vm = vm, activity = this)
                }
            }
        }
    }
}
