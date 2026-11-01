package com.example.model

data class MediaState(
    val isPlaying: Boolean = false,
    val title: String = "No Active Media",
    val artist: String = "WristMirror Remote",
    val currentVolume: Int = 0,
    val maxVolume: Int = 15,
    val isMuted: Boolean = false
) {
    val volumePercent: Int
        get() = if (maxVolume > 0) ((currentVolume.toFloat() / maxVolume) * 100).toInt() else 0
}
