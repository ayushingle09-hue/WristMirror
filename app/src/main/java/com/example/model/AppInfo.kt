package com.example.model

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap

data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val isSelected: Boolean = false,
    val iconBitmap: ImageBitmap? = null,
    val rawIcon: Drawable? = null
)
