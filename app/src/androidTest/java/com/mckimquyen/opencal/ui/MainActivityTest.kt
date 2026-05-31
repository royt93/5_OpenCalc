package com.mckimquyen.opencal.ui

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mckimquyen.opencal.R
import com.mckimquyen.opencal.db.MyPreferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test (on-device) cho [MainActivity]: gõ phím → ấn '=' → kiểm tra kết quả.
 *
 * KHÔNG dùng Espresso perform/onView vì Espresso 3.6.x khởi tạo bộ bơm sự kiện qua
 * `InputManager.getInstance()` — method đã bị gỡ trên Android 15/16 → crash. Thay vào đó
 * điều khiển UI qua [ActivityScenarioRule.getScenario].onActivity (main thread): gọi
 * View.performClick() (kích hoạt android:onClick) và đọc text trực tiếp.
 *
 * Kết quả tính trong coroutine (Dispatchers.Default) nên đọc kết quả qua [awaitText] (poll).
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun clearHistory() {
        MyPreferences(ctx).saveHistory(ctx, mutableListOf())
    }

    // ---------- helpers ----------
    private fun findButtonByText(root: View, text: String): Button? {
        if (root is Button && root.text?.toString() == text) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                findButtonByText(root.getChildAt(i), text)?.let { return it }
            }
        }
        return null
    }

    // updateDisplay() xử lý input bất đồng bộ (coroutine) → chờ ổn định giữa các lần bấm
    // để tránh race khi test bấm nhanh hơn coroutine kịp cập nhật text.
    private val SETTLE_MS = 250L

    private fun clickById(id: Int) {
        rule.scenario.onActivity { act -> act.findViewById<View>(id).performClick() }
        Thread.sleep(SETTLE_MS)
    }

    private fun clickDigit(resId: Int) {
        rule.scenario.onActivity { act ->
            val label = act.getString(resId)
            val btn = findButtonByText(act.window.decorView, label)
                ?: throw AssertionError("Không tìm thấy nút số có text='$label'")
            btn.performClick()
        }
        Thread.sleep(SETTLE_MS)
    }

    private fun textOf(id: Int): String {
        var result = ""
        rule.scenario.onActivity { act ->
            result = act.findViewById<TextView>(id).text.toString()
        }
        return result
    }

    private fun awaitText(viewId: Int, expected: String, timeoutMs: Long = 4000) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (textOf(viewId) == expected) return
            Thread.sleep(100)
        }
        assertEquals(expected, textOf(viewId))
    }

    // ---------- tests ----------
    @Test
    fun additionShowsResult() {
        clickDigit(R.string.seven)
        clickById(R.id.addButton)
        clickDigit(R.string.eight)
        clickById(R.id.equalsButton)
        awaitText(R.id.input, "15")
    }

    @Test
    fun multiplicationShowsResult() {
        clickDigit(R.string.nine)
        clickById(R.id.multiplyButton)
        clickDigit(R.string.nine)
        clickById(R.id.equalsButton)
        awaitText(R.id.input, "81")
    }

    @Test
    fun squareRootShowsResult() {
        clickById(R.id.squareButton) // √
        clickDigit(R.string.nine)
        clickById(R.id.equalsButton)
        awaitText(R.id.input, "3")
    }

    @Test
    fun divisionByZeroShowsError() {
        clickDigit(R.string.five)
        clickById(R.id.divideButton)
        clickDigit(R.string.zero)
        clickById(R.id.equalsButton)
        awaitText(R.id.resultDisplay, ctx.getString(R.string.division_by_0))
    }

    @Test
    fun backspaceRemovesLastChar() {
        clickDigit(R.string.one)
        clickDigit(R.string.two)
        clickDigit(R.string.three)
        clickById(R.id.backspaceButton)
        awaitText(R.id.input, "12")
    }

    @Test
    fun clearResetsInput() {
        clickDigit(R.string.five)
        clickDigit(R.string.five)
        clickById(R.id.clearButton)
        awaitText(R.id.input, "")
    }

    @Test
    fun digitAfterEqualsStartsFresh() {
        clickDigit(R.string.two)
        clickById(R.id.addButton)
        clickDigit(R.string.two)
        clickById(R.id.equalsButton)
        awaitText(R.id.input, "4")
        clickDigit(R.string.five)
        awaitText(R.id.input, "5")
    }

    @Test
    fun equalsPersistsToHistory() {
        clickDigit(R.string.six)
        clickById(R.id.addButton)
        clickDigit(R.string.one)
        clickById(R.id.equalsButton)
        awaitText(R.id.input, "7")
        // chờ coroutine lưu history rồi kiểm tra persist
        Thread.sleep(500)
        val history = MyPreferences(ctx).getHistory()
        assertEquals(1, history.size)
        assertEquals("6+1", history[0].calculation)
        assertEquals("7", history[0].result)
    }
}
