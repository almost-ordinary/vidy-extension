package com.frametune.extension.vidy.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.frametune.extension.vidy.server.LocalAddonServer

/**
 * Foreground Service that keeps the local loopback addon server running.
 */
class LocalAddonService : Service() {

    companion object {
        private const val CHANNEL_ID = "vidy_addon_channel"
        private const val NOTIFICATION_ID = 8910

        private var serverInstance: LocalAddonServer? = null

        fun isServerRunning(): Boolean = serverInstance?.isRunning == true

        fun start(context: Context) {
            val intent = Intent(context, LocalAddonService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocalAddonService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            android.util.Log.w("LocalAddonService", "Could not startForeground: ${e.message}")
        }

        if (serverInstance == null) {
            serverInstance = LocalAddonServer(8910).also { it.start() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (serverInstance == null || serverInstance?.isRunning == false) {
            serverInstance = LocalAddonServer(8910).also { it.start() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serverInstance?.stop()
        serverInstance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Vidy Addon Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs local loopback Stremio stream provider on 127.0.0.1"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FrameTune Vidy Extension")
            .setContentText("Local streaming provider active on 127.0.0.1:8910")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
