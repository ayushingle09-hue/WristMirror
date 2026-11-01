package com.example.manager

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.WristMirrorApplication
import com.example.model.MediaState
import com.example.service.WatchNotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MediaRemoteManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val _mediaState = MutableStateFlow(MediaState())
    val mediaState: StateFlow<MediaState> = _mediaState.asStateFlow()

    init {
        updateVolumeState()
    }

    fun updateVolumeState() {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val isMuted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
        } else {
            current == 0
        }

        val listenerInfo = WatchNotificationListenerService.activeMediaInfo.value

        _mediaState.value = _mediaState.value.copy(
            currentVolume = current,
            maxVolume = max,
            isMuted = isMuted,
            title = listenerInfo?.title ?: _mediaState.value.title,
            artist = listenerInfo?.artist ?: _mediaState.value.artist,
            isPlaying = listenerInfo?.isPlaying ?: _mediaState.value.isPlaying
        )
    }

    fun volumeUp() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_SHOW_UI
        )
        updateVolumeState()
    }

    fun volumeDown() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
        updateVolumeState()
    }

    fun setVolume(volume: Int) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val clamped = volume.coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, AudioManager.FLAG_SHOW_UI)
        updateVolumeState()
    }

    fun toggleMute() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val shouldMute = !_mediaState.value.isMuted
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (shouldMute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE,
                AudioManager.FLAG_SHOW_UI
            )
        } else {
            if (_mediaState.value.currentVolume > 0) {
                setVolume(0)
            } else {
                setVolume(5)
            }
        }
        updateVolumeState()
    }

    fun sendMediaKeyEvent(keyCode: Int) {
        val downTime = SystemClock.uptimeMillis()
        val eventDown = KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, keyCode, 0)
        val eventUp = KeyEvent(downTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0)

        audioManager.dispatchMediaKeyEvent(eventDown)
        audioManager.dispatchMediaKeyEvent(eventUp)

        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
            keyCode == KeyEvent.KEYCODE_MEDIA_PLAY ||
            keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            _mediaState.value = _mediaState.value.copy(isPlaying = !_mediaState.value.isPlaying)
        }
    }

    fun playPause() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    fun nextTrack() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun previousTrack() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    /**
     * Dispatches a real watch notification alert to test notification bridge.
     * Watches running RTOS (like Redmi Watch Move with Mi Fitness) receive and display this
     * on the watch face with vibration!
     */
    fun sendWatchTestAlert(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 101, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, WristMirrorApplication.CHANNEL_WATCH_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("WristMirror: $title")
            .setContentText(message)
            .setSubText("Smart Watch Remote")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .build()

        notificationManager.notify(7001, notification)
    }
}
