package com.mckimquyen.opencal.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.mckimquyen.opencal.BaseActivity
import com.mckimquyen.opencal.BuildConfig
import com.mckimquyen.opencal.R
import com.mckimquyen.opencal.db.MyPreferences
import com.mckimquyen.opencal.model.Themes
import com.mckimquyen.opencal.rateAppInApp
import com.roy.sdkadbmob.AdManager
import java.util.Locale

class SettingsActivity : BaseActivity() {

    private var adView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupViews(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        rateAppInApp(BuildConfig.DEBUG)
        AdManager.bannerResume(adView)
    }

    private fun setupViews(savedInstanceState: Bundle?) {
        // Themes
        val themes = Themes(this)
        themes.applyDayNightOverride()
        setTheme(themes.getTheme())

        // E-INFRA-6: setupEdgeToEdge1/2 giờ tự động chạy trong BaseActivity.setContentView().
        setContentView(R.layout.a_settings)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Change the status bar color
        if (MyPreferences(this).theme == 1) { // Amoled theme
            window.statusBarColor = ContextCompat.getColor(this, R.color.amoled_background_color)
        } else {
            window.statusBarColor = ContextCompat.getColor(this, R.color.background_color)
        }

        // back button
        findViewById<ImageView>(R.id.ivSettingsBack).setOnClickListener {
            finish()
        }

//        adView = this@SettingsActivity.createAdBanner(
//            logTag = SettingsActivity::class.simpleName,
//            viewGroup = findViewById<FrameLayout>(R.id.flAd),
//            isAdaptiveBanner = true,
//        )
        val layoutAdBanner = findViewById<ViewGroup>(R.id.layoutAdBanner)
        val bannerContainer = layoutAdBanner.findViewById<FrameLayout>(R.id.bannerContainer)
        val tvLabelAd = layoutAdBanner.findViewById<TextView>(R.id.tvLabelAd)
        adView = AdManager.loadBanner(
            context = this,
            container = bannerContainer,
            tvLabelAd = tvLabelAd,
        )
    }

    override fun onPause() {
        AdManager.bannerPause(adView)
        super.onPause()
    }

    override fun onDestroy() {
        AdManager.bannerDestroy(adView)
//        findViewById<FrameLayout>(R.id.flAd).destroyAdBanner(adView)
        super.onDestroy()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val appLanguagePreference =
                findPreference<Preference>("mckimquyen.opencal.APP_LANGUAGE")

            // Display the current selected language
            appLanguagePreference?.summary = com.mckimquyen.opencal.util.LanguageHelper.getCurrentLanguageDisplayName(requireContext())

            // Select app language button - Now works on ALL Android versions
            appLanguagePreference?.setOnPreferenceClickListener {
                activity?.let { com.mckimquyen.opencal.util.LanguageHelper.showLanguagePicker(it) }
                true
            }
        }
    }

}
