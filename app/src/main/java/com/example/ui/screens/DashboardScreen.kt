package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppInfo
import com.example.model.WatchEcosystemType
import com.example.ui.MainViewModel
import com.example.ui.NavigationTab
import com.example.ui.components.AmoledCard
import com.example.ui.components.StatusPill
import com.example.ui.components.WristMirrorTopBar
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardBorderActive
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusIncompatible
import com.example.ui.theme.StatusScanning
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onRequestScreenProjection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionStatus by viewModel.bleManager.connectionStatus.collectAsState()
    val connectedDevice by viewModel.bleManager.connectedDevice.collectAsState()
    val diagnosticReport by viewModel.bleManager.diagnosticReport.collectAsState()
    val isScanning by viewModel.bleManager.isScanning.collectAsState()
    val selectedAppsCount by viewModel.selectedAppsCount.collectAsState()
    val installedApps by viewModel.appManager.installedApps.collectAsState()
    val streamState by viewModel.streamState.collectAsState()
    val mediaState by viewModel.mediaManager.mediaState.collectAsState()

    val selectedApps = installedApps.filter { it.isSelected }

    val statusColor = when {
        connectedDevice != null -> StatusConnected
        isScanning -> StatusScanning
        connectionStatus.contains("Connecting") -> StatusWarning
        else -> StatusIncompatible
    }

    val statusLabel = when {
        connectedDevice != null -> connectedDevice?.name ?: "Connected"
        isScanning -> "Scanning BLE..."
        connectionStatus.contains("Connecting") -> "Connecting..."
        else -> "Disconnected"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        WristMirrorTopBar(
            title = "WristMirror",
            subtitle = "Smart Watch Remote & Mirror",
            connectionLabel = statusLabel,
            connectionColor = statusColor,
            onStatusClick = { viewModel.selectTab(NavigationTab.BLE_DIAGNOSTICS) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // 1. Smartwatch Hardware & Connection Card
            item {
                WatchStatusCard(
                    connectedDevice = connectedDevice,
                    connectionStatus = connectionStatus,
                    isScanning = isScanning,
                    report = diagnosticReport,
                    onConnectClick = {
                        if (connectedDevice != null) {
                            viewModel.disconnectBle()
                        } else {
                            viewModel.selectTab(NavigationTab.BLE_DIAGNOSTICS)
                        }
                    },
                    onRunRedmiDiagnostic = {
                        viewModel.loadRedmiMoveDiagnostics()
                    }
                )
            }

            // 2. Redmi Watch Move AMOLED Compatibility Advisory
            item {
                RedmiCompatibilityBanner(
                    report = diagnosticReport,
                    onViewDiagnostics = { viewModel.selectTab(NavigationTab.BLE_DIAGNOSTICS) }
                )
            }

            // 3. Screen Mirroring Quick Status Card
            item {
                ScreenMirrorQuickCard(
                    streamState = streamState,
                    selectedAppsCount = selectedAppsCount,
                    onStartMirror = {
                        if (streamState.isProjecting) {
                            viewModel.stopMirroring()
                        } else {
                            onRequestScreenProjection()
                        }
                    },
                    onOpenMirrorTab = { viewModel.selectTab(NavigationTab.SCREEN_MIRROR) }
                )
            }

            // 4. Selected Apps Carousel
            item {
                SelectedAppsSection(
                    selectedApps = selectedApps,
                    selectedCount = selectedAppsCount,
                    onManageApps = { viewModel.selectTab(NavigationTab.APP_MANAGER) },
                    onAppClick = { app ->
                        viewModel.launchApp(app.packageName)
                    }
                )
            }

            // 5. Media Remote Control Widget
            item {
                MediaRemoteWidget(
                    mediaState = mediaState,
                    onPlayPause = { viewModel.mediaManager.playPause() },
                    onNext = { viewModel.mediaManager.nextTrack() },
                    onPrev = { viewModel.mediaManager.previousTrack() },
                    onVolumeChange = { vol -> viewModel.mediaManager.setVolume(vol.toInt()) },
                    onToggleMute = { viewModel.mediaManager.toggleMute() }
                )
            }

            // 6. Quick Action Hub
            item {
                QuickActionsHub(
                    onScanClick = {
                        viewModel.selectTab(NavigationTab.BLE_DIAGNOSTICS)
                        viewModel.startBleScan()
                    },
                    onWatchHudClick = { viewModel.selectTab(NavigationTab.WATCH_HUD) },
                    onSendTestAlert = {
                        viewModel.sendWatchTestAlert(
                            "Watch Mirror Synced",
                            "WristMirror remote control is active on your smartphone!"
                        )
                    },
                    onAppManagerClick = { viewModel.selectTab(NavigationTab.APP_MANAGER) }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun WatchStatusCard(
    connectedDevice: com.example.model.WatchDevice?,
    connectionStatus: String,
    isScanning: Boolean,
    report: com.example.model.GattDiagnosticReport?,
    onConnectClick: () -> Unit,
    onRunRedmiDiagnostic: () -> Unit
) {
    AmoledCard(
        borderColor = if (connectedDevice != null) ElectricBlue else DarkCardBorder,
        borderWidth = if (connectedDevice != null) 1.5f else 1f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricBlue.copy(alpha = 0.15f))
                            .border(1.dp, ElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = "Watch Icon",
                            tint = ElectricBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = connectedDevice?.name ?: "No Watch Connected",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = connectedDevice?.address ?: connectionStatus,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                if (connectedDevice?.batteryLevel != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20))
                            .background(StatusConnected.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryFull,
                            contentDescription = "Battery",
                            tint = StatusConnected,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${connectedDevice.batteryLevel}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusConnected
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onConnectClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (connectedDevice != null) StatusIncompatible.copy(alpha = 0.2f) else ElectricBlue,
                        contentColor = if (connectedDevice != null) StatusIncompatible else AmoledBlack
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_connect_btn")
                ) {
                    Text(
                        text = if (connectedDevice != null) "Disconnect" else "Pair / Scan BLE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = onRunRedmiDiagnostic,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurface,
                        contentColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_redmi_diag_btn")
                ) {
                    Text(
                        text = "Redmi Move Diag",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RedmiCompatibilityBanner(
    report: com.example.model.GattDiagnosticReport?,
    onViewDiagnostics: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F1A28),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewDiagnostics)
            .testTag("redmi_compatibility_banner")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Compatibility Info",
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Redmi Watch Move AMOLED Architecture",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Runs closed RTOS with Mi Fitness encryption. Screen mirror operates via phone-side HUD companion and notification bridge.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun ScreenMirrorQuickCard(
    streamState: com.example.model.MirrorStreamState,
    selectedAppsCount: Int,
    onStartMirror: () -> Unit,
    onOpenMirrorTab: () -> Unit
) {
    AmoledCard(
        borderColor = if (streamState.isProjecting) NeonCyan else DarkCardBorder,
        borderWidth = if (streamState.isProjecting) 1.5f else 1f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonCyan.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenShare,
                            contentDescription = "Screen Mirroring",
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Selected App Screen Stream",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (streamState.isProjecting) "Transmitting • ${String.format("%.1f", streamState.fps)} FPS • ${String.format("%.1f", streamState.bandwidthKbps)} KB/s" else streamState.statusMessage,
                            fontSize = 11.sp,
                            color = if (streamState.isProjecting) NeonCyan else TextSecondary
                        )
                    }
                }

                StatusPill(
                    label = if (streamState.isProjecting) "LIVE" else "STOPPED",
                    statusColor = if (streamState.isProjecting) NeonCyan else TextTertiary,
                    isPulsing = streamState.isProjecting
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onStartMirror,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (streamState.isProjecting) StatusIncompatible else NeonCyan,
                        contentColor = AmoledBlack
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_start_mirror_btn")
                ) {
                    Icon(
                        imageVector = if (streamState.isProjecting) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (streamState.isProjecting) "Stop Mirror" else "Start Mirror",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = onOpenMirrorTab,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurface,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.testTag("dashboard_view_mirror_hud_btn")
                ) {
                    Text(text = "View Bezel HUD", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SelectedAppsSection(
    selectedApps: List<AppInfo>,
    selectedCount: Int,
    onManageApps: () -> Unit,
    onAppClick: (AppInfo) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Selected Apps to Mirror",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = ElectricBlue.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "$selectedCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "Manage Apps →",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = ElectricBlue,
                modifier = Modifier
                    .clickable(onClick = onManageApps)
                    .testTag("dashboard_manage_apps_link")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedApps.isEmpty()) {
            AmoledCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No apps selected for smartwatch remote",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onManageApps,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue.copy(alpha = 0.15f),
                            contentColor = ElectricBlue
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Select Apps from Phone", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(selectedApps) { app ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkCardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onAppClick(app) }
                            .testTag("app_chip_${app.packageName}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            if (app.iconBitmap != null) {
                                Image(
                                    bitmap = app.iconBitmap,
                                    contentDescription = app.appName,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = app.appName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaRemoteWidget(
    mediaState: com.example.model.MediaState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit
) {
    AmoledCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Phone Media Remote",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ElectricBlue
                    )
                    Text(
                        text = mediaState.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = mediaState.artist,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPrev,
                        modifier = Modifier.testTag("dashboard_media_prev_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = TextPrimary
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(ElectricBlue, NeonCyan)))
                            .clickable(onClick = onPlayPause)
                            .testTag("dashboard_media_play_btn")
                    ) {
                        Icon(
                            imageVector = if (mediaState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = AmoledBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.testTag("dashboard_media_next_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Volume control slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (mediaState.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeDown,
                        contentDescription = "Mute / Unmute",
                        tint = if (mediaState.isMuted) StatusIncompatible else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Slider(
                    value = mediaState.currentVolume.toFloat(),
                    onValueChange = onVolumeChange,
                    valueRange = 0f..mediaState.maxVolume.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricBlue,
                        activeTrackColor = ElectricBlue,
                        inactiveTrackColor = DarkSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_volume_slider")
                )

                Text(
                    text = "${mediaState.volumePercent}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.width(36.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickActionsHub(
    onScanClick: () -> Unit,
    onWatchHudClick: () -> Unit,
    onSendTestAlert: () -> Unit,
    onAppManagerClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Watch Remote Quick Controls",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionTile(
                icon = Icons.Default.Watch,
                label = "Watch HUD",
                color = ElectricBlue,
                modifier = Modifier.weight(1f),
                onClick = onWatchHudClick,
                testTag = "action_watch_hud"
            )

            ActionTile(
                icon = Icons.Default.NotificationsActive,
                label = "Watch Alert",
                color = NeonCyan,
                modifier = Modifier.weight(1f),
                onClick = onSendTestAlert,
                testTag = "action_send_alert"
            )

            ActionTile(
                icon = Icons.Default.BluetoothSearching,
                label = "Scan BLE",
                color = StatusScanning,
                modifier = Modifier.weight(1f),
                onClick = onScanClick,
                testTag = "action_scan_ble"
            )
        }
    }
}

@Composable
private fun ActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 6.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}
