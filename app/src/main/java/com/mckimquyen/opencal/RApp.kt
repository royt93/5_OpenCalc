package com.mckimquyen.opencal

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import com.applovin.sdk.AppLovinSdk
import com.google.android.gms.ads.MobileAds
import com.mckimquyen.opencal.db.MyPreferences
import com.mckimquyen.opencal.util.Logger
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.AdSdkConfig

//review in app
//applovin done ad id
//120hz
//font scale
//done
//leak canary
//permission ad id
//policy
//rate app, more app, share app
//github, fork github
//double tap to exit
//join beta
//ic_launcher
//keystore

//https://github.com/tplloi/OpenCalc/tree/dev
class RApp : Application() {

    override fun attachBaseContext(base: Context) {
        // Apply language BEFORE calling super.attachBaseContext()
        val newContext = com.mckimquyen.opencal.util.LanguageHelper.applyLanguage(base)
        super.attachBaseContext(newContext)
    }

    override fun onCreate() {
        super.onCreate()

        setupAd()

        // if the theme is overriding the system, the first creation doesn't work properly
        val forceDayNight = MyPreferences(this).forceDayNight
        if (forceDayNight != MODE_NIGHT_UNSPECIFIED && forceDayNight != MODE_NIGHT_FOLLOW_SYSTEM) {
            setDefaultNightMode(forceDayNight)
        }

        if (BuildConfig.DEBUG) {
            Toast.makeText(this, "OpenCalcApplication onCreate", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAd() {
        val adConfig = AdSdkConfig(
            isEnableAdmob = BuildConfig.IS_ENABLE_ADMOB,
            isDebug = BuildConfig.DEBUG,
            admobBannerId = BuildConfig.ADMOB_BANNER_ID,
            admobInterstitialId = BuildConfig.ADMOB_INTERSTITIAL_ID,
            admobAppOpenId = BuildConfig.ADMOB_APP_OPEN_ID,
            applovinBannerId = BuildConfig.APPLOVIN_BANNER_ID,
            applovinInterstitialId = BuildConfig.APPLOVIN_INTERSTITIAL_ID,
            applovinAppOpenId = BuildConfig.APPLOVIN_APP_OPEN_ID,
        )
        AdManager.setConfig(adConfig)
        AdManager.earlyInit(this)

        if (BuildConfig.IS_ENABLE_ADMOB) {
            // AdMob: init trực tiếp trên Main Thread (yêu cầu của Google)
            MobileAds.initialize(this) { status ->
                AdManager.init(this, adConfig) { success, gaid ->
                    Logger.d("AdManager init success=$success, gaid=$gaid")
                    // registerAppOpenAdLifecycle phải gọi trên Main Thread
                    Handler(Looper.getMainLooper()).post {
                        AdManager.registerAppOpenAdLifecycle(this@RApp)
                    }
                }
            }
        } else {
            // AppLovin: init SDK trước, sau đó mới gọi AdManager.init
            val sdk = AppLovinSdk.getInstance(this)
            sdk.mediationProvider = "max"
            sdk.initializeSdk {
                AdManager.init(this, adConfig) { success, gaid ->
                    Logger.d("AdManager init success=$success, gaid=$gaid")
                    // registerAppOpenAdLifecycle phải gọi trên Main Thread
                    Handler(Looper.getMainLooper()).post {
                        AdManager.registerAppOpenAdLifecycle(this@RApp)
                    }
                }
            }
        }
    }
}
