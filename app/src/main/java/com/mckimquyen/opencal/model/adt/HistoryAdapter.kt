package com.mckimquyen.opencal.model.adt

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Build
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mckimquyen.opencal.R
import com.mckimquyen.opencal.ext.share
import com.mckimquyen.opencal.model.History

class HistoryAdapter(
    private var history: MutableList<History>,
    private val onElementClick: (value: String) -> Unit,
    // N-DATA-4: báo cho MainActivity snapshot mới nhất của toàn bộ list sau khi user ghim/bỏ ghim,
    // để persist vào SharedPreferences — adapter chỉ giữ state UI, không tự đụng prefs.
    private val onPinToggle: (updatedHistory: List<History>) -> Unit = {},
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    // N-DATA-3: null = không filter (hiện nguyên `history`). Khi có filter, đây là INDEX vào
    // `history` của các entry khớp query — giữ index (không copy History) để pin/xoá vẫn sửa
    // đúng phần tử gốc, và không phải viết lại toàn bộ logic ghi/xoá bên dưới theo "vị trí hiển thị".
    private var activeQuery: String = ""
    private var filteredIndices: List<Int>? = null

    private fun recomputeFilter() {
        val query = activeQuery.trim()
        filteredIndices = if (query.isEmpty()) {
            null
        } else {
            history.indices.filter { i ->
                history[i].calculation?.contains(query, ignoreCase = true) == true ||
                        history[i].result?.contains(query, ignoreCase = true) == true
            }
        }
    }

    /** Map vị trí đang HIỂN THỊ (adapter position) -> index thật trong `history`. */
    private fun indexAt(adapterPosition: Int): Int = filteredIndices?.get(adapterPosition) ?: adapterPosition

    val isFiltered: Boolean get() = filteredIndices != null

    /** Kích thước history THẬT (không phụ thuộc filter) — dùng cho logic trim/scroll ở MainActivity. */
    val fullHistorySize: Int get() = history.size

    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        activeQuery = query
        recomputeFilter()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.i_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun getItemCount(): Int = filteredIndices?.size ?: history.size

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(indexAt(position), position)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun appendHistory(historyList: List<History>) {
        val startPosition = this.history.size
        this.history.addAll(historyList)
        recomputeFilter()
        if (filteredIndices == null) {
            notifyItemRangeInserted(startPosition, historyList.size)
        } else {
            notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun appendOneHistoryElement(history: History) {
        this.history.add(history)
        recomputeFilter()
        notifyDataSetChanged()
    }

    /**
     * N-DATA-4: xoá entry CŨ NHẤT CHƯA GHIM để nhường chỗ cho entry mới theo `historySize`.
     * Trả về false nếu mọi entry còn lại đều đã ghim (không xoá được gì) — caller phải break
     * vòng lặp trim, nếu không sẽ lặp vô hạn vì itemCount không bao giờ giảm nữa.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun removeOldestUnpinnedHistoryElement(): Boolean {
        val index = history.indexOfFirst { !it.isPinned }
        if (index == -1) return false
        this.history.removeAt(index)
        recomputeFilter()
        notifyDataSetChanged()
        return true
    }

    /**
     * E-UI-5: swipe-to-delete 1 dòng — [adapterPosition] là vị trí ĐANG HIỂN THỊ (đã qua filter
     * nếu có), map về index thật qua [indexAt]. Trả về entry vừa xoá (cho caller tự persist +
     * làm Undo — KHÔNG có callback onDelete ở đây: persist phải đọc lại từ disk bên trong
     * historyMutex như mọi write-path khác, snapshot RAM tại đây có thể đã cũ so với lúc coroutine
     * persist thực sự chạy), hoặc null nếu vị trí không hợp lệ.
     */
    fun removeAt(adapterPosition: Int): History? {
        // Check trước bằng itemCount (đã tính đúng cho cả 2 trường hợp filter/không filter) —
        // gọi indexAt() trực tiếp với vị trí ngoài phạm vi khi đang filter sẽ ném
        // IndexOutOfBoundsException từ filteredIndices!!.get(), không trả về null như doc hứa.
        if (adapterPosition !in 0 until itemCount) return null
        val index = indexAt(adapterPosition)
        if (index !in history.indices) return null
        val removed = history.removeAt(index)
        recomputeFilter()
        // notifyItemRemoved (không phải notifyDataSetChanged) để không cắt ngang animation
        // swipe-out có sẵn của ItemTouchHelper.
        notifyItemRemoved(adapterPosition)
        return removed
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearHistory() {
        this.history.clear()
        recomputeFilter()
        notifyDataSetChanged()
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val calculation: TextView = itemView.findViewById(R.id.historyItemCalculation)
        private val result: TextView = itemView.findViewById(R.id.historyItemResult)
        private val time: TextView = itemView.findViewById(R.id.historyTime)
        private val separator: View = itemView.findViewById(R.id.historySeparator)
        private val sameDateSeparator: View =
            itemView.findViewById(R.id.historySameDateSeparator)
        private val pin: ImageView = itemView.findViewById(R.id.ivHistoryPin)

        private fun resolveThemeAttrColor(attrResId: Int): Int {
            val typedValue = TypedValue()
            itemView.context.theme.resolveAttribute(attrResId, typedValue, true)
            return typedValue.data
        }

        fun bind(actualIndex: Int, adapterPosition: Int) {
            val historyElement = history[actualIndex]
            // Set calculation, result and time
            calculation.text = historyElement.calculation
            result.text = historyElement.result
            // To avoid crashes with former histories that do not have stored dates
            if (historyElement.time.isNullOrEmpty() == true) {
                time.visibility = View.GONE
            } else {
                time.text = DateUtils.getRelativeTimeSpanString(
                    /* time = */ historyElement.time.toLong(),
                    /* now = */ System.currentTimeMillis(),
                    /* minResolution = */ DateUtils.DAY_IN_MILLIS,
                    /* flags = */ DateUtils.FORMAT_ABBREV_RELATIVE,
                )
                // Check if the former VISIBLE result has the same date -> hide the date
                // N-DATA-3: so sánh với entry liền trước trong danh sách ĐANG HIỂN THỊ (qua
                // indexAt), không phải liền trước trong `history` gốc — khi đang lọc, 2 entry kề
                // nhau trên màn hình có thể cách xa nhau trong `history` thật.
                if (adapterPosition > 0) {
                    val prevElement = history[indexAt(adapterPosition - 1)]
                    if (
                        prevElement.time?.isNotEmpty() == true
                        && DateUtils.getRelativeTimeSpanString(
                            /* time = */ prevElement.time?.toLong() ?: 0,
                            /* now = */ System.currentTimeMillis(),
                            /* minResolution = */ DateUtils.DAY_IN_MILLIS,
                            /* flags = */ DateUtils.FORMAT_ABBREV_RELATIVE,
                        ) == time.text
                    ) {
                        time.visibility = View.GONE
                    } else {
                        time.visibility = View.VISIBLE
                    }
                } else {
                    time.visibility = View.VISIBLE
                }
                // Check if the next VISIBLE result has the same date -> hide the separator
                if (adapterPosition + 1 < itemCount) {
                    val nextElement = history[indexAt(adapterPosition + 1)]
                    if (
                        DateUtils.getRelativeTimeSpanString(
                            /* time = */ nextElement.time?.toLong() ?: 0,
                            /* now = */ System.currentTimeMillis(),
                            /* minResolution = */ DateUtils.DAY_IN_MILLIS,
                            /* flags = */ DateUtils.FORMAT_ABBREV_RELATIVE,
                        ) == time.text
                    ) {
                        separator.visibility = View.GONE
                        // Add more space when it's the same date than the next history element
                        sameDateSeparator.visibility = View.VISIBLE
                    } else {
                        separator.visibility = View.VISIBLE
                        sameDateSeparator.visibility = View.GONE
                    }
                } else {
                    separator.visibility = View.VISIBLE
                    sameDateSeparator.visibility = View.GONE
                }
            }

            // On click
            calculation.setOnClickListener {
                historyElement.calculation?.let {
                    onElementClick.invoke(it)
                }
            }
            result.setOnClickListener {
                historyElement.result?.let {
                    onElementClick.invoke(it)
                }
            }
            // N-UI-4: long-press TRÊN BẤT KỲ view con nào trong card (calculation/result đều
            // match_parent width, chồng gần kín chiều cao card) mở popup Copy/Share cho "calc =
            // result" — gắn thẳng vào itemView sẽ KHÔNG BAO GIỜ nhận được long-press vì 2 TextView
            // con đã tiêu thụ hết sự kiện trước, nên phải gắn trực tiếp vào calculation/result.
            val showCopyShareMenu = View.OnLongClickListener { anchor ->
                val combinedText = "${historyElement.calculation} = ${historyElement.result}"
                val popup = PopupMenu(itemView.context, anchor)
                popup.menu.add(0, 1, 0, itemView.context.getString(R.string.action_copy))
                popup.menu.add(0, 2, 1, itemView.context.getString(R.string.action_share))
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        1 -> {
                            val clipboardManager = itemView.context
                                .getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                            clipboardManager.setPrimaryClip(
                                ClipData.newPlainText(
                                    itemView.context.getString(R.string.clipboard_label_copied_calc_and_result),
                                    combinedText
                                )
                            )
                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                                Toast.makeText(itemView.context, R.string.value_copied, Toast.LENGTH_SHORT)
                                    .show()
                            true
                        }

                        2 -> {
                            (itemView.context as Activity).share(combinedText)
                            true
                        }

                        else -> false
                    }
                }
                popup.show()
                true
            }
            calculation.setOnLongClickListener(showCopyShareMenu)
            result.setOnLongClickListener(showCopyShareMenu)
            itemView.setOnLongClickListener(showCopyShareMenu)

            // Pin state
            if (historyElement.isPinned) {
                pin.setImageResource(R.drawable.ic_baseline_favorite_24)
                pin.setColorFilter(ContextCompat.getColor(itemView.context, R.color.history_pin_active))
            } else {
                pin.setImageResource(R.drawable.ic_baseline_favorite_border_24)
                pin.setColorFilter(resolveThemeAttrColor(R.attr.text_second_color))
            }
            pin.setOnClickListener {
                val currentPosition = bindingAdapterPosition
                if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                val currentIndex = indexAt(currentPosition)
                history[currentIndex] = history[currentIndex].copy(
                    isPinned = !history[currentIndex].isPinned
                )
                notifyItemChanged(currentPosition)
                onPinToggle(history.toList())
            }
        }
    }
}
