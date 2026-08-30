package com.mckimquyen.opencal.ui

import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.MenuItem
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mckimquyen.opencal.BaseActivity
import com.mckimquyen.opencal.BuildConfig
import com.mckimquyen.opencal.R
import com.mckimquyen.opencal.databinding.AMainBinding
import com.mckimquyen.opencal.db.MyPreferences
import com.mckimquyen.opencal.feature.vip.VipActivity
import com.mckimquyen.opencal.ext.Calculator
import com.mckimquyen.opencal.ext.DomainErrorReason
import com.mckimquyen.opencal.ext.division_by_0
import com.mckimquyen.opencal.ext.domain_error
import com.mckimquyen.opencal.ext.domain_error_reason
import com.mckimquyen.opencal.ext.openUrlInBrowser
import com.mckimquyen.opencal.ext.rateApp
import com.mckimquyen.opencal.ext.syntax_error
import com.mckimquyen.opencal.helper.Expression
import com.mckimquyen.opencal.helper.NumberFormatter
import com.mckimquyen.opencal.helper.PhysicalConstants
import com.mckimquyen.opencal.model.History
import com.mckimquyen.opencal.model.Themes
import com.mckimquyen.opencal.model.adt.HistoryAdapter
import com.mckimquyen.opencal.util.Logger
import com.roy.sdkadbmob.AdManager
import com.sothree.slidinguppanel.PanelSlideListener
import com.sothree.slidinguppanel.PanelState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.jvm.java

var appLanguage: Locale = Locale.getDefault()

class MainActivity : BaseActivity() {
    private lateinit var view: View

    private val decimalSeparatorSymbol =
        DecimalFormatSymbols.getInstance().decimalSeparator.toString()
    private val groupingSeparatorSymbol =
        DecimalFormatSymbols.getInstance().groupingSeparator.toString()
    private var isInvButtonClicked = false
    private var isEqualLastAction = false

    // F-CALC-7: division_by_0/domain_error/syntax_error là global var dùng chung giữa
    // updateResultDisplay() (mỗi keystroke) và equalsButton(). Cancel job cũ trước khi launch
    // job mới để đảm bảo chỉ 1 evaluate()+đọc-cờ-lỗi chạy tại 1 thời điểm, tránh interleave.
    private var calculationJob: Job? = null

    // Review fix: calculationJob.cancel() chỉ hợp tác tại điểm suspend, nên 1 job cũ đang chạy
    // giữa evaluate() (không có suspend point) vẫn có thể ghi đè cờ lỗi của job mới sau khi đã
    // bị "cancel". Mutex đảm bảo tại 1 thời điểm chỉ 1 chuỗi reset-evaluate-đọc-cờ chạy trọn vẹn.
    private val calculationMutex = Mutex()

    // Review fix: history đọc-sửa-ghi ở equalsButton() và job trim ở onResume() là 2 coroutine
    // độc lập cùng đụng vào prefs.history — không có mutex thì job chạy sau có thể ghi đè bằng
    // snapshot cũ, làm mất entry vừa lưu.
    private val historyMutex = Mutex()

    // Memory M+/M-/MR/MC — chỉ lưu trong phiên (reset khi thoát app, giống calculator vật lý).
    private var memoryValue: Double? = null

    // N-CALC-6: kết quả "=" gần nhất, dùng cho token "ans" trong biểu thức tiếp theo.
    private var lastAnswer: Double? = null

