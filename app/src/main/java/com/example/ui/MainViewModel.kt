package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.manager.AppListManager
import com.example.manager.BluetoothWatchManager
import com.example.manager.MediaRemoteManager
import com.example.model.AppInfo
import com.example.model.MirrorStreamConfig
import com.example.model.MirrorStreamState
import com.example.model.StreamResolution
import com.example.service.ScreenMirrorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppFilter {
    ALL,
    SELECTED,
    USER_ONLY,
    SYSTEM_ONLY
}

enum class NavigationTab {
    DASHBOARD,
    APP_MANAGER,
    SCREEN_MIRROR,
    WATCH_HUD,
    BLE_DIAGNOSTICS,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val bleManager = BluetoothWatchManager(application)
    val appManager = AppListManager(application)
    val mediaManager = MediaRemoteManager(application)

    private val _currentTab = MutableStateFlow(NavigationTab.DASHBOARD)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    private val _appSearchQuery = MutableStateFlow("")
    val appSearchQuery: StateFlow<String> = _appSearchQuery.asStateFlow()

    private val _appFilter = MutableStateFlow(AppFilter.ALL)
    val appFilter: StateFlow<AppFilter> = _appFilter.asStateFlow()

    private val _streamConfig = MutableStateFlow(MirrorStreamConfig())
    val streamConfig: StateFlow<MirrorStreamConfig> = _streamConfig.asStateFlow()

    private val _serviceStreamState = MutableStateFlow(MirrorStreamState())
    val streamState: StateFlow<MirrorStreamState> = _serviceStreamState.asStateFlow()

    private var mirrorService: ScreenMirrorService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? ScreenMirrorService.LocalBinder
            mirrorService = binder?.getService()
            isBound = true

            mirrorService?.let { s ->
                viewModelScope.launch {
                    s.streamState.collect { state ->
                        _serviceStreamState.value = state
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mirrorService = null
            isBound = false
        }
    }

    val filteredApps: StateFlow<List<AppInfo>> = combine(
        appManager.installedApps,
        _appSearchQuery,
        _appFilter
    ) { apps, query, filter ->
        apps.filter { app ->
            val matchesQuery = query.isBlank() ||
                    app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.SELECTED -> app.isSelected
                AppFilter.USER_ONLY -> !app.isSystemApp
                AppFilter.SYSTEM_ONLY -> app.isSystemApp
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedAppsCount: StateFlow<Int> = appManager.installedApps.combine(_appSearchQuery) { apps, _ ->
        apps.count { it.isSelected }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            appManager.loadInstalledApps()
        }
        bindScreenMirrorService()
    }

    fun selectTab(tab: NavigationTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _appSearchQuery.value = query
    }

    fun setAppFilter(filter: AppFilter) {
        _appFilter.value = filter
    }

    fun toggleAppSelection(packageName: String) {
        appManager.toggleAppSelection(packageName)
    }

    fun selectAllUserApps() {
        appManager.selectAllUserApps()
    }

    fun clearAllSelections() {
        appManager.clearAllSelections()
    }

    fun launchApp(packageName: String): Boolean {
        return appManager.launchApp(packageName)
    }

    fun updateStreamResolution(resolution: StreamResolution) {
        val updated = _streamConfig.value.copy(resolution = resolution)
        _streamConfig.value = updated
        mirrorService?.updateConfig(updated)
    }

    fun updateStreamFps(fps: Int) {
        val updated = _streamConfig.value.copy(targetFps = fps)
        _streamConfig.value = updated
        mirrorService?.updateConfig(updated)
    }

    fun toggleGrayscale() {
        val updated = _streamConfig.value.copy(isGrayscale = !_streamConfig.value.isGrayscale)
        _streamConfig.value = updated
        mirrorService?.updateConfig(updated)
    }

    fun toggleMirrorPause() {
        mirrorService?.togglePause()
    }

    fun stopMirroring() {
        val intent = Intent(getApplication(), ScreenMirrorService::class.java).apply {
            action = ScreenMirrorService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
        _serviceStreamState.value = _serviceStreamState.value.copy(
            isProjecting = false,
            statusMessage = "Screen mirror stopped"
        )
    }

    fun startBleScan(): Boolean {
        return bleManager.startScan()
    }

    fun stopBleScan() {
        bleManager.stopScan()
    }

    fun connectBleDevice(address: String): Boolean {
        return bleManager.connectDevice(address)
    }

    fun disconnectBle() {
        bleManager.disconnect()
    }

    fun loadRedmiMoveDiagnostics() {
        bleManager.loadRedmiWatchMoveDiagnosticSimulation()
    }

    fun sendWatchTestAlert(title: String, message: String) {
        mediaManager.sendWatchTestAlert(title, message)
    }

    private fun bindScreenMirrorService() {
        val intent = Intent(getApplication(), ScreenMirrorService::class.java)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            try {
                getApplication<Application>().unbindService(serviceConnection)
            } catch (_: Exception) {
            }
            isBound = false
        }
    }
}
