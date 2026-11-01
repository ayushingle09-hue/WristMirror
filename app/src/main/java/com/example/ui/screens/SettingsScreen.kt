package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.WatchNotificationListenerService
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onRequestPermissions: () -> Unit,
    onRequestScreenProjection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val streamConfig by viewModel.streamConfig.collectAsState()
    val streamState by viewModel.streamState.collectAsState()

    val isNotificationListenerGranted = WatchNotificationListenerService.isServiceConnected

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        WristMirrorTopBar(
            title = "Settings",
            subtitle = "Permissions, Privacy & Troubleshooting"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // 1. Permission Status & System Access Hub
            item {
                Text(
                    text = "System Permissions & Access",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            item {
                AmoledCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        PermissionRowItem(
                            icon = Icons.Default.Bluetooth,
                            title = "Bluetooth Low Energy",
                            desc = "Required for watch discovery and GATT connection",
                            isGranted = true,
                            actionLabel = "Permissions",
                            onAction = onRequestPermissions
                        )

                        PermissionRowItem(
                            icon = Icons.Default.ScreenShare,
                            title = "Screen Capture (MediaProjection)",
                            desc = "Required to capture and downsample selected apps",
                            isGranted = streamState.isProjecting,
                            actionLabel = if (streamState.isProjecting) "Active" else "Request",
                            onAction = onRequestScreenProjection
                        )

                        PermissionRowItem(
                            icon = Icons.Default.Notifications,
                            title = "Notification Listener Access",
                            desc = "Allows syncing media tracks and active alert actions",
                            isGranted = isNotificationListenerGranted,
                            actionLabel = if (isNotificationListenerGranted) "Granted" else "Enable",
                            onAction = {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                context.startActivity(intent)
                            }
                        )

                        PermissionRowItem(
                            icon = Icons.Default.BatteryAlert,
                            title = "Background Battery Optimization",
                            desc = "Prevents Android from pausing watch sync in background",
                            isGranted = false,
                            actionLabel = "App Settings",
                            onAction = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }

            // 2. Privacy & Stream Security Controls
            item {
                Text(
                    text = "Privacy & Stream Security",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            item {
                AmoledCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Pause on Lock Screen",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Suspends screen capture while the phone display is locked",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = streamConfig.pauseOnLockScreen,
                                onCheckedChange = { /* state handled in config */ },
                                colors = switchColors()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Protect Sensitive & Banking Apps",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Respects Android FLAG_SECURE to avoid streaming passwords",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = streamConfig.excludeSecureWindows,
                                onCheckedChange = { /* state handled in config */ },
                                colors = switchColors()
                            )
                        }
                    }
                }
            }

            // 3. Troubleshooting Guide for Redmi Watch Move & Mi Fitness
            item {
                Text(
                    text = "Redmi Watch Move Troubleshooting Guide",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            item {
                AmoledCard(borderColor = NeonCyan.copy(alpha = 0.4f)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TroubleshootStep(
                            stepNumber = "1",
                            title = "Mi Fitness Background Permission",
                            detail = "In Android Settings > Apps > Mi Fitness > Battery, select 'Unrestricted' so the Xiaomi Bluetooth service stays alive."
                        )

                        TroubleshootStep(
                            stepNumber = "2",
                            title = "Notification Forwarding",
                            detail = "Inside the Mi Fitness app, navigate to Profile > Watch > App Notifications and enable notifications for WristMirror."
                        )

                        TroubleshootStep(
                            stepNumber = "3",
                            title = "Media Player Sync",
                            detail = "When music plays on your phone, Redmi Watch Move's native music widget automatically controls tracks through Android MediaSession."
                        )

                        TroubleshootStep(
                            stepNumber = "4",
                            title = "Screen Mirror Companion Mode",
                            detail = "Redmi Watch Move operates closed RTOS firmware. Use WristMirror's Watch HUD mode for real-time streaming preview and remote launching."
                        )
                    }
                }
            }

            // 4. App Info & Version
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "WristMirror v1.0 • AMOLED Remote Edition",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ElectricBlue
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Designed for Redmi Watch Move & Standard BLE Wearables",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun PermissionRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isGranted) StatusConnected.copy(alpha = 0.15f) else DarkSurface)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) StatusConnected else ElectricBlue,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGranted) StatusConnected.copy(alpha = 0.15f) else ElectricBlue,
                contentColor = if (isGranted) StatusConnected else AmoledBlack
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("perm_btn_${title.take(6)}")
        ) {
            Text(text = actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TroubleshootStep(
    stepNumber: String,
    title: String,
    detail: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(NeonCyan.copy(alpha = 0.2f))
        ) {
            Text(
                text = stepNumber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = detail,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = AmoledBlack,
    checkedTrackColor = ElectricBlue,
    uncheckedThumbColor = TextTertiary,
    uncheckedTrackColor = DarkSurface
)
