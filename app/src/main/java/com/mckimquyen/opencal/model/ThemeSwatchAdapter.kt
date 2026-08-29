package com.mckimquyen.opencal.model

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mckimquyen.opencal.databinding.ItemThemeSwatchBinding

/**
 * X-UI-1: dữ liệu 1 lựa chọn theme trong dialog chọn theme dạng lưới-preview.
 * [backgroundColor]/[accentColor] = null nghĩa là màu động (Material You "System"), sẽ được
 * resolve từ theme hiện tại của Activity đang mở dialog thay vì hardcode.
 */
data class ThemeSwatch(
    val styleIndex: Int,
    val label: String,
    val backgroundColor: Int?,
    val accentColor: Int?,
)

class ThemeSwatchAdapter(
    private val items: List<ThemeSwatch>,
    private var selectedIndex: Int,
    private val onSelected: (ThemeSwatch) -> Unit,
) : RecyclerView.Adapter<ThemeSwatchAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemThemeSwatchBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemThemeSwatchBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val binding = holder.binding
        val isSelected = position == selectedIndex

        binding.tvSwatchLabel.text = item.label
        binding.tvSwatchLabel.setTypeface(null, if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

        // Review fix: RecyclerView grid thay cho setSingleChoiceItems() làm mất accessibility
        // mặc định (TalkBack không còn biết theme nào đang chọn). Gộp cả row thành 1 node có
        // contentDescription + isSelected để TalkBack đọc đúng "label, đã chọn".
        binding.root.contentDescription = item.label
        binding.root.isSelected = isSelected

        binding.vSwatchBg.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(item.backgroundColor ?: android.graphics.Color.LTGRAY)
        }

        if (item.accentColor != null) {
            binding.vSwatchAccent.visibility = View.VISIBLE
            binding.vSwatchAccent.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(item.accentColor)
            }
        } else {
            binding.vSwatchAccent.visibility = View.GONE
        }

        binding.vSwatchRing.background = if (isSelected) {
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setStroke(6, item.accentColor ?: item.backgroundColor ?: android.graphics.Color.DKGRAY)
            }
        } else {
            null
        }

        binding.root.setOnClickListener {
            val previousSelected = selectedIndex
            selectedIndex = position
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedIndex)
            onSelected(item)
        }
    }
}
