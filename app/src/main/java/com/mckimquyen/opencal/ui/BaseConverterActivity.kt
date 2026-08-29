package com.mckimquyen.opencal.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.core.content.ContextCompat
import com.mckimquyen.opencal.BaseActivity
import com.mckimquyen.opencal.R
import com.mckimquyen.opencal.databinding.ABaseConverterBinding
import com.mckimquyen.opencal.db.MyPreferences
import com.mckimquyen.opencal.model.Themes
import com.roy.sdkadbmob.UIUtils

/**
 * N-CALC-2: đổi hệ số DEC/HEX/OCT/BIN cho số nguyên không âm (tối đa Long.MAX_VALUE). Tách thành
 * màn hình riêng thay vì tích hợp vào Calculator.kt — bàn phím theo hệ số (0-9,A-F tuỳ base) và
 * việc parser hiện tại chỉ làm việc với Double, không có khái niệm hệ số, khiến việc nhét thêm
 * vào luồng tính toán chính rủi ro cao hơn nhiều so với 1 màn hình độc lập (giống Bill Splitter).
 */
class BaseConverterActivity : BaseActivity() {
    private lateinit var binding: ABaseConverterBinding

    // Chặn vòng lặp vô hạn: sửa field A -> cập nhật field B/C/D -> TextWatcher của B/C/D lại kích
    // hoạt cập nhật ngược. Trong lúc đang tự cập nhật (isUpdating=true), bỏ qua toàn bộ callback.
    private var isUpdating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val themes = Themes(this)
        themes.applyDayNightOverride()
        setTheme(themes.getTheme())

        if (MyPreferences(this).theme == 1) { // Amoled theme
            window.statusBarColor = ContextCompat.getColor(this, R.color.amoled_background_color)
        } else {
            window.statusBarColor = ContextCompat.getColor(this, R.color.background_color)
        }

        // F-EDGE-1: thiếu cặp setupEdgeToEdge1/2 (có ở mọi Activity khác trong app) khiến
        // targetSdk 37 (Android 15+ ép edge-to-edge) làm title/back-arrow bị status bar đè lên.
        UIUtils.setupEdgeToEdge1(window)
        binding = ABaseConverterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UIUtils.setupEdgeToEdge2(
            rootView = findViewById(R.id.layoutRoot),
            paddingTop = true,
            paddingBottom = true
        )

        binding.ivBaseConverterBack.setOnClickListener { finish() }

        bindField(binding.etDec, 10)
        bindField(binding.etHex, 16)
        bindField(binding.etOct, 8)
        bindField(binding.etBin, 2)
    }

    private fun bindField(editText: EditText, radix: Int) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                val text = s?.toString().orEmpty()
                // toLongOrNull tự trả null cho ký tự sai/tràn số (vd 64 chữ số nhị phân) — coi
                // như "chưa hợp lệ", không crash, chỉ đơn giản không cập nhật các ô còn lại.
                val value = if (text.isEmpty()) null else text.toLongOrNull(radix)
                propagate(editText, value)
            }
        })
    }

    private fun propagate(source: EditText, value: Long?) {
        isUpdating = true
        val fields = listOf(binding.etDec to 10, binding.etHex to 16, binding.etOct to 8, binding.etBin to 2)
        for ((field, radix) in fields) {
            if (field === source) continue
            val text = value?.toString(radix) ?: ""
            field.setText(if (radix == 16) text.uppercase() else text)
        }
        isUpdating = false
    }
}
