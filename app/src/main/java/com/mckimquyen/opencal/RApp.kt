package com.mckimquyen.opencal

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import com.google.android.gms.ads.MobileAds
import com.mckimquyen.opencal.common.const.AdKeys
import com.mckimquyen.opencal.db.MyPreferences
import com.mckimquyen.opencal.feature.vip.VipKeys
import com.mckimquyen.opencal.feature.vip.VipActivity
import com.mckimquyen.opencal.ui.SplashActivity
import com.mckimquyen.opencal.util.Logger
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.AdSafetyLimits
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
        // Các hash AdMob do chính GMA SDK in từ logcat; KHÔNG phải GAID. Áp trước wrapper init để
        // cả App Open preload đầu tiên trên release QA cũng là test request.
        val requestConfiguration = MobileAds.getRequestConfiguration()
            .toBuilder()
            .setTestDeviceIds(ADMOB_QA_TEST_DEVICE_HASHES)
            .build()
        MobileAds.setRequestConfiguration(requestConfiguration)

        val adConfig = AdSdkConfig(
            isEnableAdmob = BuildConfig.IS_ENABLE_ADMOB,
            isDebug = BuildConfig.DEBUG,
            admobBannerId = BuildConfig.ADMOB_BANNER_ID,
            admobInterstitialId = BuildConfig.ADMOB_INTERSTITIAL_ID,
            admobAppOpenId = BuildConfig.ADMOB_APP_OPEN_ID,
            admobRewardedId = BuildConfig.ADMOB_REWARDED_ID,
            vipKeySecret = AdKeys.VIP_SECRET,
            vipRedeemCodes = mapOf(
                VipKeys.VIP_30D_KEY to 30,
                VipKeys.VIP_3D_KEY to 3,
            ),
            // Host owns the reward transaction so one completed ad grants exactly once and the
            // screen can update atomically. Keep SDK auto-grant disabled to prevent double grants
            // if a wrapper release starts enforcing this currently-passive compatibility flag.
            grantRewardOnEarn = false,
            blockLoadUntilConsent = true,
            maxAdContentRating = "G",
            applovinPrivacyPolicyUrl = BuildConfig.PRIVACY_POLICY_URL,
            appOpenExcludedActivities = listOf(
                SplashActivity::class.java,
                VipActivity::class.java,
            ),
            // Debug: nới throttle để QC test ad nhanh. Release: preset UTILITY (UX-first cho app công cụ).
            safety = if (BuildConfig.DEBUG) AdSafetyLimits.TEST else AdSafetyLimits.UTILITY,
        )
        AdManager.setConfig(adConfig)
        // SDK 1.7.x tự early-init, khởi tạo provider và đăng ký App Open lifecycle.
        // Với GMS, provider được defer đến khi Splash hoàn tất UMP consent.
        AdManager.initialize(this) { success, gaid ->
            Logger.d("AdManager init success=$success, gaid=$gaid")
        }
    }

    private companion object {
        val ADMOB_QA_TEST_DEVICE_HASHES = listOf(
            "813DCF48B3E486F15A60676D49A2AB09",
            "E165942547A491D06E43E24870B990B2",
            "C3632968623F0B44E87CE401A06AC8F9",
            "4A2AA8832A7FE9D7805081AD03C9CE68",
            "DEE1D0C6AEA4CA5C94FA4D709087A3AC",
            "5D2E85389997C743F9CC33DF5F70D736",
            "EB7B6504801B5E518C4CE6D519ED325C",
            "FED3CA82141FF6113F2D069F8395B966",
            "96E61CBFCE6BC0BDCA1612F1BACB56BE",
            "5B409111AF01C6BB9F9FF77AEEB44275",
            "D1B50484E250B064A9BF6F7CAE29A941",
            "322285166ACB542864828826D2D92491",
        )
    }
}
