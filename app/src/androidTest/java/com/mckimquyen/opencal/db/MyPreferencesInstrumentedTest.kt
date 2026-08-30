package com.mckimquyen.opencal.db

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mckimquyen.opencal.model.History
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test cho [MyPreferences] — dùng SharedPreferences thật trên thiết bị.
 *
 * Trọng tâm: xác minh fix `getHistory()` đọc TƯƠI từ SharedPreferences (an toàn khi
 * instance được cache & tái sử dụng nhiều lần trong một session), cùng round-trip
 * lưu/đọc lịch sử và logic cắt theo historySize.
 */
@RunWith(AndroidJUnit4::class)
class MyPreferencesInstrumentedTest {

    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun h(calc: String, res: String) = History(calc, res, "1700000000000")

    @Before
    fun clean() {
        val p = MyPreferences(ctx)
        p.historySize = "100"
        MyPreferences(ctx).saveHistory(mutableListOf())
    }

    @After
    fun cleanup() {
        MyPreferences(ctx).historySize = "100"
        MyPreferences(ctx).saveHistory(mutableListOf())
    }

    @Test
    fun saveAndGetRoundTrip() {
        val p = MyPreferences(ctx)
        p.saveHistory(listOf(h("1+1", "2"), h("2+2", "4")))

        val loaded = MyPreferences(ctx).getHistory()
        assertEquals(2, loaded.size)
        assertEquals("1+1", loaded[0].calculation)
        assertEquals("4", loaded[1].result)
    }

    /**
     * REGRESSION GUARD: một instance được tái sử dụng để lưu nhiều lần liên tiếp;
     * getHistory() trên CÙNG instance phải trả về dữ liệu mới nhất.
     * Trước khi fix, getHistory() parse field snapshot cũ → mất entry.
     */
    @Test
    fun cachedInstanceReturnsFreshHistoryAfterEachSave() {
        val p = MyPreferences(ctx) // instance được cache, dùng lại như MainActivity.prefs

        p.saveHistory(listOf(h("1+1", "2")))
        assertEquals(1, p.getHistory().size)

        p.saveHistory(listOf(h("1+1", "2"), h("9×9", "81")))
        val second = p.getHistory()
        assertEquals(2, second.size)
        assertEquals("9×9", second[1].calculation)

        p.saveHistory(listOf(h("1+1", "2"), h("9×9", "81"), h("√9", "3")))
        assertEquals(3, p.getHistory().size)
    }

    @Test
    fun historyTrimmedToHistorySize() {
        MyPreferences(ctx).historySize = "2"
        val p = MyPreferences(ctx) // đọc historySize="2" lúc khởi tạo
        p.saveHistory(listOf(h("a", "1"), h("b", "2"), h("c", "3"), h("d", "4")))

        val loaded = MyPreferences(ctx).getHistory()
        assertEquals(2, loaded.size)
        // Giữ 2 phần tử MỚI NHẤT (xóa từ đầu)
        assertEquals("c", loaded[0].calculation)
        assertEquals("d", loaded[1].calculation)
    }

    @Test
    fun emptyWhenCleared() {
        val p = MyPreferences(ctx)
        p.saveHistory(listOf(h("1+1", "2")))
        p.saveHistory(mutableListOf())
        assertEquals(0, MyPreferences(ctx).getHistory().size)
    }
}
