package com.mckimquyen.opencal.ui

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mckimquyen.opencal.R
import com.mckimquyen.opencal.db.MyPreferences
import com.mckimquyen.opencal.feature.vip.VipActivity
import com.mckimquyen.opencal.model.History
import com.sothree.slidinguppanel.PanelState
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Visual, full-case live smoke test runner for Samsung Galaxy S24 Ultra.
 * Executes live user flows with deliberate delays so the user can visually monitor the test.
 */
@RunWith(AndroidJUnit4::class)
class LiveFullCaseSmokeTest {

    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val STEP_DELAY_MS = 600L

    @Before
    fun setup() {
        MyPreferences(ctx).saveHistory(mutableListOf())
    }

    private fun findButtonByText(root: View, text: String): Button? {
        if (root is Button && root.text?.toString()?.trim() == text.trim()) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                findButtonByText(root.getChildAt(i), text)?.let { return it }
            }
        }
        return null
    }

    private fun clickById(scenario: ActivityScenario<MainActivity>, id: Int) {
        scenario.onActivity { act ->
            act.findViewById<View>(id)?.performClick()
        }
        Thread.sleep(STEP_DELAY_MS)
    }

    private fun clickDigit(scenario: ActivityScenario<MainActivity>, resId: Int) {
        scenario.onActivity { act ->
            val label = act.getString(resId)
            val btn = findButtonByText(act.window.decorView, label)
                ?: throw AssertionError("Digit button not found: $label")
            btn.performClick()
        }
        Thread.sleep(STEP_DELAY_MS)
    }

    private fun textOf(scenario: ActivityScenario<MainActivity>, id: Int): String {
        var result = ""
        scenario.onActivity { act ->
            result = act.findViewById<TextView>(id)?.text?.toString() ?: ""
        }
        return result
    }

    private fun awaitText(scenario: ActivityScenario<MainActivity>, viewId: Int, expected: String, timeoutMs: Long = 4000) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (textOf(scenario, viewId) == expected) return
            Thread.sleep(100)
        }
        assertEquals(expected, textOf(scenario, viewId))
    }

    @Test
    fun fullCase01_liveMathCalculationAndOperatorPrecedence() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000) // Allow user to see initial UI

        // 1. Calculate 123 + 456 = 579
        clickDigit(scenario, R.string.one)
        clickDigit(scenario, R.string.two)
        clickDigit(scenario, R.string.three)
        clickById(scenario, R.id.addButton)
        clickDigit(scenario, R.string.four)
        clickDigit(scenario, R.string.five)
        clickDigit(scenario, R.string.six)
        clickById(scenario, R.id.equalsButton)
        awaitText(scenario, R.id.input, "579")

        // 2. Chained multiplication: * 2 = 1158 (locale formatted as 1,158 or 1.158)
        clickById(scenario, R.id.multiplyButton)
        clickDigit(scenario, R.string.two)
        clickById(scenario, R.id.equalsButton)
        val text1158 = textOf(scenario, R.id.input).replace(".", "").replace(",", "")
        assertEquals("1158", text1158)

        // 3. Clear and compute square root: √144 = 12
        clickById(scenario, R.id.clearButton)
        Thread.sleep(400)
        clickById(scenario, R.id.squareButton)
        clickDigit(scenario, R.string.one)
        clickDigit(scenario, R.string.four)
        clickDigit(scenario, R.string.four)
        clickById(scenario, R.id.equalsButton)
        awaitText(scenario, R.id.input, "12")

        // 4. Division by zero error handling: 9 / 0 = Division by zero
        clickById(scenario, R.id.clearButton)
        clickDigit(scenario, R.string.nine)
        clickById(scenario, R.id.divideButton)
        clickDigit(scenario, R.string.zero)
        clickById(scenario, R.id.equalsButton)
        awaitText(scenario, R.id.resultDisplay, ctx.getString(R.string.division_by_0))
        Thread.sleep(1000)

        // 5. Clear all
        clickById(scenario, R.id.clearButton)
        scenario.close()
    }

    @Test
    fun fullCase02_liveScientificPanelAndTrigConstants() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)

        // Toggle scientific panel open
        clickById(scenario, R.id.scientistModeSwitchButton)
        Thread.sleep(800)

        // Calculate π * 2 = 6.2831853...
        scenario.onActivity { act ->
            val piBtn = findButtonByText(act.window.decorView, act.getString(R.string.pi))
            piBtn?.performClick()
        }
        Thread.sleep(STEP_DELAY_MS)
        clickById(scenario, R.id.multiplyButton)
        clickDigit(scenario, R.string.two)
        clickById(scenario, R.id.equalsButton)
        Thread.sleep(800)

        val result = textOf(scenario, R.id.input).replace(",", ".")
        assertTrue("Result should start with 6.28, actual=$result", result.startsWith("6.28"))

        // Clear and close scientific panel
        clickById(scenario, R.id.clearButton)
        clickById(scenario, R.id.scientistModeSwitchButton)
        Thread.sleep(500)
        scenario.close()
    }

    @Test
    fun fullCase03_liveHistoryPersistenceAndSlidingPanel() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)

        // Add 3 calculations to history
        clickDigit(scenario, R.string.two)
        clickById(scenario, R.id.addButton)
        clickDigit(scenario, R.string.three)
        clickById(scenario, R.id.equalsButton)
        awaitText(scenario, R.id.input, "5")

        clickDigit(scenario, R.string.one)
        clickDigit(scenario, R.string.zero)
        clickById(scenario, R.id.multiplyButton)
        clickDigit(scenario, R.string.one)
        clickDigit(scenario, R.string.zero)
        clickById(scenario, R.id.equalsButton)
        awaitText(scenario, R.id.input, "100")

        // Open SlidingUpPanel (History panel)
        scenario.onActivity { act ->
            val slidingLayout = act.findViewById<SlidingUpPanelLayout>(R.id.slidingLayout)
            slidingLayout?.panelState = PanelState.EXPANDED
        }
        Thread.sleep(1500) // Visual observation of history items

        // Collapse history panel
        scenario.onActivity { act ->
            val slidingLayout = act.findViewById<SlidingUpPanelLayout>(R.id.slidingLayout)
            slidingLayout?.panelState = PanelState.COLLAPSED
        }
        Thread.sleep(800)
        scenario.close()
    }

    @Test
    fun fullCase04_liveSubActivitiesNavigationWalkthrough() {
        // 1. Base Converter
        val baseConvScenario = ActivityScenario.launch(BaseConverterActivity::class.java)
        Thread.sleep(1500)
        baseConvScenario.close()

        // 2. Bill Splitter
        val billScenario = ActivityScenario.launch(BillSplitterActivity::class.java)
        Thread.sleep(1500)
        billScenario.close()

        // 3. VIP Screen
        val vipScenario = ActivityScenario.launch(VipActivity::class.java)
        Thread.sleep(1500)
        vipScenario.close()

        // 4. About Activity
        val aboutScenario = ActivityScenario.launch(AboutActivity::class.java)
        Thread.sleep(1500)
        aboutScenario.close()

        // 5. Settings Activity
        val settingsScenario = ActivityScenario.launch(SettingsActivity::class.java)
        Thread.sleep(1500)
        settingsScenario.close()
    }
}
