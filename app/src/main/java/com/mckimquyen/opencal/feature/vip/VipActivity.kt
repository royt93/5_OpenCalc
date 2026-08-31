package com.mckimquyen.opencal.feature.vip

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mckimquyen.opencal.BaseActivity
import com.mckimquyen.opencal.R
import com.mckimquyen.opencal.common.const.AdKeys
import com.mckimquyen.opencal.databinding.AVipBinding
import com.mckimquyen.opencal.db.MyPreferences
import com.mckimquyen.opencal.ext.openUrlInBrowser
import com.mckimquyen.opencal.model.Themes
import com.mckimquyen.opencal.util.Logger
import com.roy.sdkadbmob.AdManager
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Màn hình quản lý VIP. Activity-based (project không dùng Navigation Component/Fragment).
 *
 * VIP do `_AdmobWrapper` SDK quản lý (auto-trial 1 ngày khi cài mới, VIP-by-key, expiry).
 * App-side chỉ render + redeem key + rewarded grant. Xem `doc/AD.MD`.
 */
class VipActivity : BaseActivity() {

    private lateinit var binding: AVipBinding
    private lateinit var vipPrefs: VipPrefs

    private val dateFormat by lazy { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    // Animator / timer — nullable + cancel ở onDestroy (memory-leak rule).
    private var countDownTimer: CountDownTimer? = null
    private var pulseAnimator: ObjectAnimator? = null
    private var shimmerAnimator: ObjectAnimator? = null
    private var countUpAnimator: ValueAnimator? = null
    private var lastShownMinute: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Themes — đồng bộ với theme app chọn
        val themes = Themes(this)
        themes.applyDayNightOverride()
        setTheme(themes.getTheme())

        if (MyPreferences(this).theme == 1) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.amoled_background_color)
        } else {
            window.statusBarColor = ContextCompat.getColor(this, R.color.background_color)
        }

        // E-INFRA-6: setupEdgeToEdge1/2 giờ tự động chạy trong BaseActivity.setContentView().
        binding = AVipBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vipPrefs = VipPrefs(this)

        setupListeners()

        // Preload rewarded để nút "Xem QC" sẵn sàng (chỉ khi free — VIP không cần).
        if (!AdManager.isVipByKeyActive()) AdManager.loadRewarded(this)

        // Animation #2 — slide-in từ dưới khi mở màn
        binding.contentContainer.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom)
        )
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener { finish() }

        // #2: nút "Kích hoạt" chỉ bật khi đã nhập mã (tránh bấm khi rỗng).
        binding.btnActivate.isEnabled = false
        binding.etKey.doAfterTextChanged { text ->
            binding.btnActivate.isEnabled = !text.isNullOrBlank()
        }
        binding.btnActivate.setOnClickListener { onRedeemKeyClicked() }

        // [7] Watch ad → 3 days VIP (rewarded; chỉ grant khi user earn reward thật)
        binding.btnWatchAd.setOnClickListener { onWatchAdClicked() }

        // Revoke (cả 2 nút đều thu hồi VIP hiện tại)
        binding.btnRevoke.setOnClickListener { confirmRevoke() }
        binding.btnRevokeAll.setOnClickListener { confirmRevoke() }

        binding.tvPrivacy.setOnClickListener {
            openUrlInBrowser(AdKeys.PRIVACY_POLICY_URL)
        }
    }

    override fun onResume() {
        super.onResume()
        bindUi()
        startDecorativeAnimations()
    }

    override fun onPause() {
        // Animation #1 + #3 chỉ chạy khi visible
        pulseAnimator?.cancel(); pulseAnimator = null
        shimmerAnimator?.cancel(); shimmerAnimator = null
        super.onPause()
    }

    override fun onDestroy() {
        countDownTimer?.cancel(); countDownTimer = null
        pulseAnimator?.cancel(); pulseAnimator = null
        shimmerAnimator?.cancel(); shimmerAnimator = null
        countUpAnimator?.cancel(); countUpAnimator?.removeAllUpdateListeners(); countUpAnimator = null
        super.onDestroy()
    }

    // region UI binding ----------------------------------------------------------------

    private fun bindUi() {
        val active = AdManager.isVipByKeyActive()
        countDownTimer?.cancel(); countDownTimer = null

        if (active) {
            bindActiveUi()
        } else {
            bindFreeUi()
        }

        // Trạng thái nút thu hồi
        binding.btnRevoke.isEnabled = active
        binding.btnRevokeAll.isEnabled = active
    }

    private fun bindFreeUi() {
        binding.layoutStatusHeader.setBackgroundResource(R.drawable.bg_vip_status_header_free)
        binding.tvStatusTitle.setText(R.string.vip_free_user)
        binding.tvStatusSubtitle.setText(R.string.vip_subtitle_free)
        binding.cardProgress.visibility = View.GONE
        binding.cardActiveVip.visibility = View.GONE
        // Free user mới có thể xem QC nhận VIP (rewarded chỉ chạy khi chưa VIP).
        binding.btnWatchAd.visibility = View.VISIBLE
    }

    private fun bindActiveUi() {
        val expiryMs = AdManager.getVipByKeyExpiry()
        var grantedAtMs = AdManager.getVipGrantedAtMs()

        // Fallback chỉ dành cho state rất cũ được tạo trước khi SDK persist grantedAt.
        // Grace = active nhưng user chưa từng tự redeem key.
        val isGrace = !vipPrefs.userRedeemedAtLeastOnce()
        if (grantedAtMs <= 0L) {
            // Grace mặc định 24h; nếu không phải grace, ước lượng grantedAt = now (progress ~0).
            grantedAtMs = if (isGrace) {
                (expiryMs - TimeUnit.DAYS.toMillis(1)).coerceAtMost(System.currentTimeMillis())
            } else {
                System.currentTimeMillis()
            }
        }

        binding.layoutStatusHeader.setBackgroundResource(R.drawable.bg_vip_status_header_active)
        binding.tvStatusTitle.setText(R.string.vip_active)
        binding.tvStatusSubtitle.text = getString(R.string.vip_until, dateFormat.format(Date(expiryMs)))
        // Đang VIP: ẩn nút "Xem QC" (rewarded bị lib chặn khi VIP → nút sẽ vô tác dụng).
        binding.btnWatchAd.visibility = View.GONE

        // [2][3] dates
        binding.cardProgress.visibility = View.VISIBLE
        binding.tvActivationDate.text =
            getString(R.string.vip_activated_at, dateFormat.format(Date(grantedAtMs)))
        binding.tvExpiryDate.text =
            getString(R.string.vip_expires_at, dateFormat.format(Date(expiryMs)))

        // [12] Active VIP card
        binding.cardActiveVip.visibility = View.VISIBLE
        binding.tvActiveKeyLabel.text = if (isGrace) {
            getString(R.string.vip_entry_first_install)
        } else {
            val days = Math.round((expiryMs - grantedAtMs).toDouble() / TimeUnit.DAYS.toMillis(1))
                .toInt().coerceAtLeast(1)
            getString(R.string.vip_entry_redeemed, days)
        }
        binding.tvActiveExpires.text =
            getString(R.string.vip_expires_at, dateFormat.format(Date(expiryMs)))

        // [4][5] progress + countdown — update mỗi giây qua CountDownTimer
        startCountdown(grantedAtMs, expiryMs)
    }

    private fun startCountdown(grantedAtMs: Long, expiryMs: Long) {
        val remainingMs = (expiryMs - System.currentTimeMillis()).coerceAtLeast(0L)
        lastShownMinute = null
        renderTick(grantedAtMs, expiryMs, remainingMs)

        countDownTimer = object : CountDownTimer(remainingMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                renderTick(grantedAtMs, expiryMs, millisUntilFinished)
            }

            override fun onFinish() {
                // Hết hạn → refresh (chuyển về free). isVipByKeyActive() recompute từ truth source.
                bindUi()
            }
        }.start()
    }

    private fun renderTick(grantedAtMs: Long, expiryMs: Long, remainingMs: Long) {
        // [4] progress (elapsed semantic): rỗng lúc kích hoạt → đầy dần đến hết hạn
        binding.progressVip.setProgressCompat(
            VipMath.elapsedProgress(grantedAtMs, expiryMs, System.currentTimeMillis()),
            true,
        )

        // [5] countdown text "Xd HHh MMm SSs"
        val r = VipMath.remaining(remainingMs)
        binding.tvCountdown.text =
            getString(R.string.vip_remaining, r.days, r.hours, r.minutes, r.seconds)
        binding.tvActiveRemaining.text =
            getString(R.string.vip_active_remaining, r.days, r.hours)

        // Animation #4 — count-up (scale bounce) khi phút đổi (không chạy mỗi giây)
        if (lastShownMinute != null && r.minutes != lastShownMinute) {
            playCountUpBounce()
        }
        lastShownMinute = r.minutes
    }

    /** Animation #4: bounce nhẹ tvCountdown khi phút thay đổi. */
    private fun playCountUpBounce() {
        countUpAnimator?.cancel()
        countUpAnimator = ValueAnimator.ofFloat(1f, 1.08f, 1f).apply {
            duration = 400L
            addUpdateListener {
                val s = it.animatedValue as Float
                binding.tvCountdown.scaleX = s
                binding.tvCountdown.scaleY = s
            }
            start()
        }
    }

    private fun startDecorativeAnimations() {
        // Animation #1 — pulse nút "Watch ad" (chỉ khi nút đang hiển thị, tức free user)
        pulseAnimator?.cancel()
        pulseAnimator = null
        if (binding.btnWatchAd.visibility == View.VISIBLE) {
            pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                binding.btnWatchAd,
                android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.04f),
                android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.04f),
            ).apply {
                duration = 1600L
                repeatMode = ObjectAnimator.REVERSE
                repeatCount = ObjectAnimator.INFINITE
                start()
            }
        }

        // Animation #3 — crown shimmer (xoay nhẹ)
        shimmerAnimator?.cancel()
        shimmerAnimator = ObjectAnimator.ofFloat(binding.ivCrown, View.ROTATION, -5f, 5f).apply {
            duration = 3000L
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    // endregion

    // region Actions -------------------------------------------------------------------

    private fun onRedeemKeyClicked() {
        val input = binding.etKey.text?.toString()?.trim().orEmpty()
        if (input.isEmpty()) return

        val days = VipKeys.lookupDays(input)
        if (days == null || !AdManager.activateVipByKey(this, input, days)) {
            showResultDialog(success = false, days = 0)
            return
        }
        binding.etKey.setText("")
        onVipActivated(days)
    }

    /**
     * Watch ad → 3 ngày VIP. CHỈ grant khi user thực sự earn reward.
     * KHÔNG grant ở nhánh thất bại (rewarded bị safety-block / đang VIP / user bỏ xem) — tránh
     * abuse và tránh ghi đè VIP dài hạn. `showRewarded` trả `earned=false` ở các trường hợp đó.
     */
    private fun onWatchAdClicked() {
        AdManager.showRewarded(this) { earned ->
            if (isFinishing || isDestroyed) return@showRewarded
            if (earned) {
                grantViaRewarded()
            } else {
                Toast.makeText(this, R.string.vip_reward_not_earned, Toast.LENGTH_SHORT).show()
            }
            // Preload lại cho lần sau (và để retry nếu lần này chưa kịp tải).
            if (!isFinishing && !isDestroyed && !AdManager.isVipByKeyActive()) {
                AdManager.loadRewarded(this)
            }
        }
    }

    private fun grantViaRewarded() {
        if (grantVipDays(REWARD_DAYS)) onVipActivated(REWARD_DAYS)
        // Thất bại ở đây chỉ xảy ra nếu adConfig chưa init (gần như bất khả đạt sau App.onCreate).
        // Dùng thông báo trung tính thay vì "mã không hợp lệ" (không phải lỗi mã key).
        else Toast.makeText(this, R.string.vip_reward_not_earned, Toast.LENGTH_SHORT).show()
    }

    /**
     * Activate hoặc GIA HẠN VIP — không bao giờ rút ngắn expiry hiện có.
     * Lib chỉ hỗ trợ `activateVipByKey(days)` = set `now + days` (ghi đè), nên cộng dồn số ngày
     * còn lại (làm tròn lên) → tổng expiry ≥ expiry cũ. Chặn bug "rớt hạng" khi đang VIP.
     */
    private fun grantVipDays(days: Int): Boolean {
        return AdManager.grantVipDays(this, days)
    }

    /** Gọi sau khi activate VIP thành công (key hoặc rewarded). */
    private fun onVipActivated(days: Int) {
        vipPrefs.markUserRedeemed()
        Logger.d("VIP activated for $days days")

        // Animation #5 — confetti + haptic
        playConfetti()
        haptic()

        bindUi()
        showResultDialog(success = true, days = days)
    }

    private fun confirmRevoke() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.vip_revoke_all_confirm_title)
            .setMessage(R.string.vip_revoke_all_confirm_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { d, _ ->
                d.dismiss()
                AdManager.clearVipByKey()
                bindUi()
            }
            .show()
    }

    private fun showResultDialog(success: Boolean, days: Int) {
        val title = if (success) R.string.vip_success_title else R.string.vip_failed_title
        val msg = if (success) {
            getString(R.string.vip_success_message, days)
        } else {
            getString(R.string.vip_redeem_invalid)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun playConfetti() {
        val party = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xfdd835, 0xffb300, 0xff8f00, 0xffffff),
            emitter = Emitter(duration = 200L, TimeUnit.MILLISECONDS).max(120),
            position = Position.Relative(0.5, 0.3),
        )
        binding.viewKonfetti.start(party)
    }

    private fun haptic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            binding.root.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            binding.root.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    // endregion

    private companion object {
        const val REWARD_DAYS = 3
    }
}
