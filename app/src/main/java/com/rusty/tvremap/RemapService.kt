package com.rusty.tvremap

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Catches remote buttons before Sony/Google TV's global-key handling and
 * opens something else instead. Accessibility key filtering runs before
 * that routing, so consuming a key here stops its normal action entirely.
 * (The YouTube button -> SmartTube remap was dropped in 1.6.)
 */
class RemapService : AccessibilityService() {

    // keycode -> intent action, only while the launcher's home screen is in front.
    // The remote's gear button is KEYCODE_BUTTON_2 (189), which normally goes
    // to Sony's quick-settings receiver, not the standard KEYCODE_SETTINGS.
    // On Hearth it opens All apps (add, remove and hide apps).
    private val launcherRemaps = mapOf(
        KeyEvent.KEYCODE_BUTTON_2 to "com.rusty.hearth.ALL_APPS",
    )

    // Package and class of the last activity that came to the foreground.
    private var foreground: String? = null
    private var foregroundClass: String? = null

    private val handler = Handler(Looper.getMainLooper())

    private var popup: Popup? = null

    override fun onServiceConnected() {
        popup = Popup(this)
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        popup?.dismiss()
        super.onDestroy()
    }

    fun showPopup(title: String, text: String) {
        popup?.show(title, text)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            Log.d(TAG, "key ${event.keyCode} (${KeyEvent.keyCodeToString(event.keyCode)}) fg=$foreground/$foregroundClass")
        }
        launcherRemaps[event.keyCode]?.let { action ->
            // Pressing it again while Hearth's All apps is open closes it.
            if (inLauncherSettings()) return closeLauncherSettings(event)
            if (foreground == LAUNCHER && foregroundClass == LAUNCHER_HOME) return launchAction(event, action)
        }
        return false
    }

    private fun launchAction(event: KeyEvent, action: String): Boolean {
        val launch = Intent(action)
        if (launch.resolveActivity(packageManager) == null) return false
        return fire(event, launch, action)
    }

    // Swallow both down and up so the original handler never sees either;
    // launch once per press, ignoring key repeat from holding it.
    private fun fire(event: KeyEvent, launch: Intent, label: String): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            Log.i(TAG, "keycode ${event.keyCode} -> $label")
            startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        return true
    }

    // Hearth's All apps screen.
    private fun inLauncherSettings(): Boolean {
        val cls = foregroundClass ?: return false
        return foreground == LAUNCHER && LAUNCHER_SETTINGS.any { cls.startsWith(it) }
    }

    // Back out one level at a time until Hearth's home screen is in front again.
    private fun closeLauncherSettings(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            Log.i(TAG, "keycode ${event.keyCode} -> close $foregroundClass")
            backOut(MAX_BACKS)
        }
        return true
    }

    private fun backOut(remaining: Int) {
        if (remaining == 0 || !inLauncherSettings()) return
        performGlobalAction(GLOBAL_ACTION_BACK)
        handler.postDelayed({ backOut(remaining - 1) }, BACK_DELAY_MS)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString() ?: return
        // Only count real activities, so toasts, the volume bar and other
        // system overlays don't make us think the launcher was left.
        val isActivity = try {
            packageManager.getActivityInfo(ComponentName(pkg, cls), 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
        if (isActivity) {
            foreground = pkg
            foregroundClass = cls
        }
    }

    override fun onInterrupt() {}

    companion object {
        // Running service, for MessageReceiver to draw pop-ups through.
        var instance: RemapService? = null
            private set

        private const val TAG = "TvButtonRemap"
        // Hearth (rusty-workshop/hearth) replaced Projectivy as the home screen.
        private const val LAUNCHER = "com.rusty.hearth"
        private const val LAUNCHER_HOME = "$LAUNCHER.HomeActivity"
        private val LAUNCHER_SETTINGS = listOf("$LAUNCHER.AllAppsActivity")
        private const val MAX_BACKS = 6
        private const val BACK_DELAY_MS = 250L
    }
}
