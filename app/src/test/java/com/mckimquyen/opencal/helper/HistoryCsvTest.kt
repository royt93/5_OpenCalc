package com.mckimquyen.opencal.helper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test cho [HistoryCsv] — parser CSV thuần dùng cho N-DATA-2 (Import History).
 */
class HistoryCsvTest {

    // ---------- parseLine ----------
    @Test fun parseLineSimpleCommaSeparated() =
        assertEquals(listOf("a", "b", "c"), HistoryCsv.parseLine("a,b,c"))

    @Test fun parseLineQuotedFieldWithComma() =
        assertEquals(listOf("1,234", "2"), HistoryCsv.parseLine("\"1,234\",\"2\""))

    @Test fun parseLineEscapedQuoteInsideQuotedField() =
        assertEquals(listOf("say \"hi\""), HistoryCsv.parseLine("\"say \"\"hi\"\"\""))

    @Test fun parseLineEmptyTrailingField() =
        assertEquals(listOf("a", "b", ""), HistoryCsv.parseLine("a,b,"))

    // ---------- parse: header handling ----------
    @Test fun parseSkipsKnownHeader() {
        val (rows, skipped) = HistoryCsv.parse(
            listOf("Calculation,Result,Time,Pinned", "1+1,2,2026-01-01 10:00:00,false")
        )
        assertEquals(1, rows.size)
        assertEquals(0, skipped)
        assertEquals("1+1", rows[0].calculation)
    }

    @Test fun parseWithoutHeaderStillWorks() {
        val (rows, skipped) = HistoryCsv.parse(listOf("1+1,2,2026-01-01 10:00:00,false"))
        assertEquals(1, rows.size)
        assertEquals(0, skipped)
    }

    // ---------- parse: dữ liệu hỏng bị bỏ qua thay vì crash ----------
    @Test fun parseSkipsRowMissingResult() {
        val (rows, skipped) = HistoryCsv.parse(listOf("1+1,2,ok", "5+5,,2026-01-01 10:00:00"))
        assertEquals(1, rows.size)
        assertEquals(1, skipped)
    }

    @Test fun parseSkipsBlankLines() {
        val (rows, skipped) = HistoryCsv.parse(listOf("1+1,2", "", "   ", "2+2,4"))
        assertEquals(2, rows.size)
        assertEquals(0, skipped)
    }

    @Test fun parseEmptyInputReturnsEmpty() {
        val (rows, skipped) = HistoryCsv.parse(emptyList())
        assertTrue(rows.isEmpty())
        assertEquals(0, skipped)
    }

    // ---------- parse: time / pinned ----------
    @Test fun parseUnparsableTimeFallsBackInsteadOfCrashing() {
        val (rows, skipped) = HistoryCsv.parse(listOf("1+1,2,not-a-date,true"))
        assertEquals(1, rows.size)
        assertEquals(0, skipped)
        // Không crash + có giá trị time hợp lệ (epoch millis dạng số) thay vì null/giữ nguyên "not-a-date".
        assertTrue(rows[0].time?.toLongOrNull() != null)
    }

    @Test fun parsePinnedTrueFalseAndMissingDefaultsFalse() {
        val (rows, _) = HistoryCsv.parse(
            listOf(
                "1+1,2,2026-01-01 10:00:00,true",
                "2+2,4,2026-01-01 10:00:00,false",
                "3+3,6",
            )
        )
        assertTrue(rows[0].isPinned)
        assertFalse(rows[1].isPinned)
        assertFalse(rows[2].isPinned)
    }

    // ---------- Round-trip với chính format buildHistoryCsv() sinh ra ----------
    @Test fun parseRoundTripsExportedCsv() {
        val csv = "Calculation,Result,Time,Pinned\n" +
                "\"100×4\",\"400\",\"2026-01-01 10:00:00\",false\n" +
                "\"say \"\"hi\"\"\",\"ok\",\"2026-01-02 11:30:00\",true\n"
        val (rows, skipped) = HistoryCsv.parse(csv.lines())
        assertEquals(0, skipped)
        assertEquals(2, rows.size)
        assertEquals("100×4", rows[0].calculation)
        assertEquals("400", rows[0].result)
        assertFalse(rows[0].isPinned)
        assertEquals("say \"hi\"", rows[1].calculation)
        assertTrue(rows[1].isPinned)
    }
}
