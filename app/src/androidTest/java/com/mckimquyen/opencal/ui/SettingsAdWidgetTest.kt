package com.mckimquyen.opencal.ui

import android.view.View
import androidx.preference.PreferenceFragmentCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mckimquyen.opencal.R
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Widget tests for the user-facing consent entry and banner host lifecycle. */
@RunWith(AndroidJUnit4::class)
class SettingsAdWidgetTest {

    @Test
    fun privacyOptions_isVisibleEnabledAndSelectable() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.supportFragmentManager.executePendingTransactions()
                val fragment = activity.supportFragmentManager
                    .findFragmentById(R.id.settings) as PreferenceFragmentCompat
                val preference = fragment.findPreference<androidx.preference.Preference>(
                    "mckimquyen.opencal.AD_PRIVACY_OPTIONS",
                )

                assertNotNull(preference)
                assertTrue(preference!!.isVisible)
                assertTrue(preference.isEnabled)
                assertTrue(preference.isSelectable)
            }
        }
    }

    @Test
    fun bannerHost_survivesPauseResumeAndDestroy() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity.findViewById<View>(R.id.layoutAdBanner))
            }
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        }
    }
}
