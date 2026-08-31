package com.mckimquyen.opencal

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mckimquyen.opencal.feature.vip.VipActivity
import com.mckimquyen.opencal.ui.SplashActivity
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.AdSafetyLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Integration contract between [RApp], BuildConfig, manifest and wrapper 1.7.x. */
@RunWith(AndroidJUnit4::class)
class AdHostIntegrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun applicationInitializesCompleteSafeDebugConfig() {
        val config = AdManager.config

        assertTrue(config.isEnableAdmob)
        assertTrue(config.isDebug)
        assertEquals(BuildConfig.ADMOB_BANNER_ID, config.admobBannerId)
        assertEquals(BuildConfig.ADMOB_INTERSTITIAL_ID, config.admobInterstitialId)
        assertEquals(BuildConfig.ADMOB_APP_OPEN_ID, config.admobAppOpenId)
        assertEquals(BuildConfig.ADMOB_REWARDED_ID, config.admobRewardedId)
        assertEquals(BuildConfig.APPLOVIN_SDK_KEY, config.applovinSdkKey)
        assertEquals(BuildConfig.APPLOVIN_BANNER_ID, config.applovinBannerId)
        assertEquals(BuildConfig.APPLOVIN_INTERSTITIAL_ID, config.applovinInterstitialId)
        assertEquals(BuildConfig.APPLOVIN_APP_OPEN_ID, config.applovinAppOpenId)
        assertEquals(BuildConfig.APPLOVIN_REWARDED_ID, config.applovinRewardedId)
        assertTrue(config.blockLoadUntilConsent)
        assertEquals("G", config.maxAdContentRating)
        assertEquals(AdSafetyLimits.TEST, config.safety)
        assertFalse("Host must own rewarded VIP grant", config.grantRewardOnEarn)
        assertEquals(30, config.vipRedeemCodes.values.maxOrNull())
        assertEquals(3, config.vipRedeemCodes.values.minOrNull())
        assertTrue(config.appOpenExcludedActivities.contains(SplashActivity::class.java))
        assertTrue(config.appOpenExcludedActivities.contains(VipActivity::class.java))
        assertTrue("SDK config validation: ${AdManager.validateConfig()}", AdManager.validateConfig().isEmpty())
    }

    @Test
    fun manifestAndRuntimeUseGoogleSampleInventory() {
        @Suppress("DEPRECATION")
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA,
        )
        assertEquals(
            "ca-app-pub-3940256099942544~3347511713",
            appInfo.metaData.getString("com.google.android.gms.ads.APPLICATION_ID"),
        )
        listOf(
            BuildConfig.ADMOB_BANNER_ID,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            BuildConfig.ADMOB_APP_OPEN_ID,
            BuildConfig.ADMOB_REWARDED_ID,
        ).forEach { assertTrue(it.startsWith("ca-app-pub-3940256099942544/")) }
    }

    @Test
    fun qaTestDeviceHashes_areInstalledBeforeAdRequests() {
        val ids = AdManager.getTestDeviceIds()
        assertEquals(12, ids.size)
        assertEquals(ids.size, ids.distinct().size)
        assertTrue(ids.all { it.matches(Regex("[0-9A-F]{32}")) })
    }
}
