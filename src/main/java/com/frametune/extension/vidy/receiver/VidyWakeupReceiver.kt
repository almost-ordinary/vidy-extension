package com.frametune.extension.vidy.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.frametune.extension.vidy.service.LocalAddonService

/**
 * Exported broadcast receiver that allows FrameTune TV and system boot events
 * to wake up and start the LocalAddonService on demand.
 */
class VidyWakeupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.i("VidyWakeupReceiver", "Wakeup intent received: ${intent?.action}")
        try {
            LocalAddonService.start(context)
        } catch (e: Exception) {
            Log.w("VidyWakeupReceiver", "Failed to start LocalAddonService: ${e.message}")
        }
    }
}

