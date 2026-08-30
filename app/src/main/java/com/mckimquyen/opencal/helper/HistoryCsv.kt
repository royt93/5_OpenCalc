package com.mckimquyen.opencal.helper

import com.mckimquyen.opencal.model.History
import java.text.SimpleDateFormat
import java.util.Locale

// N-DATA-2: logic parse CSV thuần (không đụng Android framework) tách riêng để unit-test được
// bằng JVM, theo pattern VipMath — MainActivity chỉ gọi HistoryCsv.parse(...).
object HistoryCsv {

    // Parser 1 dòng CSV thủ công (RFC 4180 tối thiểu) thay vì kéo thêm thư viện — chỉ cần xử lý
    // dấu ngoặc kép + escape "" bên trong, đúng những gì MainActivity.buildHistoryCsv() tự sinh
    // ra. KHÔNG hỗ trợ field chứa xuống dòng (buildHistoryCsv() không bao giờ sinh ra trường hợp đó).
    fun parseLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        fields.add(field.toString())
                        field.clear()
                    }

                    else -> field.append(c)
                }
            }
            i++
        }
        fields.add(field.toString())
        return fields
    }

    /**
     * Trả về (danh sách History hợp lệ, số dòng bị bỏ qua vì hỏng/thiếu cột) — không throw để 1
     * file lỗi không làm mất toàn bộ import, chỉ mất đúng những dòng thật sự không đọc được.
     */
    fun parse(lines: List<String>): Pair<List<History>, Int> {
        val nonBlankLines = lines.filter { it.isNotBlank() }
        if (nonBlankLines.isEmpty()) return emptyList<History>() to 0

        // File tự export ra luôn có header "Calculation,Result,Time,Pinned" — bỏ qua nếu có,
        // không bắt buộc phải có (import file do user tự chỉnh sửa/tạo tay không header).
        val startIndex = if (parseLine(nonBlankLines[0]).firstOrNull() == "Calculation") 1 else 0
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val result = mutableListOf<History>()
        var skipped = 0

        for (i in startIndex until nonBlankLines.size) {
            val fields = parseLine(nonBlankLines[i])
            val calculation = fields.getOrNull(0)
            val calcResult = fields.getOrNull(1)
            if (calculation.isNullOrBlank() || calcResult.isNullOrBlank()) {
                skipped++
                continue
            }
            val timeMillis = fields.getOrNull(2)
                ?.let { runCatching { dateFormat.parse(it)?.time }.getOrNull() }
                ?: System.currentTimeMillis()
            val isPinned = fields.getOrNull(3)?.toBooleanStrictOrNull() ?: false
            result.add(History(calculation, calcResult, timeMillis.toString(), isPinned))
        }
        return result to skipped
    }
}
