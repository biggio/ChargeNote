package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChargingRecord
import com.example.ui.components.BatterySocBar
import com.example.ui.components.EfficiencyTrendChart
import com.example.ui.components.MetricCard
import com.example.ui.components.OperatorAnalyticsCard
import com.example.ui.components.PeriodDepthAnalysisCard
import com.example.ui.components.VehicleHeroCard
import com.example.ui.theme.LocalExtendedColors
import com.example.ui.viewmodel.EVViewModel
import com.example.util.CalculationUtils
import com.example.util.MapUtils

@Composable
fun DashboardScreen(
    viewModel: EVViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val extendedColors = LocalExtendedColors.current
    val records by viewModel.records.collectAsState()
    val vehicleSettings by viewModel.vehicleSettings.collectAsState()
    val overallMetrics by viewModel.overallMetrics.collectAsState()
    val periodGrouping by viewModel.periodGrouping.collectAsState()
    val chartMetric by viewModel.chartMetric.collectAsState()
    val periodStatsList by viewModel.periodStatsList.collectAsState()
    val selectedPeriodKey by viewModel.selectedPeriodKey.collectAsState()
    val selectedPeriodStats by viewModel.selectedPeriodStats.collectAsState()
    val operatorStatsList by viewModel.operatorStatsList.collectAsState()
    val operatorMetricTab by viewModel.operatorMetricTab.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var isTableExpanded by remember { mutableStateOf(false) }

    val latestRecord = records.firstOrNull()

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
                    .testTag("add_record_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "記一筆", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(extendedColors.accentPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "⚡", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "EV 智慧充電能耗",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = extendedColors.textPrimary
                        )
                        Text(
                            text = "真實行駛電耗與支出追蹤",
                            fontSize = 12.sp,
                            color = extendedColors.textSecondary
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(extendedColors.cardBackground)
                            .testTag("history_nav_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = extendedColors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(extendedColors.cardBackground)
                            .testTag("settings_nav_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = extendedColors.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Vehicle Hero Card
            VehicleHeroCard(
                vehicleSettings = vehicleSettings,
                metrics = overallMetrics
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Core 6 Metrics Grid (核心能耗與花費指標)
            Text(
                text = "核心能耗與花費指標",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = extendedColors.textPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Grid Row 1: Mileage & Total Charged
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "累積總里程",
                    value = CalculationUtils.formatNumber(overallMetrics.totalDistanceKm, 1),
                    unit = vehicleSettings.distanceUnit,
                    subtitle = "全期紀錄跨度行駛",
                    icon = Icons.Default.Speed,
                    iconColor = extendedColors.rangeCyan,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "累積充電度數",
                    value = CalculationUtils.formatNumber(overallMetrics.totalChargedKwh, 1),
                    unit = "kWh",
                    subtitle = "全期充入電量總計",
                    icon = Icons.Default.Bolt,
                    iconColor = extendedColors.accentSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grid Row 2: Total Cost & Average Real Efficiency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "累積充電支出",
                    value = CalculationUtils.formatCurrency(overallMetrics.totalCost),
                    unit = vehicleSettings.currencyUnit,
                    subtitle = "總充電費用支出",
                    icon = Icons.Default.AttachMoney,
                    iconColor = extendedColors.costOrange,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "平均校正電耗",
                    value = CalculationUtils.formatNumber(overallMetrics.avgEfficiencyKmPerKwh, 2),
                    unit = "km/kWh",
                    subtitle = "依 SoC 補正之真電耗",
                    subValue = "${CalculationUtils.formatNumber(overallMetrics.avgConsumptionKwhPer100Km, 1)} kWh/100km",
                    icon = Icons.Default.TrendingUp,
                    iconColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grid Row 3: Cost per KM & Cost per KWh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "每公里行駛成本",
                    value = CalculationUtils.formatNumber(overallMetrics.avgCostPerKm, 2),
                    unit = "${vehicleSettings.currencyUnit}/km",
                    subtitle = "行駛每公里耗費",
                    icon = Icons.Default.PriceCheck,
                    iconColor = extendedColors.costOrange,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "平均每度電費",
                    value = CalculationUtils.formatNumber(overallMetrics.avgCostPerKwh, 2),
                    unit = "${vehicleSettings.currencyUnit}/kWh",
                    subtitle = "平均每充一度電",
                    icon = Icons.Default.ElectricCar,
                    iconColor = extendedColors.accentPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Efficiency Trend Chart
            EfficiencyTrendChart(
                periodStatsList = periodStatsList,
                overallMetrics = overallMetrics,
                vehicleSettings = vehicleSettings,
                periodGrouping = periodGrouping,
                chartMetric = chartMetric,
                selectedPeriodKey = selectedPeriodKey,
                onGroupingChange = { viewModel.setPeriodGrouping(it) },
                onMetricChange = { viewModel.setChartMetric(it) },
                onPeriodSelect = { viewModel.selectPeriod(it) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Period Depth Analysis Card (當期能耗深度解析)
            if (selectedPeriodStats != null) {
                PeriodDepthAnalysisCard(
                    periodStats = selectedPeriodStats!!,
                    currencyUnit = vehicleSettings.currencyUnit
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 5. Operator Distribution & Analytics Card (充電營運商佔比分析)
            if (operatorStatsList.isNotEmpty()) {
                OperatorAnalyticsCard(
                    operatorStatsList = operatorStatsList,
                    selectedTab = operatorMetricTab,
                    onTabSelect = { viewModel.setOperatorMetricTab(it) },
                    currencyUnit = vehicleSettings.currencyUnit
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 6. Collapsible Period Data Table Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(extendedColors.cardBackground)
                    .border(1.dp, extendedColors.cardBorder, RoundedCornerShape(12.dp))
                    .clickable { isTableExpanded = !isTableExpanded }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📊", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "展開各期${periodGrouping.label}能耗數據表 (${periodStatsList.size} 期)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = extendedColors.textPrimary
                        )
                    }
                    Icon(
                        imageVector = if (isTableExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Table",
                        tint = extendedColors.textSecondary
                    )
                }
            }

            AnimatedVisibility(
                visible = isTableExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(extendedColors.subCardBackground)
                        .border(1.dp, extendedColors.cardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("期別", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = extendedColors.textSecondary, modifier = Modifier.weight(1.2f))
                        Text("里程", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = extendedColors.textSecondary, modifier = Modifier.weight(1f))
                        Text("充入", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = extendedColors.textSecondary, modifier = Modifier.weight(1f))
                        Text("電耗", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = extendedColors.textSecondary, modifier = Modifier.weight(1.2f))
                        Text("費用", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = extendedColors.textSecondary, modifier = Modifier.weight(1f))
                    }
                    Divider(color = extendedColors.cardBorder)

                    periodStatsList.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectPeriod(p.periodKey) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(p.periodKey, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (p.periodKey == selectedPeriodKey) extendedColors.accentSecondary else extendedColors.textPrimary, modifier = Modifier.weight(1.2f))
                            Text("${CalculationUtils.formatNumber(p.totalDistanceKm, 0)}k", fontSize = 12.sp, color = extendedColors.textPrimary, modifier = Modifier.weight(1f))
                            Text("${CalculationUtils.formatNumber(p.totalChargedKwh, 0)}k", fontSize = 12.sp, color = extendedColors.textPrimary, modifier = Modifier.weight(1f))
                            Text("${CalculationUtils.formatNumber(p.efficiencyKmPerKwh, 2)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = extendedColors.rangeCyan, modifier = Modifier.weight(1.2f))
                            Text("${CalculationUtils.formatCurrency(p.totalCost)}", fontSize = 12.sp, color = extendedColors.costOrange, modifier = Modifier.weight(1f))
                        }
                        Divider(color = extendedColors.cardBorder.copy(alpha = 0.5f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 6. Latest Charging Session Quick Card
            if (latestRecord != null) {
                Text(
                    text = "最新充電紀錄",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = extendedColors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(extendedColors.cardBackground)
                        .border(1.dp, extendedColors.cardBorder, RoundedCornerShape(14.dp))
                        .clickable { onNavigateToHistory() }
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // DC/AC Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (latestRecord.chargeType == "DC") extendedColors.dcColor else extendedColors.acColor)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = latestRecord.chargeType,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = latestRecord.operator,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = extendedColors.textPrimary
                                )
                            }

                            Text(
                                text = CalculationUtils.formatDate(latestRecord.timestamp, "MM/dd HH:mm"),
                                fontSize = 12.sp,
                                color = extendedColors.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        BatterySocBar(
                            socPercent = latestRecord.socPercent,
                            label = "最新電量"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "儀表：${CalculationUtils.formatNumber(latestRecord.odometerKm, 1)} km",
                                fontSize = 12.sp,
                                color = extendedColors.textSecondary
                            )
                            Text(
                                text = "充入：${CalculationUtils.formatNumber(latestRecord.energyKwh, 1)} kWh",
                                fontSize = 12.sp,
                                color = extendedColors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "支出：${vehicleSettings.currencyUnit} ${CalculationUtils.formatCurrency(latestRecord.cost)}",
                                fontSize = 12.sp,
                                color = extendedColors.costOrange,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val hasLatestGps = latestRecord.latitude != null && latestRecord.longitude != null && latestRecord.latitude != 0.0 && latestRecord.longitude != 0.0
                        val hasLatestLoc = latestRecord.locationName.isNotBlank() || hasLatestGps

                        if (hasLatestLoc) {
                            val locText = when {
                                latestRecord.locationName.isNotBlank() -> latestRecord.locationName
                                hasLatestGps -> "GPS: ${CalculationUtils.formatNumber(latestRecord.latitude!!, 4)}, ${CalculationUtils.formatNumber(latestRecord.longitude!!, 4)}"
                                else -> ""
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(extendedColors.subCardBackground)
                                    .border(0.8.dp, extendedColors.accentSecondary.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        MapUtils.openInGoogleMaps(
                                            context = context,
                                            latitude = latestRecord.latitude,
                                            longitude = latestRecord.longitude,
                                            locationName = latestRecord.locationName
                                        )
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Open in Maps",
                                    tint = extendedColors.accentSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = locText,
                                    fontSize = 11.sp,
                                    color = extendedColors.textPrimary,
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
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    if (showAddDialog) {
        AddEditRecordDialog(
            latestOdometer = latestRecord?.odometerKm ?: 0.0,
            viewModel = viewModel,
            onDismiss = { showAddDialog = false },
            onSave = { newRecord ->
                viewModel.insertRecord(newRecord)
                showAddDialog = false
            }
        )
    }
}
