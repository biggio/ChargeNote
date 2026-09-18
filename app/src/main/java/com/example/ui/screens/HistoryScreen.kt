package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChargingRecord
import com.example.ui.components.BatterySocBar
import com.example.ui.theme.LocalExtendedColors
import com.example.ui.viewmodel.EVViewModel
import com.example.util.CalculationUtils
import com.example.util.MapUtils

@Composable
fun HistoryScreen(
    viewModel: EVViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalExtendedColors.current
    val records by viewModel.records.collectAsState()
    val vehicleSettings by viewModel.vehicleSettings.collectAsState()
    val recordIntervalMap by viewModel.recordIntervalMap.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val historyFilter by viewModel.historyFilter.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<ChargingRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<ChargingRecord?>(null) }

    val filteredRecords = remember(records, searchQuery, historyFilter) {
        records.filter { r ->
            val matchQuery = searchQuery.isBlank() ||
                    r.operator.contains(searchQuery, ignoreCase = true) ||
                    r.locationName.contains(searchQuery, ignoreCase = true) ||
                    r.notes.contains(searchQuery, ignoreCase = true) ||
                    r.odometerKm.toString().contains(searchQuery)

            val matchFilter = when (historyFilter) {
                "DC" -> r.chargeType.equals("DC", ignoreCase = true)
                "AC" -> !r.chargeType.equals("DC", ignoreCase = true)
                "HOME" -> r.operator.contains("家", ignoreCase = true) || r.operator.contains("自", ignoreCase = true)
                else -> true
            }
            matchQuery && matchFilter
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = extendedColors.accentPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("history_add_record_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "新增紀錄", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(extendedColors.cardBackground)
                        .testTag("history_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = extendedColors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "充電紀錄明細",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.textPrimary
                    )
                    Text(
                        text = "共 ${filteredRecords.size} 筆紀錄 · 總支出 ${vehicleSettings.currencyUnit} ${CalculationUtils.formatCurrency(filteredRecords.sumOf { it.cost })}",
                        fontSize = 12.sp,
                        color = extendedColors.textSecondary
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("搜尋站點、營運商、備註、里程...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = extendedColors.textSecondary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = extendedColors.textSecondary
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_field"),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = extendedColors.cardBackground,
                    unfocusedContainerColor = extendedColors.cardBackground,
                    focusedTextColor = extendedColors.textPrimary,
                    unfocusedTextColor = extendedColors.textPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "全部紀錄",
                    "DC" to "⚡ DC 快充",
                    "AC" to "🔌 AC 慢充",
                    "HOME" to "🏠 家充/自宅"
                ).forEach { (key, label) ->
                    val isSelected = historyFilter == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) extendedColors.accentPrimary else extendedColors.cardBackground)
                            .border(
                                1.dp,
                                if (isSelected) extendedColors.accentPrimary else extendedColors.cardBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { viewModel.setHistoryFilter(key) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else extendedColors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Record List
            if (filteredRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🔌", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "查無充電紀錄",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = extendedColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "點擊右下角按鈕新增一筆紀錄",
                            fontSize = 13.sp,
                            color = extendedColors.textSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredRecords, key = { it.id }) { record ->
                        val interval = recordIntervalMap[record.id]

                        HistoryRecordCard(
                            record = record,
                            intervalStats = interval,
                            currencyUnit = vehicleSettings.currencyUnit,
                            distanceUnit = vehicleSettings.distanceUnit,
                            onEdit = { recordToEdit = record },
                            onDelete = { recordToDelete = record }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditRecordDialog(
            latestOdometer = records.firstOrNull()?.odometerKm ?: 0.0,
            viewModel = viewModel,
            onDismiss = { showAddDialog = false },
            onSave = { newRecord ->
                viewModel.insertRecord(newRecord)
                showAddDialog = false
            }
        )
    }

    if (recordToEdit != null) {
        AddEditRecordDialog(
            initialRecord = recordToEdit,
            viewModel = viewModel,
            onDismiss = { recordToEdit = null },
            onSave = { updated ->
                viewModel.updateRecord(updated)
                recordToEdit = null
            }
        )
    }

    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("確認刪除充電紀錄？", color = extendedColors.textPrimary) },
            text = {
                Text(
                    "確定要刪除 ${CalculationUtils.formatDate(recordToDelete!!.timestamp)} 於 ${recordToDelete!!.operator} 的這筆紀錄嗎？此動作無法復原。",
                    color = extendedColors.textSecondary
                )
            },
            containerColor = extendedColors.cardBackground,
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecord(recordToDelete!!)
                        recordToDelete = null
                    }
                ) {
                    Text("刪除", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("取消", color = extendedColors.textSecondary)
                }
            }
        )
    }
}

