package com.frametune.extension.vidy

import android.app.Application
import com.frametune.extension.vidy.service.LocalAddonService

class VidyExtensionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            LocalAddonService.start(this)
        } catch (e: Exception) {
            android.util.Log.w("VidyExtensionApp", "Could not start LocalAddonService: ${e.message}")
        }
    }
}

