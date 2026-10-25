package com.rusty.tvremap

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

/**
 * A small card in the bottom-right corner, drawn as an accessibility overlay so
 * it shows over any app without needing the "display over other apps"
 * permission. It never takes focus or input, so the remote keeps working
 * normally while it's up.
 */
class Popup(private val service: AccessibilityService) {

    private val wm = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var current: View? = null

    fun show(title: String, text: String) {
        dismiss()
        val card = card(title, text)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = dp(48)
            y = dp(48)
        }
        card.alpha = 0f
        wm.addView(card, params)
        card.animate().alpha(1f).setDuration(FADE_MS)
        current = card
        handler.postDelayed({ fadeOut(card) }, SHOW_MS)
    }

    private fun fadeOut(card: View) {
        card.animate().alpha(0f).setDuration(FADE_MS).withEndAction {
            if (current === card) dismiss()
        }
    }

    fun dismiss() {
        handler.removeCallbacksAndMessages(null)
        current?.let { runCatching { wm.removeView(it) } }
        current = null
    }

    private fun card(title: String, text: String) = LinearLayout(service).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(28), dp(20), dp(32), dp(22))
        background = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(Color.parseColor("#E6101318"))
            setStroke(dp(2), ACCENT)
        }
        addView(TextView(service).apply {
            this.text = title.uppercase()
            setTextColor(ACCENT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            letterSpacing = 0.12f
            typeface = Typeface.DEFAULT_BOLD
        })
        addView(TextView(service).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            maxWidth = dp(560)
            setPadding(0, dp(6), 0, 0)
        })
    }

    private fun dp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), service.resources.displayMetrics,
    ).toInt()

    private companion object {
        // Jellyfin's purple, since these are mostly "ready in Jellyfin" notes.
        val ACCENT = Color.parseColor("#AA5CC3")
        const val SHOW_MS = 12_000L
        const val FADE_MS = 400L
    }
}
