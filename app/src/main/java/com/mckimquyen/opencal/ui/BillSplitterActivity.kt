package com.mckimquyen.opencal.ui

import android.os.Bundle
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.mckimquyen.opencal.BaseActivity
import com.mckimquyen.opencal.R
import com.mckimquyen.opencal.databinding.ABillSplitterBinding
import com.mckimquyen.opencal.db.MyPreferences
import com.mckimquyen.opencal.helper.NumberFormatter
import com.mckimquyen.opencal.model.Themes
import java.text.DecimalFormatSymbols
import kotlin.math.roundToInt

/**
 * X-UI-0: chia hoá đơn + tip cho N người — tính năng độc quyền so với calculator cơ bản khác.
 * Số học thuần (không phụ thuộc parser Calculator), chỉ tái dùng NumberFormatter để hiện số
 * theo đúng dấu thập phân/phân nhóm của locale (đồng bộ quy ước chung của project).
 */
class BillSplitterActivity : BaseActivity() {
    private companion object {
        private const val KEY_PEOPLE_COUNT = "people_count"
    }

    private lateinit var binding: ABillSplitterBinding

    private val decimalSeparatorSymbol =
        DecimalFormatSymbols.getInstance().decimalSeparator.toString()
    private val groupingSeparatorSymbol =
        DecimalFormatSymbols.getInstance().groupingSeparator.toString()

    // Review fix: giữ peopleCount qua xoay màn hình/config change (etBillAmount và sbTipPercent
    // tự lưu qua onSaveInstanceState vì là View có id, peopleCount là field thuần thì không).
    private var peopleCount = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        peopleCount = savedInstanceState?.getInt(KEY_PEOPLE_COUNT, 2) ?: 2

        val themes = Themes(this)
        themes.applyDayNightOverride()
        setTheme(themes.getTheme())

        if (MyPreferences(this).theme == 1) { // Amoled theme
            window.statusBarColor = ContextCompat.getColor(this, R.color.amoled_background_color)
        } else {
            window.statusBarColor = ContextCompat.getColor(this, R.color.background_color)
        }

        binding = ABillSplitterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBillSplitterBack.setOnClickListener { finish() }

        binding.tvPeopleCount.text = peopleCount.toString()
        binding.btnPeopleMinus.setOnClickListener {
            if (peopleCount > 1) {
                peopleCount--
                binding.tvPeopleCount.text = peopleCount.toString()
                recalculate()
            }
        }
        binding.btnPeoplePlus.setOnClickListener {
            peopleCount++
            binding.tvPeopleCount.text = peopleCount.toString()
            recalculate()
        }

        binding.etBillAmount.doAfterTextChanged { recalculate() }

        binding.sbTipPercent.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvTipValue.text = getString(R.string.bill_splitter_tip_value, progress)
                recalculate()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Review fix: giá trị hiển thị ban đầu lấy từ string resource (localizable) thay vì
        // hardcode "10%" trong layout XML.
        binding.tvTipValue.text = getString(R.string.bill_splitter_tip_value, binding.sbTipPercent.progress)
        recalculate()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PEOPLE_COUNT, peopleCount)
    }

    private fun parseBillAmount(): Double {
        val raw = binding.etBillAmount.text.toString()
        if (raw.isEmpty()) return 0.0
        return raw.replace(decimalSeparatorSymbol, ".").toDoubleOrNull() ?: 0.0
    }

    private fun formatAmount(amount: Double): String {
        return NumberFormatter.format(
            amount.toString().replace(".", decimalSeparatorSymbol),
            decimalSeparatorSymbol,
            groupingSeparatorSymbol
        )
    }

    private fun recalculate() {
        val billAmount = parseBillAmount()
        val tipPercent = binding.sbTipPercent.progress
        val totalWithTip = billAmount * (1.0 + tipPercent / 100.0)
        val perPerson = totalWithTip / peopleCount

        binding.tvTotalWithTip.text = formatAmount(totalWithTip)
        binding.tvPerPerson.text = formatAmount((perPerson * 100).roundToInt() / 100.0)
    }
}
