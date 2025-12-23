package com.mckimquyen.opencal.model.adt

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Build
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.mckimquyen.opencal.R
import com.mckimquyen.opencal.model.History

class HistoryAdapter(
    private var history: MutableList<History>,
    private val onElementClick: (value: String) -> Unit,
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
        this.history.addAll(historyList)
        notifyItemRangeInserted(
            this.history.size,
            historyList.size - 1
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    fun appendOneHistoryElement(history: History) {
        this.history.add(history)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun removeFirstHistoryElement() {
        this.history.removeAt(0)
        notifyDataSetChanged()
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
                        "Copied history calculation",
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
                        "Copied history result",
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
                        "Copied calculation and result",
                        combinedText
                    )
                )
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                    Toast.makeText(itemView.context, R.string.value_copied, Toast.LENGTH_SHORT)
                        .show()
                true
            }
        }
    }
}
