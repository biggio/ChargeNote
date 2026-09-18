package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalExtendedColors
import com.example.util.CalculationUtils
import com.example.util.OperatorMetricTab
import com.example.util.OperatorStats

private val OPERATOR_PALETTE = listOf(
    Color(0xFF10B981), // Emerald Green (Home / #1)
    Color(0xFF38BDF8), // Cyan
    Color(0xFFF59E0B), // Amber / Orange
    Color(0xFF8B5CF6), // Purple
    Color(0xFFEC4899), // Pink
    Color(0xFF3B82F6), // Blue
    Color(0xFF14B8A6), // Teal
    Color(0xFF64748B)  // Slate
)

@Composable
fun OperatorAnalyticsCard(
    operatorStatsList: List<OperatorStats>,
    selectedTab: OperatorMetricTab,
    onTabSelect: (OperatorMetricTab) -> Unit,
    currencyUnit: String,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalExtendedColors.current
    var isExpanded by remember { mutableStateOf(false) }

    if (operatorStatsList.isEmpty()) return

    val totalChargedKwh = operatorStatsList.sumOf { it.totalChargedKwh }
    val totalSessions = operatorStatsList.sumOf { it.sessionCount }
    val totalCost = operatorStatsList.sumOf { it.totalCost }

    val sortedList = remember(operatorStatsList, selectedTab) {
        when (selectedTab) {
            OperatorMetricTab.ENERGY -> operatorStatsList.sortedByDescending { it.totalChargedKwh }
            OperatorMetricTab.SESSIONS -> operatorStatsList.sortedByDescending { it.sessionCount }
            OperatorMetricTab.COST -> operatorStatsList.sortedByDescending { it.totalCost }
        }
    }

    val displayList = if (isExpanded || sortedList.size <= 3) sortedList else sortedList.take(3)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(extendedColors.cardBackground)
            .border(1.dp, extendedColors.cardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(extendedColors.accentPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = "Operator Distribution",
                            tint = extendedColors.accentPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "充電營運商佔比分析",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = extendedColors.textPrimary
                        )
                        Text(
                            text = "共 ${operatorStatsList.size} 家營運商紀錄 · 主要為 ${sortedList.firstOrNull()?.operatorName ?: ""}",
                            fontSize = 11.sp,
                            color = extendedColors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metric Selector Tabs (電量 / 次數 / 花費)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(extendedColors.subCardBackground)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OperatorMetricTab.values().forEach { tab ->
                    val isSelected = tab == selectedTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) extendedColors.cardBackground else Color.Transparent)
                            .clickable { onTabSelect(tab) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) extendedColors.accentPrimary else extendedColors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-segment Proportional Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(extendedColors.subCardBackground)
            ) {
                sortedList.forEachIndexed { index, op ->
                    val pct = when (selectedTab) {
                        OperatorMetricTab.ENERGY -> op.kwhPercentage
                        OperatorMetricTab.SESSIONS -> op.countPercentage
                        OperatorMetricTab.COST -> op.costPercentage
                    }
                    if (pct > 0.5) {
                        val color = OPERATOR_PALETTE[index % OPERATOR_PALETTE.size]
                        Box(
                            modifier = Modifier
                                .weight(pct.toFloat().coerceAtLeast(0.1f))
                                .height(14.dp)
                                .background(color)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Operator Items List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                displayList.forEachIndexed { index, op ->
                    val color = OPERATOR_PALETTE[index % OPERATOR_PALETTE.size]
                    val pct = when (selectedTab) {
                        OperatorMetricTab.ENERGY -> op.kwhPercentage
                        OperatorMetricTab.SESSIONS -> op.countPercentage
                        OperatorMetricTab.COST -> op.costPercentage
                    }

                    OperatorRowItem(
                        rank = index + 1,
                        stat = op,
                        percentage = pct,
                        color = color,
                        selectedTab = selectedTab,
                        currencyUnit = currencyUnit
                    )
                }
            }

            // Expand / Collapse button if > 3 operators
            if (sortedList.size > 3) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "收起詳細列表" else "展開其餘 ${sortedList.size - 3} 家營運商",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = extendedColors.accentPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = extendedColors.accentPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OperatorRowItem(
    rank: Int,
    stat: OperatorStats,
    percentage: Double,
    color: Color,
    selectedTab: OperatorMetricTab,
    currencyUnit: String
) {
    val extendedColors = LocalExtendedColors.current
    val isHome = stat.operatorName.contains("家") || stat.operatorName.contains("自")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(extendedColors.subCardBackground)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Rank Circle
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$rank",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = if (isHome) Icons.Default.Home else Icons.Default.ElectricBolt,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = stat.operatorName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = extendedColors.textPrimary
                )
            }

            // Percentage Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.15f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${CalculationUtils.formatNumber(percentage, 1)}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(2.5.dp))
                .background(color.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((percentage / 100.0).toFloat().coerceIn(0.02f, 1f))
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(color)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3-Metric Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "⚡ ${CalculationUtils.formatNumber(stat.totalChargedKwh, 1)} kWh",
                fontSize = 11.sp,
                fontWeight = if (selectedTab == OperatorMetricTab.ENERGY) FontWeight.Bold else FontWeight.Normal,
                color = if (selectedTab == OperatorMetricTab.ENERGY) extendedColors.accentPrimary else extendedColors.textSecondary
            )

            Text(
                text = "🔄 ${stat.sessionCount} 次 (DC:${stat.dcCount} / AC:${stat.acCount})",
                fontSize = 11.sp,
                fontWeight = if (selectedTab == OperatorMetricTab.SESSIONS) FontWeight.Bold else FontWeight.Normal,
                color = if (selectedTab == OperatorMetricTab.SESSIONS) extendedColors.accentPrimary else extendedColors.textSecondary
            )

            Text(
                text = "💰 $currencyUnit ${CalculationUtils.formatCurrency(stat.totalCost)} (${CalculationUtils.formatNumber(stat.avgCostPerKwh, 1)}/度)",
                fontSize = 11.sp,
                fontWeight = if (selectedTab == OperatorMetricTab.COST) FontWeight.Bold else FontWeight.Normal,
                color = if (selectedTab == OperatorMetricTab.COST) extendedColors.costOrange else extendedColors.textSecondary
            )
        }
    }
}
