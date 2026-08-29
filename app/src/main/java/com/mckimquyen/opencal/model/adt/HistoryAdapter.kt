package com.mckimquyen.opencal.model.adt

import android.annotation.SuppressLint
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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mckimquyen.opencal.R
import com.mckimquyen.opencal.model.History

class HistoryAdapter(
    private var history: MutableList<History>,
    private val onElementClick: (value: String) -> Unit,
    // N-DATA-4: báo cho MainActivity snapshot mới nhất của toàn bộ list sau khi user ghim/bỏ ghim,
    // để persist vào SharedPreferences — adapter chỉ giữ state UI, không tự đụng prefs.
    private val onPinToggle: (updatedHistory: List<History>) -> Unit = {},
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.i_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun getItemCount(): Int = history.size

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(history[position], position)
    }

    fun appendHistory(historyList: List<History>) {
        val startPosition = this.history.size
        this.history.addAll(historyList)
        notifyItemRangeInserted(startPosition, historyList.size)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun appendOneHistoryElement(history: History) {
        this.history.add(history)
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
        notifyDataSetChanged()
        return true
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearHistory() {
        this.history.clear()
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

        fun bind(historyElement: History, position: Int) {
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
                // Check if the former result has the same date -> hide the date
                if (position > 0) {
                    if (
                        history[position - 1].time?.isNotEmpty() == true
                        && DateUtils.getRelativeTimeSpanString(
                            /* time = */ history[position - 1].time?.toLong() ?: 0,
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
                // Check if the next result has the same date -> hide the separator
                if (position + 1 < history.size) {
                    if (
                        DateUtils.getRelativeTimeSpanString(
                            /* time = */ history[position + 1].time?.toLong() ?: 0,
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
            calculation.setOnLongClickListener {
                val clipboardManager =
                    itemView.context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText(
                        itemView.context.getString(R.string.clipboard_label_copied_calculation),
                        historyElement.calculation
                    )
                )
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                    Toast.makeText(itemView.context, R.string.value_copied, Toast.LENGTH_SHORT)
                        .show()


                true // Or false if not consumed
            }
            result.setOnLongClickListener {
                val clipboardManager =
                    itemView.context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText(
                        itemView.context.getString(R.string.clipboard_label_copied_history_result),
                        historyElement.result
                    )
                )
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                    Toast.makeText(itemView.context, R.string.value_copied, Toast.LENGTH_SHORT)
                        .show()
                true // Or false if not consumed
            }

            // Long-press on entire card to copy both calculation and result
            itemView.setOnLongClickListener {
                val clipboardManager =
                    itemView.context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val combinedText = "${historyElement.calculation} = ${historyElement.result}"
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
                history[currentPosition] = history[currentPosition].copy(
                    isPinned = !history[currentPosition].isPinned
                )
                notifyItemChanged(currentPosition)
                onPinToggle(history.toList())
            }
        }
    }
}
