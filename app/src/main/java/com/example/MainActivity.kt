package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.ScreenMirrorService
import com.example.ui.MainViewModel
import com.example.ui.NavigationTab
import com.example.ui.components.WristMirrorBottomNav
import com.example.ui.screens.AppManagerScreen
import com.example.ui.screens.BleDiagnosticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ScreenMirrorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WatchInterfaceScreen
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.WristMirrorTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WristMirrorTheme {
                WristMirrorApp()
            }
        }
    }
}

@Composable
fun WristMirrorApp(
    viewModel: MainViewModel = viewModel()
) {
    val currentTab by viewModel.currentTab.collectAsState()

    // BackHandler: returns to Dashboard if on a subscreen
    BackHandler(enabled = currentTab != NavigationTab.DASHBOARD) {
        viewModel.selectTab(NavigationTab.DASHBOARD)
    }

    // Bluetooth and Notification Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Handle results if needed
    }

    // Trigger initial permission requests smoothly on first launch
    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)

        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    // MediaProjection Permission Launcher
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val app = viewModel.getApplication<WristMirrorApplication>()
            val serviceIntent = Intent(app, ScreenMirrorService::class.java).apply {
                action = ScreenMirrorService.ACTION_START
                putExtra(ScreenMirrorService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenMirrorService.EXTRA_DATA_INTENT, result.data)
            }
            ContextCompat.startForegroundService(app, serviceIntent)
            Toast.makeText(app, "WristMirror Screen Stream Started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                viewModel.getApplication(),
                "Screen capture permission was canceled",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val requestScreenProjection: () -> Unit = {
        val projectionManager = viewModel.getApplication<WristMirrorApplication>()
            .getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (projectionManager != null) {
            mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
            WristMirrorBottomNav(
                currentTab = currentTab,
                onTabSelected = { tab -> viewModel.selectTab(tab) }
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AmoledBlack)
        ) {
            when (currentTab) {
                NavigationTab.DASHBOARD -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onRequestScreenProjection = requestScreenProjection
                    )
                }
                NavigationTab.APP_MANAGER -> {
                    AppManagerScreen(viewModel = viewModel)
                }
                NavigationTab.SCREEN_MIRROR -> {
                    ScreenMirrorScreen(
                        viewModel = viewModel,
                        onRequestScreenProjection = requestScreenProjection
                    )
                }
                NavigationTab.WATCH_HUD -> {
                    WatchInterfaceScreen(
                        viewModel = viewModel,
                        onRequestScreenProjection = requestScreenProjection
                    )
                }
                NavigationTab.BLE_DIAGNOSTICS -> {
                    BleDiagnosticsScreen(viewModel = viewModel)
                }
                NavigationTab.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onRequestPermissions = {
                            val perms = mutableListOf<String>()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                perms.add(Manifest.permission.BLUETOOTH_SCAN)
                                perms.add(Manifest.permission.BLUETOOTH_CONNECT)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
                            permissionLauncher.launch(perms.toTypedArray())
                        },
                        onRequestScreenProjection = requestScreenProjection
                    )
                }
            }
        }
    }
}
