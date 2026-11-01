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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppInfo
import com.example.ui.AppFilter
import com.example.ui.MainViewModel
import com.example.ui.components.AmoledCard
import com.example.ui.components.WristMirrorTopBar
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun AppManagerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val filteredApps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.appSearchQuery.collectAsState()
    val currentFilter by viewModel.appFilter.collectAsState()
    val isLoading by viewModel.appManager.isLoading.collectAsState()
    val selectedCount by viewModel.selectedAppsCount.collectAsState()
    val totalAppsCount = viewModel.appManager.installedApps.collectAsState().value.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        WristMirrorTopBar(
            title = "App Manager",
            subtitle = "$selectedCount of $totalAppsCount apps selected to mirror"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search installed applications...", color = TextTertiary, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = ElectricBlue
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedContainerColor = DarkCardBg,
                    unfocusedContainerColor = DarkCardBg,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips & Quick Select Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = currentFilter == AppFilter.ALL,
                        onClick = { viewModel.setAppFilter(AppFilter.ALL) },
                        label = { Text("All ($totalAppsCount)") },
                        colors = chipColors()
                    )
                }
                item {
                    FilterChip(
                        selected = currentFilter == AppFilter.SELECTED,
                        onClick = { viewModel.setAppFilter(AppFilter.SELECTED) },
                        label = { Text("Selected ($selectedCount)") },
                        colors = chipColors()
                    )
                }
                item {
                    FilterChip(
                        selected = currentFilter == AppFilter.USER_ONLY,
                        onClick = { viewModel.setAppFilter(AppFilter.USER_ONLY) },
                        label = { Text("User Apps") },
                        colors = chipColors()
                    )
                }
                item {
                    FilterChip(
                        selected = currentFilter == AppFilter.SYSTEM_ONLY,
                        onClick = { viewModel.setAppFilter(AppFilter.SYSTEM_ONLY) },
                        label = { Text("System Apps") },
                        colors = chipColors()
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick selection action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Select All User",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonCyan,
                    modifier = Modifier
                        .clickable { viewModel.selectAllUserApps() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("btn_select_all_user_apps")
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Clear All",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextTertiary,
                    modifier = Modifier
                        .clickable { viewModel.clearAllSelections() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("btn_clear_all_apps")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ElectricBlue)
                }
            } else if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No apps found matching \"$searchQuery\"" else "No apps found in this category",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppListItemCard(
                            app = app,
                            onToggleSelect = { viewModel.toggleAppSelection(app.packageName) },
                            onLaunch = { viewModel.launchApp(app.packageName) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AppListItemCard(
    app: AppInfo,
    onToggleSelect: () -> Unit,
    onLaunch: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkCardBg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (app.isSelected) ElectricBlue.copy(alpha = 0.5f) else DarkCardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onToggleSelect)
            .testTag("app_row_${app.packageName}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            if (app.iconBitmap != null) {
                Image(
                    bitmap = app.iconBitmap,
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurface)
                ) {
                    Text(
                        text = app.appName.take(1),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (app.isSystemApp) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DarkSurface
                        ) {
                            Text(
                                text = "SYS",
                                fontSize = 9.sp,
                                color = TextTertiary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = app.packageName,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Test launch button
            IconButton(
                onClick = onLaunch,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_launch_${app.packageName}")
            ) {
                Icon(
                    imageVector = Icons.Default.Launch,
                    contentDescription = "Launch on Phone",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Selection Switch
            Switch(
                checked = app.isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AmoledBlack,
                    checkedTrackColor = ElectricBlue,
                    uncheckedThumbColor = TextTertiary,
                    uncheckedTrackColor = DarkSurface
                ),
                modifier = Modifier.testTag("switch_select_${app.packageName}")
            )
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = DarkCardBg,
    labelColor = TextSecondary,
    selectedContainerColor = ElectricBlue.copy(alpha = 0.2f),
    selectedLabelColor = ElectricBlue
)
