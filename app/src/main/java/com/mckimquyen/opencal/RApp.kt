package com.mckimquyen.opencal

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import com.google.android.gms.ads.MobileAds
import com.mckimquyen.opencal.db.MyPreferences
import com.mckimquyen.opencal.sdkadbmob.AdMobManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//TODO firebase
//TODO splash screen

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

    override fun onCreate() {
        super.onCreate()

//        this.setupApplovinAd()
        setupAdmob()

        // if the theme is overriding the system, the first creation doesn't work properly
        val forceDayNight = MyPreferences(this).forceDayNight
        if (forceDayNight != MODE_NIGHT_UNSPECIFIED && forceDayNight != MODE_NIGHT_FOLLOW_SYSTEM) {
            setDefaultNightMode(forceDayNight)
        }

        if (BuildConfig.DEBUG) {
            Toast.makeText(this, "OpenCalcApplication onCreate", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAdmob() {
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@RApp) {}
            AdMobManager.init(this@RApp) { success, gaidCurrent ->
                Log.d("roy93~", "AdMobManager init success $success, gaidCurrent $gaidCurrent")
            }
        }
//        registerActivityLifecycleCallbacks(
//            AppLifecycleListener(
//                { isForeground, activity ->
////                    if (isForeground) {
////                        Log.d("roy93~", "App moved to Foreground")
////                        if (activity.localClassName == SplashActivity::class.java.simpleName) {
////                            //do nothing
////                        } else {
////                            AdMobManager.showAppOpenAd(activity)
////                        }
////                    } else {
////                        Log.d("roy93~", "App moved to Background")
////                    }
//                }, { activity ->
////                    Log.d("roy93~", "callbackActivityCreated ${activity.localClassName}")
////                    if (activity.localClassName == SplashActivity::class.java.simpleName) {
////                        //do nothing
////                    } else {
////                        AdMobManager.loadAppOpenAd(
////                            context = this,
////                            adUnitId = BuildConfig.ADMOB_APP_OPEN_ID,
////                            onAdLoaded = {},
////                        )
////                    }
//                }
//            )
//        )
    }
}
