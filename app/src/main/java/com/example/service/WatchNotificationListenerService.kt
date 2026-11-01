package com.example.service

import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WatchNotificationListenerService : NotificationListenerService() {

    companion object {
        private val _activeMediaInfo = MutableStateFlow<MediaInfo?>(null)
        val activeMediaInfo = _activeMediaInfo.asStateFlow()

        var isServiceConnected: Boolean = false
            private set
    }

    data class MediaInfo(
        val title: String,
        val artist: String,
        val isPlaying: Boolean,
        val packageName: String
    )

    override fun onListenerConnected() {
        super.onListenerConnected()
        isServiceConnected = true
        checkActiveMediaSessions()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isServiceConnected = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        checkActiveMediaSessions()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        checkActiveMediaSessions()
    }

    private fun checkActiveMediaSessions() {
        try {
            val mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return
            val component = ComponentName(this, WatchNotificationListenerService::class.java)
            val controllers = mediaSessionManager.getActiveSessions(component)

            for (controller in controllers) {
                val playbackState = controller.playbackState
                val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
                val metadata = controller.metadata

                if (metadata != null) {
                    val title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "Playing Media"
                    val artist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"

                    _activeMediaInfo.value = MediaInfo(
                        title = title,
                        artist = artist,
                        isPlaying = isPlaying,
                        packageName = controller.packageName
                    )
                    return
                }
            }
        } catch (_: SecurityException) {
            // Permission not yet granted in Notification Access settings
        } catch (_: Exception) {
        }
    }
}