@Composable
fun HistoryRecordCard(
    record: ChargingRecord,
    intervalStats: com.example.util.RecordIntervalStats?,
    currencyUnit: String,
    distanceUnit: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalExtendedColors.current
    val isDc = record.chargeType.equals("DC", ignoreCase = true)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(extendedColors.cardBackground)
            .border(1.dp, extendedColors.cardBorder, RoundedCornerShape(24.dp))
            .padding(16.dp)
            .testTag("record_card_${record.id}")
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // DC / AC Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDc) extendedColors.dcColor else extendedColors.acColor)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isDc) "DC 快充" else "AC 慢充",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = record.operator,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.textPrimary
                    )
                }

                Text(
                    text = CalculationUtils.formatDate(record.timestamp),
                    fontSize = 12.sp,
                    color = extendedColors.textSecondary
                )
            }

            // Location & GPS Google Maps Link
            val context = LocalContext.current
            val hasGps = record.latitude != null && record.longitude != null && record.latitude != 0.0 && record.longitude != 0.0
            val hasLocation = record.locationName.isNotBlank() || hasGps

            if (hasLocation) {
                val displayLocText = when {
                    record.locationName.isNotBlank() && hasGps -> "${record.locationName} (${CalculationUtils.formatNumber(record.latitude!!, 4)}, ${CalculationUtils.formatNumber(record.longitude!!, 4)})"
                    record.locationName.isNotBlank() -> record.locationName
                    hasGps -> "GPS: ${CalculationUtils.formatNumber(record.latitude!!, 4)}, ${CalculationUtils.formatNumber(record.longitude!!, 4)}"
                    else -> ""
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(extendedColors.subCardBackground)
                        .border(0.8.dp, extendedColors.accentSecondary.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .clickable {
                            MapUtils.openInGoogleMaps(
                                context = context,
                                latitude = record.latitude,
                                longitude = record.longitude,
                                locationName = record.locationName
                            )
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Open in Google Maps",
                        tint = extendedColors.accentSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = displayLocText,
                        fontSize = 11.sp,
                        color = extendedColors.textPrimary,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "開啟地圖 ↗",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.accentSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Battery SoC Bar
            BatterySocBar(
                socPercent = record.socPercent,
                label = "充飽電量 (SoC)"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stats 2x2 Breakdown
            Row(modifier = Modifier.fillMaxWidth()) {
                // Col 1: Distance & Energy
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "儀表總里程：${CalculationUtils.formatNumber(record.odometerKm, 1)} $distanceUnit",
                        fontSize = 12.sp,
                        color = extendedColors.textSecondary
                    )
                    if (intervalStats != null && intervalStats.deltaKm > 0) {
                        Text(
                            text = "區間行駛：+${CalculationUtils.formatNumber(intervalStats.deltaKm, 1)} $distanceUnit",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = extendedColors.rangeCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "充入度數：${CalculationUtils.formatNumber(record.energyKwh, 2)} kWh",
                        fontSize = 12.sp,
                        color = extendedColors.textPrimary
                    )
                }

                // Col 2: Efficiency & Cost
                Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                    if (intervalStats != null && intervalStats.efficiencyKmPerKwh > 0 && !record.skipEfficiencyCalc) {
                        Text(
                            text = "區間電耗：${CalculationUtils.formatNumber(intervalStats.efficiencyKmPerKwh, 2)} km/kWh",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = extendedColors.accentSecondary
                        )
                        Text(
                            text = "(${CalculationUtils.formatNumber(intervalStats.consumptionKwhPer100Km, 1)} kWh/100km)",
                            fontSize = 11.sp,
                            color = extendedColors.textMuted
                        )
                    } else if (record.skipEfficiencyCalc) {
                        Text(
                            text = "⚠️ 略過電耗計算",
                            fontSize = 11.sp,
                            color = Color(0xFFF59E0B)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "費用：$currencyUnit ${CalculationUtils.formatCurrency(record.cost)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.costOrange
                    )
                    if (record.energyKwh > 0 && record.cost > 0) {
                        Text(
                            text = "($currencyUnit ${CalculationUtils.formatNumber(record.cost / record.energyKwh, 2)}/kWh)",
                            fontSize = 11.sp,
                            color = extendedColors.textMuted
                        )
                    }
                }
            }

            // Notes
            if (record.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(extendedColors.subCardBackground)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "📝 ${record.notes}",
                        fontSize = 11.sp,
                        color = extendedColors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Actions: Edit / Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = extendedColors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
