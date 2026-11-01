package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GattCharacteristicInfo
import com.example.model.GattDiagnosticReport
import com.example.model.GattServiceInfo
import com.example.model.WatchDevice
import com.example.model.WatchEcosystemType
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
import com.example.ui.theme.StatusScanning
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun BleDiagnosticsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isScanning by viewModel.bleManager.isScanning.collectAsState()
    val discoveredDevices by viewModel.bleManager.discoveredDevices.collectAsState()
    val connectedDevice by viewModel.bleManager.connectedDevice.collectAsState()
    val diagnosticReport by viewModel.bleManager.diagnosticReport.collectAsState()
    val connectionStatus by viewModel.bleManager.connectionStatus.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        WristMirrorTopBar(
            title = "BLE & Diagnostics",
            subtitle = "GATT Interrogation & Compatibility Engine",
            connectionLabel = if (isScanning) "SCANNING" else if (connectedDevice != null) "CONNECTED" else "IDLE",
            connectionColor = if (isScanning) StatusScanning else if (connectedDevice != null) StatusConnected else TextTertiary
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // 1. Scanner Controller
            item {
                BleScanControlCard(
                    isScanning = isScanning,
                    statusText = connectionStatus,
                    onToggleScan = {
                        if (isScanning) viewModel.stopBleScan() else viewModel.startBleScan()
                    },
                    onRunRedmiDiagnosticSimulation = {
                        viewModel.loadRedmiMoveDiagnostics()
                    }
                )
            }

            // 2. Hardware Compatibility Diagnostic Report (CRITICAL)
            if (diagnosticReport != null) {
                item {
                    DiagnosticReportCard(report = diagnosticReport!!)
                }
            }

            // 3. Discovered Devices Section
            item {
                Text(
                    text = "Discovered Bluetooth LE Devices (${discoveredDevices.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            if (discoveredDevices.isEmpty()) {
                item {
                    AmoledCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.BluetoothSearching,
                                contentDescription = null,
                                tint = ElectricBlue.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isScanning) "Searching for nearby smartwatches..." else "No devices discovered yet. Press Start BLE Scan or test the Redmi Watch Move profile above.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(discoveredDevices, key = { it.address }) { device ->
                    DeviceListItemCard(
                        device = device,
                        isConnected = connectedDevice?.address == device.address,
                        onConnect = { viewModel.connectBleDevice(device.address) },
                        onDisconnect = { viewModel.disconnectBle() }
                    )
                }
            }

            // 4. Detailed GATT Services Inspector (if connected / analyzed)
            if (diagnosticReport != null && diagnosticReport!!.services.isNotEmpty()) {
                item {
                    Text(
                        text = "Discovered GATT Services & Characteristics (${diagnosticReport!!.services.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                items(diagnosticReport!!.services) { service ->
                    GattServiceAccordion(service = service)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun BleScanControlCard(
    isScanning: Boolean,
    statusText: String,
    onToggleScan: () -> Unit,
    onRunRedmiDiagnosticSimulation: () -> Unit
) {
    AmoledCard {
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue.copy(alpha = 0.15f))
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                color = ElectricBlue,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isScanning) "BLE Scanner Active" else "BLE Scanner Idle",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                StatusPill(
                    label = if (isScanning) "Scanning" else "Ready",
                    statusColor = if (isScanning) StatusScanning else TextTertiary,
                    isPulsing = isScanning
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onToggleScan,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanning) StatusIncompatible else ElectricBlue,
                        contentColor = AmoledBlack
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ble_scan_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.BluetoothSearching,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isScanning) "Stop Scan" else "Start BLE Scan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = onRunRedmiDiagnosticSimulation,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurface,
                        contentColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("btn_run_redmi_simulation")
                ) {
                    Text(
                        text = "Diagnose Redmi Move",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticReportCard(report: GattDiagnosticReport) {
    val isRedmi = report.ecosystem == WatchEcosystemType.REDMI_XIAOMI_MI_FITNESS

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = DarkCardBg,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isRedmi) NeonCyan else ElectricBlue
        ),
        modifier = Modifier.fillMaxWidth().testTag("diagnostic_report_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hardware Diagnostic Verdict",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRedmi) NeonCyan else ElectricBlue,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = report.deviceName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                StatusPill(
                    label = if (isRedmi) "Xiaomi RTOS" else "Standard BLE",
                    statusColor = if (isRedmi) StatusWarning else StatusConnected
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Summary description
            Text(
                text = report.summary,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Supported Features
            Text(
                text = "✓ Supported Hardware Features",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = StatusConnected
            )
            Spacer(modifier = Modifier.height(4.dp))
            report.supportedFeatures.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusConnected,
                        modifier = Modifier.size(14.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = feature,
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Restricted Features
            Text(
                text = "✕ Firmware-Restricted Features",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = StatusIncompatible
            )
            Spacer(modifier = Modifier.height(4.dp))
            report.restrictedFeatures.forEach { restriction ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = StatusIncompatible,
                        modifier = Modifier.size(14.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = restriction,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recommended Action
            Text(
                text = "💡 WristMirror Operating Strategy",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricBlue
            )
            Spacer(modifier = Modifier.height(4.dp))
            report.recommendedActions.forEach { action ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue)
                            .padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = action,
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceListItemCard(
    device: WatchDevice,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkCardBg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isConnected) StatusConnected else DarkCardBorder
        ),
        modifier = Modifier.fillMaxWidth().testTag("ble_device_${device.address}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isConnected) StatusConnected.copy(alpha = 0.15f) else DarkSurface)
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = if (isConnected) StatusConnected else ElectricBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${device.address}  •  RSSI: ${device.rssi} dBm",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Button(
                onClick = if (isConnected) onDisconnect else onConnect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) StatusIncompatible.copy(alpha = 0.2f) else ElectricBlue,
                    contentColor = if (isConnected) StatusIncompatible else AmoledBlack
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_connect_${device.address}")
            ) {
                Text(
                    text = if (isConnected) "Disconnect" else "Diagnose",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GattServiceAccordion(service: GattServiceInfo) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue
                    )
                    Text(
                        text = service.uuid,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = DarkCardBg
                    ) {
                        Text(
                            text = "${service.characteristics.size} chars",
                            fontSize = 10.sp,
                            color = TextTertiary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = TextSecondary
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    service.characteristics.forEach { ch ->
                        CharacteristicRow(ch = ch)
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacteristicRow(ch: GattCharacteristicInfo) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkCardBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ch.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ch.properties.forEach { prop ->
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = when (prop) {
                                "READ" -> StatusConnected.copy(alpha = 0.2f)
                                "WRITE" -> ElectricBlue.copy(alpha = 0.2f)
                                "NOTIFY" -> NeonCyan.copy(alpha = 0.2f)
                                else -> DarkSurface
                            }
                        ) {
                            Text(
                                text = prop,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (prop) {
                                    "READ" -> StatusConnected
                                    "WRITE" -> ElectricBlue
                                    "NOTIFY" -> NeonCyan
                                    else -> TextSecondary
                                },
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = ch.uuid,
                fontSize = 9.sp,
                color = TextTertiary,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
