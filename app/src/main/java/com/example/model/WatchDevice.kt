package com.example.model

data class WatchDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val isBonded: Boolean = false,
    val isConnected: Boolean = false,
    val batteryLevel: Int? = null,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)
