package com.mckimquyen.opencal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM release-gate tests for the debug AdMob inventory. */
class AdBuildConfigurationTest {

    private val googleSamplePrefix = "ca-app-pub-3940256099942544/"

    @Test
    fun debugBuild_usesOnlyGoogleSampleAdUnits() {
        assertTrue(BuildConfig.DEBUG)
        val ids = listOf(
            BuildConfig.ADMOB_BANNER_ID,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            BuildConfig.ADMOB_APP_OPEN_ID,
            BuildConfig.ADMOB_REWARDED_ID,
        )

        assertEquals(4, ids.distinct().size)
        assertTrue(ids.all { it.startsWith(googleSamplePrefix) })
        assertFalse(ids.any { it.contains("3004713799155145") })
    }

    @Test
    fun adAndPrivacyFlags_areSafeForDebug() {
        assertTrue(BuildConfig.IS_ENABLE_ADMOB)
        assertTrue(BuildConfig.PRIVACY_POLICY_URL.startsWith("https://"))
    }
}
