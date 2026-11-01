package com.example.ui.screens

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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppInfo
import com.example.ui.MainViewModel
import com.example.ui.NavigationTab
import com.example.ui.components.AmoledCard
import com.example.ui.components.StatusPill
import com.example.ui.components.WristMirrorTopBar
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusIncompatible
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WatchInterfaceScreen(
    viewModel: MainViewModel,
    onRequestScreenProjection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val installedApps by viewModel.appManager.installedApps.collectAsState()
    val streamState by viewModel.streamState.collectAsState()
    val mediaState by viewModel.mediaManager.mediaState.collectAsState()
    val connectedDevice by viewModel.bleManager.connectedDevice.collectAsState()

    val selectedApps = installedApps.filter { it.isSelected }

    var currentTimeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            currentTimeString = sdf.format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        WristMirrorTopBar(
            title = "Watch Remote HUD",
            subtitle = "AMOLED Smartwatch Interface Simulator",
            connectionLabel = if (connectedDevice != null) "SYNCED" else "STANDALONE HUD",
            connectionColor = if (connectedDevice != null) StatusConnected else ElectricBlue
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // 1. The Watch HUD Bezel
            item {
                AMOLEDWatchFaceContainer(
                    timeString = currentTimeString,
                    batteryLevel = connectedDevice?.batteryLevel ?: 88,
                    isConnected = connectedDevice != null,
                    streamState = streamState,
                    mediaState = mediaState,
                    selectedApps = selectedApps,
                    onAppClick = { app -> viewModel.launchApp(app.packageName) },
                    onPlayPause = { viewModel.mediaManager.playPause() },
                    onNext = { viewModel.mediaManager.nextTrack() },
                    onPrev = { viewModel.mediaManager.previousTrack() },
                    onVolumeUp = { viewModel.mediaManager.volumeUp() },
                    onVolumeDown = { viewModel.mediaManager.volumeDown() }
                )
            }

            // 2. Physical Watch Alert & Sync Test
            item {
                AmoledCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Physical Watch Notification Bridge",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            StatusPill(
                                label = "Mi Fitness Bridge",
                                statusColor = NeonCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Redmi Watch Move and RTOS smartwatches display high-priority system alerts forwarded via Mi Fitness. Send a real test trigger to verify your watch vibrates and displays controls.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.sendWatchTestAlert(
                                    "Watch Remote Active",
                                    "WristMirror connected! Media controls & app shortcuts synced."
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = AmoledBlack
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_trigger_watch_alert")
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Send Test Alert to Watch",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // 3. Quick Navigation to App Manager if no apps selected
            if (selectedApps.isEmpty()) {
                item {
                    AmoledCard(
                        borderColor = ElectricBlue,
                        onClick = { viewModel.selectTab(NavigationTab.APP_MANAGER) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "No apps on watch launcher yet",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Tap to choose which phone apps appear on your smartwatch",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AMOLEDWatchFaceContainer(
    timeString: String,
    batteryLevel: Int,
    isConnected: Boolean,
    streamState: com.example.model.MirrorStreamState,
    mediaState: com.example.model.MediaState,
    selectedApps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit
) {
    // Watch Case Wrapper
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Watch Outer Body with Metal Gradient
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(310.dp)
                .clip(RoundedCornerShape(64.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF223046), Color(0xFF0F172A), Color(0xFF03070D))
                    )
                )
                .border(
                    3.dp,
                    Brush.linearGradient(listOf(ElectricBlue.copy(alpha = 0.8f), NeonCyan.copy(alpha = 0.4f))),
                    RoundedCornerShape(64.dp)
                )
        ) {
            // AMOLED Display Inner Area (Pure Black)
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(52.dp))
                    .background(AmoledBlack)
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Watch Status Bar (Time, Battery, BLE Icon)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (timeString.isNotEmpty()) timeString else "12:00",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = "BLE Status",
                                tint = if (isConnected) NeonCyan else TextTertiary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = "Battery",
                                tint = StatusConnected,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "$batteryLevel%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusConnected
                            )
                        }
                    }

                    // Middle View: Live Mirror or Quick App Launcher
                    if (streamState.isProjecting && streamState.latestFrame != null) {
                        // Embedded live stream window
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(130.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, NeonCyan, RoundedCornerShape(20.dp))
                                .background(DarkSurface)
                        ) {
                            Image(
                                bitmap = streamState.latestFrame.asImageBitmap(),
                                contentDescription = "Stream Feed",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .background(AmoledBlack.copy(alpha = 0.7f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "MIRROR LIVE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                        }
                    } else {
                        // Watch App Launcher Grid / Carousel
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "WATCH LAUNCHER",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            if (selectedApps.isEmpty()) {
                                Text(
                                    text = "Select apps from App Manager tab",
                                    fontSize = 10.sp,
                                    color = TextTertiary,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(selectedApps) { app ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { onAppClick(app) }
                                                .padding(2.dp)
                                                .testTag("watch_tile_${app.packageName}")
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(DarkSurface)
                                                    .border(1.dp, ElectricBlue.copy(alpha = 0.6f), CircleShape)
                                            ) {
                                                if (app.iconBitmap != null) {
                                                    Image(
                                                        bitmap = app.iconBitmap,
                                                        contentDescription = app.appName,
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                    )
                                                } else {
                                                    Text(
                                                        text = app.appName.take(1),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ElectricBlue
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = app.appName,
                                                fontSize = 9.sp,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.width(44.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Watch Media Remote Pill Bar
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onVolumeDown, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.VolumeDown, contentDescription = "Vol -", tint = TextSecondary, modifier = Modifier.size(15.dp))
                            }

                            IconButton(onClick = onPrev, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = TextPrimary, modifier = Modifier.size(16.dp))
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(ElectricBlue, NeonCyan)))
                                    .clickable(onClick = onPlayPause)
                                    .testTag("watch_hud_play_pause")
                            ) {
                                Icon(
                                    imageVector = if (mediaState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = AmoledBlack,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(onClick = onNext, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = TextPrimary, modifier = Modifier.size(16.dp))
                            }

                            IconButton(onClick = onVolumeUp, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Vol +", tint = TextSecondary, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
