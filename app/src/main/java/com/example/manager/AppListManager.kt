package com.example.manager

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.asImageBitmap
import com.example.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AppListManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("wrist_mirror_apps_pref", Context.MODE_PRIVATE)
    private val PREF_KEY_SELECTED_PACKAGES = "selected_packages_set"

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    suspend fun loadInstalledApps() {
        _isLoading.value = true
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val savedSelectedSet = prefs.getStringSet(PREF_KEY_SELECTED_PACKAGES, emptySet()) ?: emptySet()

            val appList = resolveInfos.mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                val packageName = activityInfo.packageName
                // Skip our own app from being listed
                if (packageName == context.packageName) return@mapNotNull null

                val appName = resolveInfo.loadLabel(pm)?.toString() ?: packageName
                val isSystem = (activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val rawDrawable = resolveInfo.loadIcon(pm)

                val bitmap = drawableToBitmap(rawDrawable)
                val imageBitmap = bitmap?.asImageBitmap()

                val versionName = try {
                    pm.getPackageInfo(packageName, 0).versionName ?: "1.0"
                } catch (_: Exception) {
                    "1.0"
                }

                AppInfo(
                    packageName = packageName,
                    appName = appName,
                    versionName = versionName,
                    isSystemApp = isSystem,
                    isSelected = savedSelectedSet.contains(packageName),
                    iconBitmap = imageBitmap,
                    rawIcon = rawDrawable
                )
            }.sortedWith(compareBy({ !it.isSelected }, { it.appName.lowercase() }))

            _installedApps.value = appList
            _isLoading.value = false
        }
    }

    fun toggleAppSelection(packageName: String) {
        val currentList = _installedApps.value.toMutableList()
        val index = currentList.indexOfFirst { it.packageName == packageName }
        if (index != -1) {
            val updated = currentList[index].copy(isSelected = !currentList[index].isSelected)
            currentList[index] = updated
            _installedApps.value = currentList
            persistSelectedApps()
        }
    }

    fun selectAllUserApps() {
        val currentList = _installedApps.value.map {
            if (!it.isSystemApp) it.copy(isSelected = true) else it
        }
        _installedApps.value = currentList
        persistSelectedApps()
    }

    fun clearAllSelections() {
        val currentList = _installedApps.value.map { it.copy(isSelected = false) }
        _installedApps.value = currentList
        persistSelectedApps()
    }

    private fun persistSelectedApps() {
        val selectedPackages = _installedApps.value
            .filter { it.isSelected }
            .map { it.packageName }
            .toSet()

        prefs.edit().putStringSet(PREF_KEY_SELECTED_PACKAGES, selectedPackages).apply()
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
        if (drawable == null) return null
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