    // N-DATA-1: SAF launcher phải đăng ký ở property (trước khi Activity STARTED) — đăng ký trong
    // hàm onClick lúc user bấm nút sẽ crash (IllegalStateException từ ActivityResultRegistry).
    private val exportHistoryLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch(Dispatchers.IO) {
            val exported = try {
                val csv = buildHistoryCsv(prefs.getHistory())
                contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
                true
            } catch (e: Exception) {
                false
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    if (exported) R.string.export_history_success else R.string.export_history_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private var isDegreeModeActivated = true // Set degree by default
    private var errorStatusOld = false

    private lateinit var binding: AMainBinding
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var historyLayoutMgr: LinearLayoutManager

    // Cache preferences thay vì tạo mới mỗi lần dùng (vd keyVibration chạy mỗi phím bấm).
    // MyPreferences đọc giá trị lúc khởi tạo nên được làm mới trong onResume để bắt
    // thay đổi từ SettingsActivity khi quay lại.
    private lateinit var prefs: MyPreferences

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = MyPreferences(this)

        // Themes
        val themes = Themes(this)
        themes.applyDayNightOverride()
        setTheme(themes.getTheme())

        // E-INFRA-6: setupEdgeToEdge1/2 giờ tự động chạy trong BaseActivity.setContentView().
        binding = AMainBinding.inflate(layoutInflater)
        view = binding.root
        setContentView(view)

        AdManager.setCurrentActivity(this)
        AdManager.loadInterstitial(this)

        // Chip VIP badge: bấm vào mở VIP screen (không show interstitial khi đã là VIP).
        // Nullable vì chỉ có ở layout portrait (MainActivity khoá portrait).
        binding.chipVipBadge?.setOnClickListener {
            startActivity(Intent(this, VipActivity::class.java), null)
        }

        // Memory M+/M-/MR/MC: MR/MC mờ đi khi chưa có gì trong bộ nhớ.
        refreshMemoryButtonsState()

        // Disable the keyboard on display EditText
        binding.input.showSoftInputOnFocus = false

        // https://www.geeksforgeeks.org/how-to-detect-long-press-in-android/
        binding.backspaceButton.setOnLongClickListener {
            binding.input.setText("")
            binding.resultDisplay.setText("")
            true
        }

        // Set default animations and disable the fade out default animation
        // https://stackoverflow.com/questions/19943466/android-animatelayoutchanges-true-what-can-i-do-if-the-fade-out-effect-is-un
        val lt = LayoutTransition()
        lt.disableTransitionType(LayoutTransition.DISAPPEARING)
        binding.tableLayout.layoutTransition = lt

        // Set decimalSeparator
        binding.pointButton.setImageResource(if (decimalSeparatorSymbol == ",") R.drawable.ic_comma else R.drawable.ic_dot)

        // Set history
        historyLayoutMgr = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL,
            false
        )
        binding.rvHistory.layoutManager = historyLayoutMgr
        historyAdapter = HistoryAdapter(
            history = mutableListOf(),
            onElementClick = { value ->
                //val valueUpdated = value.replace(".", NumberFormatter.decimalSeparatorSymbol)
                updateDisplay(window.decorView, value)
            },
            // N-DATA-4: adapter đã tự cập nhật state ghim trong RAM, ở đây chỉ cần persist
            // snapshot mới nhất xuống SharedPreferences (đồng bộ qua historyMutex như mọi chỗ
            // khác đọc/ghi cùng blob history).
            onPinToggle = { updatedHistory ->
                lifecycleScope.launch(Dispatchers.IO) {
                    historyMutex.withLock {
                        prefs.saveHistory(this@MainActivity, updatedHistory.toMutableList())
                    }
                }
            },
        )
        binding.rvHistory.adapter = historyAdapter
        // F-DATA-2: đọc + parse JSON toàn bộ history không được block Main thread lúc mở app
        lifecycleScope.launch(Dispatchers.IO) {
            val historyList = prefs.getHistory()
            withContext(Dispatchers.Main) {
                historyAdapter.appendHistory(historyList)
                // Scroll to the bottom of the recycle view
                if (historyAdapter.itemCount > 0) {
                    binding.rvHistory.scrollToPosition(historyAdapter.itemCount - 1)
                }
            }
        }

        // N-DATA-3: lọc history theo biểu thức/kết quả — không cần debounce, filter() chỉ
        // duyệt mảng trong RAM (không I/O), rẻ hơn nhiều so với việc thêm Handler/coroutine delay.
        binding.etHistorySearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                historyAdapter.filter(s?.toString().orEmpty())
            }
        })

        binding.slidingLayout.addPanelSlideListener(object : PanelSlideListener {
            override fun onPanelSlide(panel: View, slideOffset: Float) {
                if (slideOffset == 0f) { // If the panel got collapsed
                    binding.slidingLayout.scrollableView = binding.rvHistory
                }
            }

            override fun onPanelStateChanged(
                panel: View,
                previousState: PanelState,
                newState: PanelState,
            ) {
                if (newState == PanelState.ANCHORED) { // To prevent the panel from getting stuck in the middle
                    binding.slidingLayout.panelState = PanelState.EXPANDED
                }
            }
        })

        // Prevent the phone from sleeping (if option enabled)
        if (prefs.preventPhoneFromSleepingMode) {
            view.keepScreenOn = true
        }

        // scientific mode enabled by default (if option enabled)
        if (prefs.scientificMode) {
            enableOrDisableScientistMode()
        }

        // use radians instead of degrees by default (if option enabled)
        if (prefs.useRadiansByDefault) {
            enableOrDisableDegreeMode()
        }

        // Focus by default
        binding.input.requestFocus()

        // Makes the input take the whole width of the screen by default
        val screenWidthPX = resources.displayMetrics.widthPixels
        binding.input.minWidth =
            screenWidthPX - (binding.input.paddingRight + binding.input.paddingLeft) // remove the paddingHorizontal

        // Do not clear after equal button if you move the cursor
        binding.input.accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun sendAccessibilityEvent(host: View, eventType: Int) {
                super.sendAccessibilityEvent(host, eventType)
                if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
                    isEqualLastAction = false
                }
                if (!binding.input.isCursorVisible) {
                    binding.input.isCursorVisible = true
                }
            }
        }

        // LongClick on result to copy it
        binding.resultDisplay.setOnLongClickListener {
            when {
                binding.resultDisplay.text.toString() != "" -> {
                    val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    clipboardManager.setPrimaryClip(
                        ClipData.newPlainText(
                            getString(R.string.clipboard_label_copied_result),
                            binding.resultDisplay.text
                        )
                    )
                    // Only show a toast for Android 12 and lower.
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                        Toast.makeText(
                            this,
                            R.string.value_copied,
                            Toast.LENGTH_SHORT
                        ).show()
                    true
                }

                else -> false
            }
        }

        // Handle changes into input to update resultDisplay
        binding.input.addTextChangedListener(object : TextWatcher {
            private var beforeTextLength = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                beforeTextLength = s?.length ?: 0
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateResultDisplay()
                /*val afterTextLength = s?.length ?: 0
                // If the afterTextLength is equals to 0 we have to clear resultDisplay
                if (afterTextLength == 0) {
                    binding.resultDisplay.setText("")
                }

                /* we check if the length of the text entered into the EditText
                is greater than the length of the text before the change (beforeTextLength)
                by more than 1 character. If it is, we assume that this is a paste event. */
                val clipData = clipboardManager.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    //val clipText = clipData.getItemAt(0).coerceToText(this@MainActivity).toString()

                    if (s != null) {
                        //val newValue = s.subSequence(start, start + count).toString()
                        if (
                            (afterTextLength - beforeTextLength > 1)
                            // Removed to avoid anoying notification (https://developer.android.com/develop/ui/views/touch-and-input/copy-paste#PastingSystemNotifications)
                            //|| (afterTextLength - beforeTextLength >= 1 && clipText == newValue) // Supports 1+ new caractere if it is equals to the latest element from the clipboard
                        ) {
                            // Handle paste event here
                            updateResultDisplay()
                        }
                    }
                }*/
            }

            override fun afterTextChanged(s: Editable?) {
                // Do nothing
            }
        })
    }

    fun selectThemeDialog(menuItem: MenuItem) {
        Themes.openDialogThemeSelector(this)
    }

    fun openAppMenu(view: View) {
        val popup = PopupMenu(this, view)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.menu_app, popup.menu)

        // Update Show/Hide History menu item based on panel state
        val historyMenuItem = popup.menu.findItem(R.id.menu_show_history)
        if (binding.slidingLayout.panelState == PanelState.EXPANDED) {
            historyMenuItem?.title = getString(R.string.app_menu_hide_history)
        } else {
            historyMenuItem?.title = getString(R.string.app_menu_show_history)
        }

        popup.show()
    }

    fun openAbout(menuItem: MenuItem) {
        AdManager.showInterstitial(this) { success ->
            if (success) {
                Logger.d("Ad đã hiển thị và đóng thành công")
            } else {
                Logger.d("Ad không hiển thị được hoặc có lỗi")
            }
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent, null)
        }
    }

    fun openSettings(menuItem: MenuItem) {
        AdManager.showInterstitial(this) { success ->
            if (success) {
                Logger.d("Ad đã hiển thị và đóng thành công")
            } else {
                Logger.d("Ad không hiển thị được hoặc có lỗi")
            }
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent, null)
        }
    }

    fun openBillSplitter(menuItem: MenuItem) {
        val intent = Intent(this, BillSplitterActivity::class.java)
        startActivity(intent, null)
    }

    fun openBaseConverter(menuItem: MenuItem) {
        val intent = Intent(this, BaseConverterActivity::class.java)
        startActivity(intent, null)
    }

    /**
     * Chip badge "VIP" ở action bar — LUÔN hiển thị, đổi giao diện theo trạng thái:
     * - Đang VIP: chip vàng đặc (premium rõ ràng).
     * - Free: chip viền theo màu chữ theme (gợi ý "chạm để lên VIP").
     * Refresh trong onResume (bắt thay đổi khi back từ VipActivity).
     */
    private var chipPulseAnimator: ObjectAnimator? = null

    private fun bindVipBadge() {
        val chip = binding.chipVipBadge ?: return
        chip.isVisible = true
        chip.text = getString(R.string.vip_badge)
        // Revamp: chip cân đối với nút menu 3 chấm (24dp) bên cạnh — icon co lại còn 10dp.
        chip.chipIconSize = resources.displayMetrics.density * 10f
        if (AdManager.isVipByKeyActive()) {
            chip.setChipBackgroundColorResource(R.color.vip_gold)
            chip.chipStrokeWidth = 0f
            val onGold = ContextCompat.getColor(this, R.color.vip_on_gold)
            chip.setTextColor(onGold)
            chip.chipIconTint = ColorStateList.valueOf(onGold)
        } else {
            val fg = themedColor(R.attr.text_color, 0xFF888888.toInt())
            chip.setChipBackgroundColorResource(android.R.color.transparent)
            chip.chipStrokeWidth = resources.displayMetrics.density // 1dp
            chip.chipStrokeColor = ColorStateList.valueOf(fg)
            chip.setTextColor(fg)
            chip.chipIconTint = ColorStateList.valueOf(fg)
        }
    }

    /** Resolve màu từ themed attr (vd R.attr.text_color), fallback nếu không có. */
    private fun themedColor(attr: Int, fallback: Int): Int {
        val tv = android.util.TypedValue()
        return if (theme.resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) ContextCompat.getColor(this, tv.resourceId) else tv.data
        } else fallback
    }

    /** #4: animation "thở" nhẹ + lung linh cho chip VIP (premium, vẫn tinh tế). */
    private fun startChipPulse() {
        val chip = binding.chipVipBadge ?: return
        chipPulseAnimator?.cancel()
        chipPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
            chip,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.06f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.06f),
        ).apply {
            duration = 1300L
            interpolator = AccelerateDecelerateInterpolator()
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    private fun stopChipPulse() {
        chipPulseAnimator?.cancel()
        chipPulseAnimator = null
        binding.chipVipBadge?.apply { scaleX = 1f; scaleY = 1f }
    }

    fun openGithub(menuItem: MenuItem) {
        this.openUrlInBrowser("https://github.com/gj-loitp/5_OpenCalc")
    }

    fun openSourceCode(menuItem: MenuItem) {
        this.openUrlInBrowser("https://github.com/Darkempire78/OpenCalc")
    }

    fun openLicense(menuItem: MenuItem) {
        this.openUrlInBrowser("https://raw.githubusercontent.com/Darkempire78/OpenCalc/main/LICENSE")
    }

    fun joinTesterCommunity(menuItem: MenuItem) {
        showDialogTesterCommunity()
    }

    fun showHistory(menuItem: MenuItem) {
        // Expand the history panel
        if (binding.slidingLayout.panelState == PanelState.COLLAPSED) {
            binding.slidingLayout.panelState = PanelState.EXPANDED
        } else {
            binding.slidingLayout.panelState = PanelState.COLLAPSED
        }
    }

    fun clearHistory(menuItem: MenuItem) {
        // N-DATA-4: entry đã ghim phải sống sót qua "Clear History" — đó là mục đích của ghim.
        lifecycleScope.launch(Dispatchers.IO) {
            historyMutex.withLock {
                val remaining = prefs.getHistory().filter { it.isPinned }.toMutableList()
                prefs.saveHistory(this@MainActivity, remaining)
                withContext(Dispatchers.Main) {
                    historyAdapter.clearHistory()
                    if (remaining.isNotEmpty()) {
                        historyAdapter.appendHistory(remaining)
                    }
                }
            }
        }
    }

    fun exportHistory(menuItem: MenuItem) {
        lifecycleScope.launch(Dispatchers.IO) {
            val isEmpty = prefs.getHistory().isEmpty()
            withContext(Dispatchers.Main) {
                if (isEmpty) {
                    Toast.makeText(this@MainActivity, R.string.export_history_empty, Toast.LENGTH_SHORT).show()
                } else {
                    val fileName = "opencalc_history_" +
                            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) +
                            ".csv"
                    exportHistoryLauncher.launch(fileName)
                }
            }
        }
    }

    private fun buildHistoryCsv(history: List<History>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder("Calculation,Result,Time,Pinned\n")
        for (entry in history) {
            val timeText = entry.time?.toLongOrNull()?.let { dateFormat.format(Date(it)) } ?: ""
            sb.append(csvEscape(entry.calculation ?: "")).append(',')
            sb.append(csvEscape(entry.result ?: "")).append(',')
            sb.append(csvEscape(timeText)).append(',')
            sb.append(entry.isPinned).append('\n')
        }
        return sb.toString()
    }

    // RFC 4180: bọc field trong dấu ngoặc kép, escape dấu ngoặc kép bên trong bằng cách nhân đôi —
    // calculation/result có thể chứa dấu phẩy (groupingSeparatorSymbol của locale), nếu không quote
    // sẽ vỡ cấu trúc CSV.
    private fun csvEscape(field: String): String = "\"" + field.replace("\"", "\"\"") + "\""

    private fun keyVibration(view: View) {
        if (prefs.vibrationMode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }
    }

    private fun setErrorColor(errorStatus: Boolean) {
        // Only run if the color needs to be updated
        if (errorStatus != errorStatusOld) {
            // Set error color
            if (errorStatus) {
                binding.input.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.calculation_error_color
                    )
                )
                binding.resultDisplay.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.calculation_error_color
                    )
                )
            }
            // Clear error color
            else {
                binding.input.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.text_color
                    )
                )
                binding.resultDisplay.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.text_second_color
                    )
                )
            }
            errorStatusOld = errorStatus
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateDisplay(view: View, value: String) {
        // Reset input with current number if following "equal"
        if (isEqualLastAction) {
            val anyNumber = "0123456789$decimalSeparatorSymbol".toCharArray().map {
                it.toString()
            }
            if (anyNumber.contains(value)) {
                binding.input.setText("")
            } else {
                binding.input.setSelection(binding.input.text.length)
                binding.inputHorizontalScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT)
            }
            isEqualLastAction = false
        }

        if (!binding.input.isCursorVisible) {
            binding.input.isCursorVisible = true
        }

        // Vibrate when key pressed
        keyVibration(view)

        // F-UI-2: toàn bộ hàm này chỉ format string ngắn (không gọi Calculator().evaluate()),
        // chi phí không đáng kể — chạy thẳng trên Main để tránh đọc/ghi binding.input từ
        // background thread và tránh race condition khi tap nhanh (nhiều coroutine chồng chéo
        // từng snapshot state cũ trước khi ai đó kịp ghi state mới).
        val formerValue = binding.input.text.toString()
        val cursorPosition = binding.input.selectionStart
        val leftValue = formerValue.subSequence(0, cursorPosition).toString()
        val rightValue = formerValue.subSequence(cursorPosition, formerValue.length).toString()

        val newValue = leftValue + value + rightValue

        var newValueFormatted =
            NumberFormatter.format(newValue, decimalSeparatorSymbol, groupingSeparatorSymbol)

        // Avoid two decimalSeparator in the same number
        // 1. When you click on the decimalSeparator button
        if (value == decimalSeparatorSymbol && decimalSeparatorSymbol in binding.input.text.toString()) {
            if (binding.input.text.toString().isNotEmpty()) {
                var lastNumberBefore = ""
                if (cursorPosition > 0 && binding.input.text.toString()
                        .substring(0, cursorPosition)
                        .last() in "0123456789\\$decimalSeparatorSymbol"
                ) {
                    lastNumberBefore = NumberFormatter.extractNumbers(
                        binding.input.text.toString().substring(0, cursorPosition),
                        decimalSeparatorSymbol
                    ).last()
                }
                var firstNumberAfter = ""
                if (cursorPosition < binding.input.text.length - 1) {
                    firstNumberAfter = NumberFormatter.extractNumbers(
                        binding.input.text.toString()
                            .substring(cursorPosition, binding.input.text.length),
                        decimalSeparatorSymbol
                    ).first()
                }
                if (decimalSeparatorSymbol in lastNumberBefore || decimalSeparatorSymbol in firstNumberAfter) {
                    return
                }
            }
        }
        // 2. When you click on a former calculation from the history
        if (binding.input.text.isNotEmpty()
            && cursorPosition > 0
            && decimalSeparatorSymbol in value
            && value != decimalSeparatorSymbol // The value should not be *only* the decimal separator
        ) {
            if (NumberFormatter.extractNumbers(value, decimalSeparatorSymbol)
                    .isNotEmpty()
            ) {
                val firstValueNumber =
                    NumberFormatter.extractNumbers(value, decimalSeparatorSymbol).first()
                val lastValueNumber =
                    NumberFormatter.extractNumbers(value, decimalSeparatorSymbol).last()
                if (decimalSeparatorSymbol in firstValueNumber || decimalSeparatorSymbol in lastValueNumber) {
                    var numberBefore =
                        binding.input.text.toString().substring(0, cursorPosition)
                    if (numberBefore.last() !in "()*-/+^!√πe") {
                        numberBefore = NumberFormatter.extractNumbers(
                            numberBefore,
                            decimalSeparatorSymbol
                        ).last()
                    }
                    var numberAfter = ""
                    if (cursorPosition < binding.input.text.length - 1) {
                        numberAfter = NumberFormatter.extractNumbers(
                            binding.input.text.toString()
                                .substring(cursorPosition, binding.input.text.length),
                            decimalSeparatorSymbol
                        ).first()
                    }
                    var tmpValue = value
                    var numberBeforeParenthesisLength = 0
                    if (decimalSeparatorSymbol in numberBefore) {
                        numberBefore = "($numberBefore)"
                        numberBeforeParenthesisLength += 2
                    }
                    if (decimalSeparatorSymbol in numberAfter) {
                        tmpValue = "($value)"
                    }
                    val tmpNewValue = binding.input.text.toString().substring(
                        0,
                        (cursorPosition + numberBeforeParenthesisLength - numberBefore.length)
                    ) + numberBefore + tmpValue + rightValue
                    newValueFormatted = NumberFormatter.format(
                        tmpNewValue,
                        decimalSeparatorSymbol,
                        groupingSeparatorSymbol
                    )
                }
            }
        }

        // Update Display
        binding.input.setText(newValueFormatted)

        // Increase cursor position
        val cursorOffset = newValueFormatted.length - newValue.length
        binding.input.setSelection(cursorPosition + value.length + cursorOffset)
    }

    private fun roundResult(result: Double): Double {
        if (result.isNaN() || result.isInfinite()) {
            return result
        }
        return BigDecimal(result).setScale(
            prefs.numberPrecision!!.toInt(),
            RoundingMode.HALF_EVEN
        ).toDouble()
    }

    private fun enableOrDisableScientistMode() {
        if (binding.scientistModeRow2.visibility != View.VISIBLE) {
            binding.scientistModeRow2.visibility = View.VISIBLE
            binding.scientistModeRow3.visibility = View.VISIBLE
            binding.scientistModeRow4?.visibility = View.VISIBLE
            binding.scientistModeSwitchButton?.setImageResource(R.drawable.ic_baseline_keyboard_arrow_up_24)
            binding.degreeTextView.visibility = View.VISIBLE
            binding.degreeTextView.text = binding.degreeButton.text.toString()
        } else {
            binding.scientistModeRow2.visibility = View.GONE
            binding.scientistModeRow3.visibility = View.GONE
            binding.scientistModeRow4?.visibility = View.GONE
            binding.scientistModeSwitchButton?.setImageResource(R.drawable.ic_baseline_keyboard_arrow_down_24)
            binding.degreeTextView.visibility = View.GONE
            binding.degreeTextView.text = binding.degreeButton.text.toString()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun enableOrDisableDegreeMode() {
        if (binding.degreeButton.text.toString() == getString(R.string.degree_mode_deg)) {
            binding.degreeButton.text = getString(R.string.degree_mode_rad)
            isDegreeModeActivated = false
        } else {
            binding.degreeButton.text = getString(R.string.degree_mode_deg)
            isDegreeModeActivated = true
        }

        binding.degreeTextView.text = binding.degreeButton.text.toString()
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun updateResultDisplay() {
        calculationJob?.cancel()
        calculationJob = lifecycleScope.launch(Dispatchers.Default) {
            // F-UI-2: đọc binding.input phải trên Main thread (Editable không thread-safe)
            val calculation = withContext(Dispatchers.Main) {
                setErrorColor(false)
                binding.input.text.toString()
            }

            if (calculation != "") {
                calculationMutex.withLock {
                    division_by_0 = false
                    domain_error = false
                    domain_error_reason = null
                    syntax_error = false

                    val calculationTmp = Expression().getCleanExpression(
                        calculation,
                        decimalSeparatorSymbol,
                        groupingSeparatorSymbol,
                        lastAnswer
                    )
                    // F-UI-9: parser đệ quy xuống, biểu thức bất thường (nhiều ngoặc lồng nhau)
                    // có thể vượt stack — không để crash cả app.
                    var result = try {
                        Calculator().evaluate(calculationTmp, isDegreeModeActivated)
                    } catch (e: StackOverflowError) {
                        syntax_error = true
                        Double.NaN
                    }

                    // If result is a number and it is finite
                    if (!result.isNaN() && result.isFinite()) {
                        // Round at 10^-12
                        result = roundResult(result)
                        var formattedResult = NumberFormatter.format(
                            result.toString().replace(".", decimalSeparatorSymbol),
                            decimalSeparatorSymbol,
                            groupingSeparatorSymbol
                        )

                        // If result = -0, change it to 0
                        if (result == -0.0) {
                            result = 0.0
                        }
                        // If the double ends with .0 we remove the .0
                        if ((result * 10) % 10 == 0.0) {
                            val resultString = String.format("%.0f", result)
                            formattedResult = NumberFormatter.format(
                                resultString,
                                decimalSeparatorSymbol,
                                groupingSeparatorSymbol
                            )

                            withContext(Dispatchers.Main) {
                                if (formattedResult != calculation) {
                                    binding.resultDisplay.setText(formattedResult)
                                } else {
                                    binding.resultDisplay.setText("")
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                if (formattedResult != calculation) {
                                    binding.resultDisplay.setText(formattedResult)
                                } else {
                                    binding.resultDisplay.setText("")
                                }
                            }
                        }
                    } else withContext(Dispatchers.Main) {
                        if (result.isInfinite() && !division_by_0 && !domain_error) {
                            if (result < 0) binding.resultDisplay.setText("-" + getString(R.string.infinity))
                            else binding.resultDisplay.setText(getString(R.string.value_too_large))
                        } else {
                            withContext(Dispatchers.Main) {
                                binding.resultDisplay.setText("")
                            }
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    binding.resultDisplay.setText("")
                }
            }
        }
    }

    fun keyDigitPadMappingToDisplay(view: View) {
        updateDisplay(view, (view as Button).text as String)
    }

    @SuppressLint("SetTextI18n")
    private fun addSymbol(view: View, currentSymbol: String) {
        // Get input text length
        val textLength = binding.input.text.length

        // If the input is not empty
        if (textLength > 0) {
            // Get cursor's current position
            val cursorPosition = binding.input.selectionStart

            // Get next / previous characters relative to the cursor
            val nextChar =
                if (textLength - cursorPosition > 0) binding.input.text[cursorPosition].toString() else "0" // use "0" as default like it's not a symbol
            val previousChar =
                if (cursorPosition > 0) binding.input.text[cursorPosition - 1].toString() else "0"

            if (currentSymbol != previousChar // Ignore multiple presses of the same button
                && currentSymbol != nextChar
                && previousChar != "√" // No symbol can be added on an empty square root
                && previousChar != decimalSeparatorSymbol // Ensure that the previous character is not a comma
                && nextChar != decimalSeparatorSymbol // Ensure that the next character is not a comma
                && (previousChar != "(" // Ensure that we are not at the beginning of a parenthesis
                        || currentSymbol == "-")
            ) { // Minus symbol is an override
                // If previous character is a symbol, replace it
                if (previousChar.matches("[+\\-÷×^]".toRegex())) {
                    keyVibration(view)

                    val leftString =
                        binding.input.text.subSequence(0, cursorPosition - 1).toString()
                    val rightString =
                        binding.input.text.subSequence(cursorPosition, textLength).toString()

                    // Add a parenthesis if there is another symbol before minus
                    if (currentSymbol == "-") {
                        if (previousChar in "+-") {
                            binding.input.setText(leftString + currentSymbol + rightString)
                            binding.input.setSelection(cursorPosition)
                        } else {
                            binding.input.setText(leftString + previousChar + currentSymbol + rightString)
                            binding.input.setSelection(cursorPosition + 1)
                        }
                    } else if (cursorPosition > 1 && binding.input.text[cursorPosition - 2] != '(') {
                        binding.input.setText(leftString + currentSymbol + rightString)
                        binding.input.setSelection(cursorPosition)
                    } else if (currentSymbol == "+") {
                        binding.input.setText(leftString + rightString)
                        binding.input.setSelection(cursorPosition - 1)
                    }
                }
                // If next character is a symbol, replace it
                else if (nextChar.matches("[+\\-÷×^%!]".toRegex())
                    && currentSymbol != "%"
                ) { // Make sure that percent symbol doesn't replace succeeding symbols
                    keyVibration(view)

                    val leftString = binding.input.text.subSequence(0, cursorPosition).toString()
                    val rightString =
                        binding.input.text.subSequence(cursorPosition + 1, textLength).toString()

                    if (cursorPosition > 0 && previousChar != "(") {
                        binding.input.setText(leftString + currentSymbol + rightString)
                        binding.input.setSelection(cursorPosition + 1)
                    } else if (currentSymbol == "+") binding.input.setText(leftString + rightString)
                }
                // Otherwise just update the display
                else if (cursorPosition > 0 || nextChar != "0" && currentSymbol == "-") {
                    updateDisplay(view, currentSymbol)
                } else keyVibration(view)
            } else keyVibration(view)
        } else { // Allow minus symbol, even if the input is empty
            if (currentSymbol == "-") updateDisplay(view, currentSymbol)
            else keyVibration(view)
        }
    }

    fun addButton(view: View) {
        addSymbol(view, "+")
    }

    /** Đánh giá biểu thức hiện tại trong input, dùng cho M+/M- (không đụng UI/history). */
    private fun evaluateCurrentInputOrNull(calculation: String): Double? {
        if (calculation.isEmpty()) return null
        val calculationTmp = Expression().getCleanExpression(
            calculation,
            decimalSeparatorSymbol,
            groupingSeparatorSymbol,
            lastAnswer
        )
        val result = try {
            Calculator().evaluate(calculationTmp, isDegreeModeActivated)
        } catch (e: StackOverflowError) {
            Double.NaN
        }
        return if (!result.isNaN() && result.isFinite()) result else null
    }

    private fun refreshMemoryButtonsState() {
        val hasMemory = memoryValue != null
        binding.btnMemoryRecall?.isEnabled = hasMemory
        binding.btnMemoryRecall?.alpha = if (hasMemory) 1f else 0.4f
        binding.btnMemoryClear?.isEnabled = hasMemory
        binding.btnMemoryClear?.alpha = if (hasMemory) 1f else 0.4f
    }

    fun memoryAddButton(view: View) {
        keyVibration(view)
        calculationJob?.cancel()
        calculationJob = lifecycleScope.launch(Dispatchers.Default) {
            val calculation = withContext(Dispatchers.Main) { binding.input.text.toString() }
            calculationMutex.withLock {
                val result = evaluateCurrentInputOrNull(calculation) ?: return@withLock
                memoryValue = (memoryValue ?: 0.0) + result
                withContext(Dispatchers.Main) { refreshMemoryButtonsState() }
            }
        }
    }

    fun memorySubtractButton(view: View) {
        keyVibration(view)
        calculationJob?.cancel()
        calculationJob = lifecycleScope.launch(Dispatchers.Default) {
            val calculation = withContext(Dispatchers.Main) { binding.input.text.toString() }
            calculationMutex.withLock {
                val result = evaluateCurrentInputOrNull(calculation) ?: return@withLock
                memoryValue = (memoryValue ?: 0.0) - result
                withContext(Dispatchers.Main) { refreshMemoryButtonsState() }
            }
        }
    }

    fun memoryRecallButton(view: View) {
        val value = memoryValue ?: return
        keyVibration(view)
        updateDisplay(view, value.toString().replace(".", decimalSeparatorSymbol))
    }

    fun memoryClearButton(view: View) {
        keyVibration(view)
        memoryValue = null
        refreshMemoryButtonsState()
    }

    fun subtractButton(view: View) {
        addSymbol(view, "-")
    }

    fun divideButton(view: View) {
        addSymbol(view, "÷")
    }

    fun multiplyButton(view: View) {
        addSymbol(view, "×")
    }

    fun exponentButton(view: View) {
        addSymbol(view, "^")
    }

    fun pointButton(view: View) {
        updateDisplay(view, decimalSeparatorSymbol)
    }

    fun sineButton(view: View) {
        if (!isInvButtonClicked) {
            updateDisplay(view, "sin(")
        } else {
            updateDisplay(view, "sin⁻¹(")
        }
    }

    fun cosineButton(view: View) {
        if (!isInvButtonClicked) {
            updateDisplay(view, "cos(")
        } else {
            updateDisplay(view, "cos⁻¹(")
        }
    }

    fun tangentButton(view: View) {
        if (!isInvButtonClicked) {
            updateDisplay(view, "tan(")
        } else {
            updateDisplay(view, "tan⁻¹(")
        }
    }

    fun eButton(view: View) {
        updateDisplay(view, "e")
    }

    fun naturalLogarithmButton(view: View) {
        if (!isInvButtonClicked) {
            updateDisplay(view, "ln(")
        } else {
            updateDisplay(view, "exp(")
        }
    }

    fun logarithmButton(view: View) {
        if (!isInvButtonClicked) {
            updateDisplay(view, "log(")
        } else {
            updateDisplay(view, "10^")
        }
    }

    fun piButton(view: View) {
        updateDisplay(view, "π")
    }

    fun ansButton(view: View) {
        updateDisplay(view, getString(R.string.ans))
    }

    fun physicalConstantButton(view: View) {
        keyVibration(view)
        val labels = PhysicalConstants.ALL.map { it.label }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_physical_constant_title)
            .setItems(labels) { _, which ->
                updateDisplay(view, PhysicalConstants.ALL[which].token)
            }
            .show()
    }

    fun factorialButton(view: View) {
        addSymbol(view, "!")
    }

    fun squareButton(view: View) {
        if (!isInvButtonClicked) {
            updateDisplay(view, "√")
        } else {
            updateDisplay(view, "^2")
        }
    }

    fun divideBy100(view: View) {
        addSymbol(view, "%")
    }

    @SuppressLint("SetTextI18n")
    fun degreeButton(view: View) {
        keyVibration(view)
        enableOrDisableDegreeMode()
        updateResultDisplay()
    }

    fun invButton(view: View) {
        keyVibration(view)

        if (!isInvButtonClicked) {
            isInvButtonClicked = true

            // change buttons
            binding.sineButton.setText(R.string.sineInv)
            binding.cosineButton.setText(R.string.cosineInv)
            binding.tangentButton.setText(R.string.tangentInv)
            binding.naturalLogarithmButton.setText(R.string.naturalLogarithmInv)
            binding.logarithmButton.setText(R.string.logarithmInv)
            binding.squareButton.setText(R.string.squareInv)
        } else {
            isInvButtonClicked = false

            // change buttons
            binding.sineButton.setText(R.string.sine)
            binding.cosineButton.setText(R.string.cosine)
            binding.tangentButton.setText(R.string.tangent)
            binding.naturalLogarithmButton.setText(R.string.naturalLogarithm)
            binding.logarithmButton.setText(R.string.logarithm)
            binding.squareButton.setText(R.string.square)
        }
    }

    @SuppressLint("SetTextI18n")
    fun clearButton(view: View) {
        keyVibration(view)
        binding.input.setText("")
        binding.resultDisplay.setText("")
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    fun equalsButton(view: View) {
        calculationJob?.cancel()
        calculationJob = lifecycleScope.launch(Dispatchers.Default) {
            // F-UI-2: đọc binding.input phải trên Main thread (Editable không thread-safe)
            val calculation = withContext(Dispatchers.Main) {
                keyVibration(view)
                binding.input.text.toString()
            }

            if (calculation != "") {
                calculationMutex.withLock {
                    division_by_0 = false
                    domain_error = false
                    domain_error_reason = null
                    syntax_error = false

                    val calculationTmp = Expression().getCleanExpression(
                        calculation,
                        decimalSeparatorSymbol,
                        groupingSeparatorSymbol,
                        lastAnswer
                    )
                    // F-UI-9: parser đệ quy xuống, biểu thức bất thường (nhiều ngoặc lồng nhau)
                    // có thể vượt stack — không để crash cả app.
                    val result = try {
                        roundResult(Calculator().evaluate(calculationTmp, isDegreeModeActivated))
                    } catch (e: StackOverflowError) {
                        syntax_error = true
                        Double.NaN
                    }
                    var resultString = result.toString()
                    var formattedResult = NumberFormatter.format(
                        resultString.replace(".", decimalSeparatorSymbol),
                        decimalSeparatorSymbol,
                        groupingSeparatorSymbol
                    )

                    // If result is a number and it is finite
                    if (!result.isNaN() && result.isFinite()) {
                        lastAnswer = result

                        // If there is an unused 0 at the end, remove it : 2.0 -> 2
                        if ((result * 10) % 10 == 0.0) {
                            resultString = String.format("%.0f", result)
                            formattedResult = NumberFormatter.format(
                                resultString,
                                decimalSeparatorSymbol,
                                groupingSeparatorSymbol
                            )
                        }

                        // Hide the cursor before updating binding.input to avoid weird cursor movement
                        withContext(Dispatchers.Main) {
                            binding.input.isCursorVisible = false
                        }

                        // Bug: setText(formattedResult) bên dưới kích hoạt TextWatcher ->
                        // updateResultDisplay() -> calculationJob?.cancel() tự huỷ CHÍNH coroutine
                        // đang chạy (equalsButton dùng chung field calculationJob). Việc huỷ này có
                        // hiệu lực NGAY khi resume sau withContext(Main) chứa setText — tức là
                        // NonCancellable phải bọc từ trước lệnh setText, bọc từ sau mới đã muộn
                        // (exception ném ra trước khi vào được block NonCancellable).
                        withContext(NonCancellable) {
                            // Display result
                            withContext(Dispatchers.Main) { binding.input.setText(formattedResult) }

                            // Set cursor
                            withContext(Dispatchers.Main) {
                                // Scroll to the end
                                binding.input.setSelection(binding.input.length())

                                // Hide the cursor (do not remove this, it's not a duplicate)
                                binding.input.isCursorVisible = false

                                // Clear resultDisplay
                                binding.resultDisplay.setText("")
                            }

                            if (calculation != formattedResult) {
                                // Review fix: đồng bộ với job trim history ở onResume() (đọc/sửa/ghi
                                // cùng 1 blob SharedPreferences) để tránh mất entry vừa lưu.
                                historyMutex.withLock {
                                    val history = prefs.getHistory()

                                    // Do not save to history if the previous entry is the same as the current one
                                    if (history.isEmpty() || history[history.size - 1].calculation != calculation) {
                                        // Store time
                                        val currentTime = System.currentTimeMillis().toString()

                                        // Save to history
                                        history.add(
                                            History(
                                                calculation = calculation,
                                                result = formattedResult,
                                                time = currentTime,
                                            )
                                        )

                                        prefs.saveHistory(this@MainActivity, history)

                                        // Update history variables
                                        withContext(Dispatchers.Main) {
                                            historyAdapter.appendOneHistoryElement(
                                                History(
                                                    calculation = calculation,
                                                    result = formattedResult,
                                                    time = currentTime,
                                                )
                                            )

                                            // Remove former results if > historySize preference
                                            // N-DATA-4: xoá entry cũ nhất CHƯA GHIM — break nếu
                                            // toàn bộ còn lại đã ghim để tránh lặp vô hạn.
                                            // N-DATA-3: dùng fullHistorySize (không phải itemCount,
                                            // vốn bị thu hẹp khi đang lọc) để không trim nhầm theo
                                            // số lượng entry đang HIỂN THỊ thay vì số thật.
                                            val historySize =
                                                prefs.historySize!!.toInt()
                                            while (historySize > 0 && historyAdapter.fullHistorySize >= historySize) {
                                                if (!historyAdapter.removeOldestUnpinnedHistoryElement()) break
                                            }

                                            // Scroll to the bottom of the recycle view — bỏ qua
                                            // khi đang lọc vì "cuối danh sách hiển thị" không nhất
                                            // thiết là entry vừa thêm.
                                            if (!historyAdapter.isFiltered) {
                                                binding.rvHistory.scrollToPosition(historyAdapter.itemCount - 1)
                                            }
                                        }
                                    }
                                }
                            }
                            isEqualLastAction = true
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            if (syntax_error) {
                                setErrorColor(true)
                                binding.resultDisplay.setText(getString(R.string.syntax_error))
                            } else if (domain_error) {
                                setErrorColor(true)
                                // X-CALC-1: giải thích cụ thể thay vì "Domain error" chung chung
                                binding.resultDisplay.setText(
                                    when (domain_error_reason) {
                                        DomainErrorReason.LOG_NON_POSITIVE ->
                                            getString(R.string.error_explain_log_nonpositive)

                                        DomainErrorReason.TAN_SINGULARITY ->
                                            getString(R.string.error_explain_tan_singularity)

                                        null -> getString(R.string.domain_error)
                                    }
                                )
                            } else if (division_by_0) {
                                // F-CALC-5: 0/0 = NaN (không phải Infinity) nên phải check cờ
                                // division_by_0 TRƯỚC nhánh isInfinite()/isNaN(), không thì rơi
                                // vào "Math error" chung chung thay vì "Division by zero".
                                setErrorColor(true)
                                binding.resultDisplay.setText(getString(R.string.division_by_0))
                            } else if (result.isInfinite()) {
                                if (result < 0) binding.resultDisplay.setText("-" + getString(R.string.infinity))
                                else binding.resultDisplay.setText(getString(R.string.value_too_large))
                            } else if (result.isNaN()) {
                                setErrorColor(true)
                                binding.resultDisplay.setText(getString(R.string.math_error))
                            } else {
                                binding.resultDisplay.setText(formattedResult)
                                isEqualLastAction =
                                    true // Do not clear the calculation (if you click into a number) if there is an error
                            }
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) { binding.resultDisplay.setText("") }
            }
        }
    }

    fun parenthesesButton(view: View) {
        val cursorPosition = binding.input.selectionStart
        val textLength = binding.input.text.length

        var openParentheses = 0
        var closeParentheses = 0

        val text = binding.input.text.toString()

        for (i in 0 until cursorPosition) {
            if (text[i] == '(') {
                openParentheses += 1
            }
            if (text[i] == ')') {
                closeParentheses += 1
            }
        }

        if (
            !(textLength > cursorPosition && binding.input.text.toString()[cursorPosition] in "×÷+-^")
            && (
                    openParentheses == closeParentheses
                            || binding.input.text.toString()[cursorPosition - 1] == '('
                            || binding.input.text.toString()[cursorPosition - 1] in "×÷+-^"
                    )
        ) {
            updateDisplay(view, "(")
        } else {
            updateDisplay(view, ")")
        }
    }

    fun backspaceButton(view: View) {
        keyVibration(view)

        var cursorPosition = binding.input.selectionStart
        val textLength = binding.input.text.length
        var newValue = ""
        var isFunction = false
        var functionLength = 0

        if (isEqualLastAction) {
            cursorPosition = textLength
        }

        if (cursorPosition != 0 && textLength != 0) {
            // Check if it is a function to delete
            val functionsList =
                listOf("cos⁻¹(", "sin⁻¹(", "tan⁻¹(", "cos(", "sin(", "tan(", "ln(", "log(", "exp(")
            for (function in functionsList) {
                val leftPart = binding.input.text.subSequence(0, cursorPosition).toString()
                if (leftPart.endsWith(function)) {
                    newValue = binding.input.text.subSequence(0, cursorPosition - function.length)
                        .toString() +
                            binding.input.text.subSequence(cursorPosition, textLength).toString()
                    isFunction = true
                    functionLength = function.length - 1
                    break
                }
            }
            // Else
            if (!isFunction) {
                // remove the grouping separator
                val leftPart = binding.input.text.subSequence(0, cursorPosition).toString()
                val leftPartWithoutSpaces = leftPart.replace(groupingSeparatorSymbol, "")
                functionLength = leftPart.length - leftPartWithoutSpaces.length

                newValue = leftPartWithoutSpaces.subSequence(0, leftPartWithoutSpaces.length - 1)
                    .toString() +
                        binding.input.text.subSequence(cursorPosition, textLength).toString()
            }

            val newValueFormatted =
                NumberFormatter.format(newValue, decimalSeparatorSymbol, groupingSeparatorSymbol)
            var cursorOffset = newValueFormatted.length - newValue.length
            if (cursorOffset < 0) cursorOffset = 0

            binding.input.setText(newValueFormatted)
            binding.input.setSelection((cursorPosition - 1 + cursorOffset - functionLength).takeIf { it > 0 }
                ?: 0)
        }
    }

    //do not delete params
    fun scientistModeSwitchButton(view: View) {
        enableOrDisableScientistMode()
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()

        // Làm mới snapshot preferences để bắt thay đổi từ SettingsActivity
        prefs = MyPreferences(this)

        if (appLanguage != Locale.getDefault()) {
            appLanguage = Locale.getDefault()
            // Clear inputs to avoid conflicts with decimal & grouping separators
            binding.input.setText("")
            binding.resultDisplay.setText("")
        }

        // Update settings
        // Prevent phone from sleeping while the app is in foreground
        view.keepScreenOn = prefs.preventPhoneFromSleepingMode

        // Remove former results if > historySize preference
        // Remove from the RecycleView
        // N-DATA-4: xoá entry cũ nhất CHƯA GHIM — break nếu toàn bộ còn lại đã ghim.
        // N-DATA-3: fullHistorySize thay vì itemCount — tránh trim theo số lượng bị thu hẹp lúc
        // đang lọc.
        val historySize = prefs.historySize!!.toInt()
        while (historySize > 0 && historyAdapter.fullHistorySize >= historySize) {
            if (!historyAdapter.removeOldestUnpinnedHistoryElement()) break
        }
        // F-DATA-3: đọc/ghi lại toàn bộ blob history mỗi lần resume — chạy nền, không block Main.
        // Review fix: historyMutex đồng bộ với equalsButton() để tránh 2 coroutine đọc-sửa-ghi
        // chồng chéo lên cùng 1 blob SharedPreferences làm mất entry vừa lưu.
        lifecycleScope.launch(Dispatchers.IO) {
            historyMutex.withLock {
                val history = prefs.getHistory()
                while (historySize > 0 && history.size > historySize) {
                    val index = history.indexOfFirst { !it.isPinned }
                    if (index == -1) break
                    history.removeAt(index)
                }
                prefs.saveHistory(this@MainActivity, history)
            }
        }

        // Disable the keyboard on display EditText
        binding.input.showSoftInputOnFocus = false

        // Refresh VIP badge khi back từ VIP/Settings + chạy animation
        bindVipBadge()
        startChipPulse()
    }

    override fun onPause() {
        stopChipPulse()
        super.onPause()
    }

    private var doubleBackToExitPressedOnce: Boolean = false
    private val backPressHandler = Handler(Looper.getMainLooper())
    private val resetBackPressRunnable = Runnable { doubleBackToExitPressedOnce = false }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }
        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, getString(R.string.toast_double_back_to_exit), Toast.LENGTH_LONG).show()
        backPressHandler.postDelayed(resetBackPressRunnable, 2000)
    }

    override fun onDestroy() {
        // Fix memory leak: remove pending Handler callbacks + cancel animator
        backPressHandler.removeCallbacks(resetBackPressRunnable)
        stopChipPulse()
        super.onDestroy()
    }

    private fun showDialogTesterCommunity() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_tester_community_title))
            .setMessage(getString(R.string.dialog_tester_community_message))
            .setPositiveButton(getString(R.string.button_ok)) { dialog, _ ->
                dialog.dismiss()
                rateApp("com.mckimquyen.bemytester")
            }
            .setNegativeButton(getString(R.string.button_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}