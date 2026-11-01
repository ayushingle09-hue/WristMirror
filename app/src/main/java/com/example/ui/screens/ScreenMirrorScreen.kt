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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StreamResolution
import com.example.ui.MainViewModel
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
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun ScreenMirrorScreen(
    viewModel: MainViewModel,
    onRequestScreenProjection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val streamState by viewModel.streamState.collectAsState()
    val streamConfig by viewModel.streamConfig.collectAsState()
    val diagnosticReport by viewModel.bleManager.diagnosticReport.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        WristMirrorTopBar(
            title = "Screen Mirror",
            subtitle = "MediaProjection Stream Pipeline",
            connectionLabel = if (streamState.isProjecting) "LIVE ${String.format("%.1f", streamState.fps)} FPS" else "IDLE",
            connectionColor = if (streamState.isProjecting) NeonCyan else TextTertiary
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // 1. Futuristic Smartwatch AMOLED Bezel Display Preview
            item {
                WatchBezelMirrorView(
                    streamState = streamState,
                    resolution = streamConfig.resolution
                )
            }

            // 2. Stream Transmission Telemetry
            item {
                StreamTelemetryCard(streamState = streamState, streamConfig = streamConfig)
            }

            // 3. Primary Stream Controls (Start / Stop / Pause / Reconnect)
            item {
                StreamControlsBar(
                    isProjecting = streamState.isProjecting,
                    isPaused = streamState.isPaused,
                    onStart = onRequestScreenProjection,
                    onStop = { viewModel.stopMirroring() },
                    onTogglePause = { viewModel.toggleMirrorPause() }
                )
            }

            // 4. Low-Bandwidth Smartwatch Optimization Settings
            item {
                StreamOptimizationSettings(
                    config = streamConfig,
                    onResolutionChange = { viewModel.updateStreamResolution(it) },
                    onFpsChange = { viewModel.updateStreamFps(it) },
                    onToggleGrayscale = { viewModel.toggleGrayscale() }
                )
            }

            // 5. Hardware Protocol Transparency Note
            item {
                HardwareStreamProtocolNotice(diagnosticReport = diagnosticReport)
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun WatchBezelMirrorView(
    streamState: com.example.model.MirrorStreamState,
    resolution: StreamResolution
) {
    val isCircular = resolution == StreamResolution.CIRCULAR_AMOLED

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Smartwatch AMOLED Bezel Simulation",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = ElectricBlue,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Watch Outer Case & Straps
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(280.dp)
        ) {
            // Watch Strap Accents
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(280.dp)
                    .background(Color(0xFF141A24), RoundedCornerShape(20.dp))
            )

            // Watch Outer Metal Bezel
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(240.dp)
                    .clip(if (isCircular) CircleShape else RoundedCornerShape(48.dp))
                    .background(Brush.radialGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))))
                    .border(
                        3.dp,
                        Brush.linearGradient(listOf(ElectricBlue, NeonCyan.copy(alpha = 0.5f))),
                        if (isCircular) CircleShape else RoundedCornerShape(48.dp)
                    )
            ) {
                // AMOLED Display Inner Area
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(216.dp)
                        .clip(if (isCircular) CircleShape else RoundedCornerShape(36.dp))
                        .background(AmoledBlack)
                ) {
                    if (streamState.isProjecting && streamState.latestFrame != null) {
                        Image(
                            bitmap = streamState.latestFrame.asImageBitmap(),
                            contentDescription = "Live Mirror Stream",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Live overlay watermark
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmoledBlack.copy(alpha = 0.75f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "STREAM LIVE • ${String.format("%.0f", streamState.fps)} FPS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                    } else {
                        // Standby Futuristic Watch Face HUD
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenShare,
                                contentDescription = null,
                                tint = ElectricBlue.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "WristMirror",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "AMOLED 454x454 Ready",
                                fontSize = 10.sp,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Press Start Mirror to capture and transmit",
                                fontSize = 10.sp,
                                color = TextTertiary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamTelemetryCard(
    streamState: com.example.model.MirrorStreamState,
    streamConfig: com.example.model.MirrorStreamConfig
) {
    AmoledCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transmission Telemetry",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = streamConfig.resolution.label.split(" ")[0],
                    fontSize = 11.sp,
                    color = ElectricBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryStatItem(
                    label = "Frame Rate",
                    value = "${String.format("%.1f", streamState.fps)} FPS",
                    color = NeonCyan
                )
                TelemetryStatItem(
                    label = "Bandwidth",
                    value = "${String.format("%.1f", streamState.bandwidthKbps)} KB/s",
                    color = ElectricBlue
                )
                TelemetryStatItem(
                    label = "Packets Sent",
                    value = "${streamState.packetsSent}",
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Pipeline Status: ${streamState.statusMessage}",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun TelemetryStatItem(
    label: String,
    value: String,
    color: Color
) {
    Column {
        Text(text = label, fontSize = 10.sp, color = TextTertiary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun StreamControlsBar(
    isProjecting: Boolean,
    isPaused: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onTogglePause: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!isProjecting) {
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = AmoledBlack
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("mirror_start_stream_btn")
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Start Screen Mirroring", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        } else {
            Button(
                onClick = onTogglePause,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPaused) StatusWarning else DarkSurface,
                    contentColor = if (isPaused) AmoledBlack else TextPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("mirror_pause_stream_btn")
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = if (isPaused) "Resume" else "Pause", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusIncompatible,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("mirror_stop_stream_btn")
            ) {
                Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Stop Mirror", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun StreamOptimizationSettings(
    config: com.example.model.MirrorStreamConfig,
    onResolutionChange: (StreamResolution) -> Unit,
    onFpsChange: (Int) -> Unit,
    onToggleGrayscale: () -> Unit
) {
    AmoledCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Low-Bandwidth Transmission Settings",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "Display Resolution Profile", fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StreamResolution.values().forEach { res ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (config.resolution == res) ElectricBlue.copy(alpha = 0.15f) else DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (config.resolution == res) ElectricBlue else DarkCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onResolutionChange(res) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = res.label,
                                fontSize = 12.sp,
                                fontWeight = if (config.resolution == res) FontWeight.Bold else FontWeight.Normal,
                                color = if (config.resolution == res) ElectricBlue else TextPrimary
                            )
                            if (config.resolution == res) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(ElectricBlue)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Target FPS Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Target Frame Rate Limit", fontSize = 12.sp, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5, 10, 15).forEach { fps ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (config.targetFps == fps) NeonCyan.copy(alpha = 0.2f) else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (config.targetFps == fps) NeonCyan else DarkCardBorder
                            ),
                            modifier = Modifier.clickable { onFpsChange(fps) }
                        ) {
                            Text(
                                text = "$fps FPS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (config.targetFps == fps) NeonCyan else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Grayscale Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Monochrome / Grayscale Mode", fontSize = 13.sp, color = TextPrimary)
                    Text(text = "Reduces BLE data packet size by 60%", fontSize = 11.sp, color = TextSecondary)
                }
                Switch(
                    checked = config.isGrayscale,
                    onCheckedChange = { onToggleGrayscale() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AmoledBlack,
                        checkedTrackColor = ElectricBlue,
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = DarkSurface
                    )
                )
            }
        }
    }
}

@Composable
private fun HardwareStreamProtocolNotice(
    diagnosticReport: com.example.model.GattDiagnosticReport?
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F1826),
        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = ElectricBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Smartwatch Transmission Protocol Guide",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricBlue
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Android Bluetooth Low Energy has a typical MTU throughput of 20-50 KB/s. Devices like Redmi Watch Move (RTOS) do not contain custom video decoders or open frame buffers. WristMirror downsamples and compresses the frame pipeline so you can monitor your selected app on the Phone HUD companion, while forwarding media controls and notifications directly to the watch.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
