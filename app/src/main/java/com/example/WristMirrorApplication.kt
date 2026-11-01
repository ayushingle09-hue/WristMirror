package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class WristMirrorApplication : Application() {

    companion object {
        const val CHANNEL_SCREEN_MIRROR = "channel_screen_mirror"
        const val CHANNEL_WATCH_ALERTS = "channel_watch_alerts"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val mirrorChannel = NotificationChannel(
                CHANNEL_SCREEN_MIRROR,
                "WristMirror Active Stream",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifies when screen mirroring to smartwatch is running"
            }

            val alertChannel = NotificationChannel(
                CHANNEL_WATCH_ALERTS,
                "WristMirror Watch Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Delivers synced remote control notifications to connected smartwatch"
                enableVibration(true)
            }

            notificationManager?.createNotificationChannel(mirrorChannel)
            notificationManager?.createNotificationChannel(alertChannel)
        }
    }
}
