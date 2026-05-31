package com.mckimquyen.opencal.model.adt

import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mckimquyen.opencal.model.History
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * "Widget" test (Android native — Robolectric/instrumented là tương đương 'widget test'
 * của Flutter) cho [HistoryAdapter]: append/remove/clear và đếm item.
 *
 * Gắn adapter vào một RecyclerView thật + observer để các lệnh notify* (gồm
 * appendHistory đã fix off-by-one) được thực thi mà không ném exception.
 */
@RunWith(AndroidJUnit4::class)
class HistoryAdapterInstrumentedTest {

    private lateinit var adapter: HistoryAdapter
    private lateinit var recycler: RecyclerView
    private var lastClicked: String? = null

    private fun h(calc: String, res: String) = History(calc, res, "1700000000000")

    private fun onMain(block: () -> Unit) =
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    @Before
    fun setup() = onMain {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        adapter = HistoryAdapter(mutableListOf()) { value -> lastClicked = value }
        recycler = RecyclerView(ctx).apply {
            layoutManager =
                androidx.recyclerview.widget.LinearLayoutManager(ctx)
            adapter = this@HistoryAdapterInstrumentedTest.adapter
        }
    }

    @Test
    fun appendHistoryAddsAllItems() {
        onMain { adapter.appendHistory(listOf(h("1+1", "2"), h("2+2", "4"), h("3+3", "6"))) }
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun appendEmptyListKeepsZero() {
        onMain { adapter.appendHistory(emptyList()) }
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun appendOneElementIncrements() {
        onMain {
            adapter.appendHistory(listOf(h("1+1", "2")))
            adapter.appendOneHistoryElement(h("2+2", "4"))
        }
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun removeFirstDecrements() {
        onMain {
            adapter.appendHistory(listOf(h("a", "1"), h("b", "2")))
            adapter.removeFirstHistoryElement()
        }
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun clearEmptiesAdapter() {
        onMain {
            adapter.appendHistory(listOf(h("a", "1"), h("b", "2"), h("c", "3")))
            adapter.clearHistory()
        }
        assertEquals(0, adapter.itemCount)
    }
}
