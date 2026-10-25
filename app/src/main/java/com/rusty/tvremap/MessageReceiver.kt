package com.rusty.tvremap

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Shows a pop-up on the TV:
 *
 *   adb shell am broadcast -n com.rusty.tvremap/.MessageReceiver \
 *     --es title "Ready to watch" --es text "Some Movie (2003)"
 *
 * The manifest requires senders to hold android.permission.DUMP, which adb's
 * shell has and ordinary apps can't get, so only adb (the laptop or the
 * Latitude) can trigger it.
 */
class MessageReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Message"
        val text = intent.getStringExtra("text") ?: return
        val service = RemapService.instance
        if (service == null) {
            Log.w(TAG, "service not running, dropping: $title / $text")
            setResultCode(1)
            return
        }
        Log.i(TAG, "popup: $title / $text")
        service.showPopup(title, text)
    }

    private companion object {
        const val TAG = "TvButtonRemap"
    }
}
