package com.mckimquyen.opencal.ui

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

// F-UI-4: chặn paste ở onTextContextMenuItem — điểm chung DUY NHẤT mà cả context menu (nút
// "Paste" trên ActionMode nổi) LẪN phím tắt/paste chương trình (KEYCODE_PASTE) đều đi qua. Chỉ ẩn
// menu item qua customSelectionActionModeCallback (xem MainActivity) không chặn được KEYCODE_PASTE.
class CalculatorInputEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {

    override fun onTextContextMenuItem(id: Int): Boolean {
        if (id == android.R.id.paste || id == android.R.id.pasteAsPlainText) {
            return false
        }
        return super.onTextContextMenuItem(id)
    }

    // EditText luôn đảm bảo có buffer Editable (không bao giờ null) — khai báo lại non-null ở
    // đây để binding.input.text tại MainActivity.kt vẫn dùng được như platform type EditText cũ,
    // không phải sửa toàn bộ ~23 chỗ gọi vì Kotlin coi lớp custom trong cùng module là nullable-strict.
    override fun getText(): Editable = super.getText()!!
}
