package com.rusty.tvremap

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Catches remote buttons before Sony/Google TV's global-key handling and
 * opens a different app instead.
 *
 * On the BRAVIA, the YouTube button sends KEYCODE_BUTTON_9 (196), which the
 * system routes to sonyvendorservice -> Google TV -> YouTube. Accessibility
 * key filtering runs before that routing, so consuming the key here stops
 * YouTube from opening at all.
 */
class RemapService : AccessibilityService() {

    // keycode -> package to open instead
    private val remaps = mapOf(
        KeyEvent.KEYCODE_BUTTON_9 to "org.smarttube.stable",
    )

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val target = remaps[event.keyCode] ?: return false
        val launch = packageManager.getLeanbackLaunchIntentForPackage(target)
            ?: packageManager.getLaunchIntentForPackage(target)
        // Target uninstalled: let the button do its normal thing.
        if (launch == null) return false

        // Swallow both down and up so the original app never sees either;
        // launch once per press, ignoring key repeat from holding it.
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            Log.i(TAG, "keycode ${event.keyCode} -> $target")
            startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    private companion object {
        const val TAG = "TvButtonRemap"
    }
}
