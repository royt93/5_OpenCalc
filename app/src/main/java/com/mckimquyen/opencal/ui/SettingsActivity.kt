package com.mckimquyen.opencal.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.mckimquyen.opencal.BaseActivity
import com.mckimquyen.opencal.BuildConfig
import com.mckimquyen.opencal.R
import com.mckimquyen.opencal.db.MyPreferences
import com.mckimquyen.opencal.model.Themes
import com.mckimquyen.opencal.rateAppInApp
import com.mckimquyen.opencal.sdkadbmob.AdMobManager
import com.mckimquyen.opencal.sdkadbmob.UIUtils
import java.util.Locale

class SettingsActivity : BaseActivity() {

    //    private var adView: MaxAdView? = null
    private var adView: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupViews(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        rateAppInApp(BuildConfig.DEBUG)
        adView?.resume()
    }

    private fun setupViews(savedInstanceState: Bundle?) {
        // Themes
        val themes = Themes(this)
        themes.applyDayNightOverride()
        setTheme(themes.getTheme())

        UIUtils.setupEdgeToEdge1(window)
        setContentView(R.layout.a_settings)
        UIUtils.setupEdgeToEdge2(
            rootView = findViewById(R.id.layoutRoot),
            paddingTop = true,
            paddingBottom = true
        )

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
        adView = AdMobManager.loadBanner(
            context = this,
            adUnitId = BuildConfig.ADMOB_BANNER_ID,
            container = bannerContainer,
            tvLabelAd = tvLabelAd,
            adSize = AdSize.LARGE_BANNER,
        )
    }

    override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        adView?.destroy()
//        findViewById<FrameLayout>(R.id.flAd).destroyAdBanner(adView)
        super.onDestroy()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val appLanguagePreference =
                findPreference<Preference>("mckimquyen.opencal.APP_LANGUAGE")

            // remove the app language button if you are using an Android version lower than v33 (Android 13)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                appLanguagePreference?.isVisible = false
            } else {
                // Display the current selected language
                appLanguagePreference?.summary = Locale.getDefault().displayLanguage
            }
            // Select app language button
            appLanguagePreference?.setOnPreferenceClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launchChangeAppLanguageIntent()
                }
                true
            }
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun launchChangeAppLanguageIntent() {
            try {
                Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                    startActivity(this)
                }
            } catch (e: Exception) {
                try {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", requireContext().packageName, null)
                        startActivity(this)
                    }
                } catch (e: Exception) {
                    println(e)
                }
            }
        }
    }

}
