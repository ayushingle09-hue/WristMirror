package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.WristMirrorApplication
import com.example.model.MirrorStreamConfig
import com.example.model.MirrorStreamState
import com.example.model.StreamResolution
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class ScreenMirrorService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var streamConfig = MirrorStreamConfig()
    private var isPaused = false

    private val _streamState = MutableStateFlow(MirrorStreamState())
    val streamState: StateFlow<MirrorStreamState> = _streamState.asStateFlow()

    private var frameCounter = 0
    private var lastFpsCalculationTime = System.currentTimeMillis()
    private var totalBytesSent = 0L

    companion object {
        private const val NOTIFICATION_ID = 4096
        const val ACTION_START = "ACTION_START_MIRROR"
        const val ACTION_STOP = "ACTION_STOP_MIRROR"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_DATA_INTENT = "EXTRA_DATA_INTENT"

        var instance: ScreenMirrorService? = null
            private set
    }

    inner class LocalBinder : Binder() {
        fun getService(): ScreenMirrorService = this@ScreenMirrorService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        instance = this
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val dataIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_DATA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_DATA_INTENT)
                }

                startForegroundServiceNotification()
                if (resultCode != 0 && dataIntent != null) {
                    startProjection(resultCode, dataIntent)
                }
            }
            ACTION_STOP -> {
                stopProjection()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundServiceNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, WristMirrorApplication.CHANNEL_SCREEN_MIRROR)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("WristMirror Streaming Active")
            .setContentText("Screen mirroring pipeline is capturing frames for smartwatch stream")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    fun updateConfig(config: MirrorStreamConfig) {
        this.streamConfig = config
        if (_streamState.value.isProjecting) {
            // Reconfigure virtual display resolution
            recreateVirtualDisplay()
        }
    }

    fun togglePause() {
        isPaused = !isPaused
        _streamState.value = _streamState.value.copy(
            isPaused = isPaused,
            statusMessage = if (isPaused) "Stream Paused" else "Streaming Live"
        )
    }

    fun startProjection(resultCode: Int, data: Intent) {
        stopProjection()

        try {
            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    stopProjection()
                }
            }, Handler(Looper.getMainLooper()))

            setupVirtualDisplay()

            _streamState.value = _streamState.value.copy(
                isProjecting = true,
                isPaused = false,
                statusMessage = "Screen projection active"
            )
        } catch (e: Exception) {
            _streamState.value = _streamState.value.copy(
                isProjecting = false,
                statusMessage = "Projection failed: ${e.message}"
            )
        }
    }

    private fun setupVirtualDisplay() {
        val width = streamConfig.resolution.width
        val height = streamConfig.resolution.height
        val density = resources.displayMetrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            if (isPaused) return@setOnImageAvailableListener

            try {
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                processCapturedImage(image, width, height)
                image.close()
            } catch (_: Exception) {
            }
        }, Handler(Looper.getMainLooper()))

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "WristMirrorProjection",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )
    }

    private fun recreateVirtualDisplay() {
        virtualDisplay?.release()
        imageReader?.close()
        if (mediaProjection != null) {
            setupVirtualDisplay()
        }
    }

    private fun processCapturedImage(image: android.media.Image, width: Int, height: Int) {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width

        val bitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)

        // Crop padding if necessary
        val croppedBitmap = if (rowPadding > 0) {
            Bitmap.createBitmap(bitmap, 0, 0, width, height)
        } else {
            bitmap
        }

        // Compress frame for simulated/real BLE transmission
        val outputStream = ByteArrayOutputStream()
        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, streamConfig.compressionQuality, outputStream)
        val compressedBytes = outputStream.toByteArray()

        frameCounter++
        totalBytesSent += compressedBytes.size

        val now = System.currentTimeMillis()
        val durationSec = (now - lastFpsCalculationTime) / 1000.0
        var currentFps = _streamState.value.fps
        var currentKbps = _streamState.value.bandwidthKbps

        if (durationSec >= 1.0) {
            currentFps = frameCounter / durationSec
            currentKbps = (totalBytesSent / 1024.0) / durationSec
            frameCounter = 0
            totalBytesSent = 0L
            lastFpsCalculationTime = now
        }

        _streamState.value = _streamState.value.copy(
            latestFrame = croppedBitmap,
            fps = currentFps,
            packetsSent = _streamState.value.packetsSent + 1,
            bandwidthKbps = currentKbps
        )
    }

    fun stopProjection() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null

        _streamState.value = _streamState.value.copy(
            isProjecting = false,
            isPaused = false,
            fps = 0.0,
            bandwidthKbps = 0.0,
            statusMessage = "Projection stopped"
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        stopProjection()
        instance = null
    }
}
