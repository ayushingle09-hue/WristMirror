package com.example.model

import android.graphics.Bitmap

enum class StreamResolution(val label: String, val width: Int, val height: Int) {
    COMPACT("160x160 (Low BLE Bandwidth)", 160, 160),
    STANDARD("240x240 (Optimal Watch HUD)", 240, 240),
    REDMI_AMOLED("390x450 (Redmi Move AMOLED)", 390, 450),
    CIRCULAR_AMOLED("454x454 (Round AMOLED)", 454, 454)
}

data class MirrorStreamConfig(
    val resolution: StreamResolution = StreamResolution.STANDARD,
    val targetFps: Int = 10,
    val isGrayscale: Boolean = false,
    val compressionQuality: Int = 65,
    val pauseOnLockScreen: Boolean = true,
    val excludeSecureWindows: Boolean = true
)

data class MirrorStreamState(
    val isProjecting: Boolean = false,
    val isPaused: Boolean = false,
    val fps: Double = 0.0,
    val packetsSent: Long = 0L,
    val bandwidthKbps: Double = 0.0,
    val latestFrame: Bitmap? = null,
    val activeAppName: String = "No App Selected",
    val statusMessage: String = "Ready to start mirror projection"
)
