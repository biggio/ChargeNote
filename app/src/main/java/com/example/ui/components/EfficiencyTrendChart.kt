package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VehicleSettings
import com.example.ui.theme.LocalExtendedColors
import com.example.util.CalculationUtils
import com.example.util.ChartMetric
import com.example.util.OverallMetrics
import com.example.util.PeriodGrouping
import com.example.util.PeriodStats
import kotlin.math.max

@Composable
fun EfficiencyTrendChart(
    periodStatsList: List<PeriodStats>,
    overallMetrics: OverallMetrics,
    vehicleSettings: VehicleSettings,
    periodGrouping: PeriodGrouping,
    chartMetric: ChartMetric,
    selectedPeriodKey: String?,
    onGroupingChange: (PeriodGrouping) -> Unit,
    onMetricChange: (ChartMetric) -> Unit,
    onPeriodSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalExtendedColors.current

    // Extract values based on selected metric
    val dataPoints = remember(periodStatsList, chartMetric) {
        periodStatsList.map { stat ->
            val value = when (chartMetric) {
                ChartMetric.KM_PER_KWH -> stat.efficiencyKmPerKwh
                ChartMetric.KWH_PER_100KM -> stat.consumptionKwhPer100Km
                ChartMetric.COST_PER_KM -> stat.costPerKm
            }
            Pair(stat, value)
        }
    }

    val values = dataPoints.map { it.second }.filter { it > 0 }
    val meanValue = when (chartMetric) {
        ChartMetric.KM_PER_KWH -> overallMetrics.avgEfficiencyKmPerKwh
        ChartMetric.KWH_PER_100KM -> overallMetrics.avgConsumptionKwhPer100Km
        ChartMetric.COST_PER_KM -> overallMetrics.avgCostPerKm
    }

    val bestValue = if (values.isNotEmpty()) {
        if (chartMetric == ChartMetric.KWH_PER_100KM || chartMetric == ChartMetric.COST_PER_KM) values.minOrNull() ?: 0.0
        else values.maxOrNull() ?: 0.0
    } else 0.0

    val officialValue = if (vehicleSettings.usableBatteryKwh > 0 && vehicleSettings.officialRangeKm > 0) {
        when (chartMetric) {
            ChartMetric.KM_PER_KWH -> vehicleSettings.officialRangeKm / vehicleSettings.usableBatteryKwh
            ChartMetric.KWH_PER_100KM -> (vehicleSettings.usableBatteryKwh / vehicleSettings.officialRangeKm) * 100.0
            ChartMetric.COST_PER_KM -> 0.0
        }
    } else 0.0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(extendedColors.cardBackground)
            .border(1.dp, extendedColors.cardBorder, RoundedCornerShape(24.dp))
            .padding(18.dp)
            .testTag("efficiency_trend_chart")
    ) {
        Column {
            // Header Row with Title and SoC Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "各期實際電耗趨勢",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = extendedColors.textPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(extendedColors.accentPrimary.copy(alpha = 0.12f))
                        .border(1.dp, extendedColors.accentPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "SoC 前後校正",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.accentPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "依${periodGrouping.label}統計之真實行駛電耗與支出",
                fontSize = 12.sp,
                color = extendedColors.textSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Period Grouping Row Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(extendedColors.subCardBackground)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PeriodGrouping.values().forEach { grouping ->
                    val isSelected = grouping == periodGrouping
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) extendedColors.accentPrimary else Color.Transparent)
                            .clickable { onGroupingChange(grouping) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📅 ${grouping.label}",
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else extendedColors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metric Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(extendedColors.subCardBackground)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ChartMetric.values().forEach { metric ->
                    val isSelected = metric == chartMetric
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) extendedColors.accentPrimary else Color.Transparent)
                            .clickable { onMetricChange(metric) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = metric.label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else extendedColors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Summary Baseline Legend Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mean Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFD97706).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFFD97706).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "● 全期均值: ${CalculationUtils.formatNumber(meanValue, 2)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706)
                    )
                }

                // Best Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF16A34A).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFF16A34A).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "● 最佳期: ${CalculationUtils.formatNumber(bestValue, 2)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }

                // Official Standard Pill
                if (officialValue > 0 && chartMetric != ChartMetric.COST_PER_KM) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(extendedColors.accentPrimary.copy(alpha = 0.12f))
                            .border(1.dp, extendedColors.accentPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "● ${vehicleSettings.ratingStandard}: ${CalculationUtils.formatNumber(officialValue, 2)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = extendedColors.accentPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(dataPoints) {
                            detectTapGestures { offset ->
                                if (dataPoints.isNotEmpty()) {
                                    val leftPadding = 36.dp.toPx()
                                    val rightPadding = 16.dp.toPx()
                                    val chartWidth = size.width - leftPadding - rightPadding
                                    val step = chartWidth / max(1, dataPoints.size - 1)
                                    val relativeX = (offset.x - leftPadding).coerceIn(0f, chartWidth)
                                    val nearestIndex = ((relativeX + step / 2) / step).toInt().coerceIn(0, dataPoints.size - 1)
                                    onPeriodSelect(dataPoints[nearestIndex].first.periodKey)
                                }
                            }
                        }
                ) {
                    val leftPadding = 36.dp.toPx()
                    val rightPadding = 16.dp.toPx()
                    val topPadding = 16.dp.toPx()
                    val bottomPadding = 32.dp.toPx()

                    val chartWidth = size.width - leftPadding - rightPadding
                    val chartHeight = size.height - topPadding - bottomPadding

                    // Determine Y scale
                    val maxValFromData = (values + listOf(meanValue, officialValue, 6.0)).maxOrNull() ?: 7.0
                    val maxY = (maxValFromData * 1.2).coerceAtLeast(6.0)
                    val minY = 0.0

                    // Y Grid Lines and Labels
                    val yTicks = listOf(0.0, maxY * 0.333, maxY * 0.666, maxY)
                    val gridPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#94A3B8")
                        textSize = 28f
                        isAntiAlias = true
                    }

                    for (tick in yTicks) {
                        val yPos = topPadding + chartHeight - ((tick - minY) / (maxY - minY) * chartHeight).toFloat()
                        // Draw grid line
                        drawLine(
                            color = Color(0xFF334155).copy(alpha = 0.5f),
                            start = Offset(leftPadding, yPos),
                            end = Offset(size.width - rightPadding, yPos),
                            strokeWidth = 1.dp.toPx()
                        )
                        // Draw tick text
                        drawContext.canvas.nativeCanvas.drawText(
                            String.format("%.1f", tick),
                            4.dp.toPx(),
                            yPos + 8f,
                            gridPaint
                        )
                    }

                    // Dashed Line: Mean Value
                    if (meanValue > 0) {
                        val meanY = topPadding + chartHeight - ((meanValue - minY) / (maxY - minY) * chartHeight).toFloat()
                        drawLine(
                            color = Color(0xFFF59E0B),
                            start = Offset(leftPadding, meanY),
                            end = Offset(size.width - rightPadding, meanY),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    // Dashed Line: Official Standard
                    if (officialValue > 0 && chartMetric != ChartMetric.COST_PER_KM) {
                        val offY = topPadding + chartHeight - ((officialValue - minY) / (maxY - minY) * chartHeight).toFloat()
                        drawLine(
                            color = Color(0xFF3B82F6),
                            start = Offset(leftPadding, offY),
                            end = Offset(size.width - rightPadding, offY),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    }

                    if (dataPoints.isNotEmpty()) {
                        val stepX = if (dataPoints.size > 1) chartWidth / (dataPoints.size - 1) else chartWidth / 2

                        val points = dataPoints.mapIndexed { idx, pair ->
                            val x = if (dataPoints.size == 1) leftPadding + chartWidth / 2 else leftPadding + idx * stepX
                            val v = pair.second
                            val y = topPadding + chartHeight - ((v - minY) / (maxY - minY) * chartHeight).toFloat()
                            Offset(x, y)
                        }

                        // Gradient Area Path
                        val fillPath = Path()
                        fillPath.moveTo(points.first().x, topPadding + chartHeight)
                        points.forEach { fillPath.lineTo(it.x, it.y) }
                        fillPath.lineTo(points.last().x, topPadding + chartHeight)
                        fillPath.close()

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0xFF38BDF8).copy(alpha = 0.25f),
                                    Color(0xFF2563EB).copy(alpha = 0.02f)
                                ),
                                startY = topPadding,
                                endY = topPadding + chartHeight
                            )
                        )

                        // Line Stroke Path
                        val linePath = Path()
                        linePath.moveTo(points.first().x, points.first().y)
                        points.forEach { linePath.lineTo(it.x, it.y) }

                        drawPath(
                            path = linePath,
                            color = Color(0xFF38BDF8),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Data Points & Highlight
                        points.forEachIndexed { idx, point ->
                            val stat = dataPoints[idx].first
                            val isSelected = stat.periodKey == selectedPeriodKey

                            if (isSelected) {
                                // Vertical highlight line
                                drawLine(
                                    color = Color(0xFFEC4899),
                                    start = Offset(point.x, topPadding),
                                    end = Offset(point.x, topPadding + chartHeight),
                                    strokeWidth = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                )

                                // Outer Pink Glow Ring
                                drawCircle(
                                    color = Color(0xFFEC4899).copy(alpha = 0.35f),
                                    radius = 12.dp.toPx(),
                                    center = point
                                )
                                // Inner Pink Ring
                                drawCircle(
                                    color = Color(0xFFEC4899),
                                    radius = 7.dp.toPx(),
                                    center = point
                                )
                                // White Dot Center
                                drawCircle(
                                    color = Color.White,
                                    radius = 4.dp.toPx(),
                                    center = point
                                )
                            } else {
                                // Normal point
                                drawCircle(
                                    color = Color(0xFF0F172A),
                                    radius = 5.dp.toPx(),
                                    center = point
                                )
                                drawCircle(
                                    color = Color(0xFF38BDF8),
                                    radius = 4.dp.toPx(),
                                    center = point
                                )
                            }

                            // X Axis Labels
                            val xLabelPaint = android.graphics.Paint().apply {
                                color = if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#94A3B8")
                                textSize = if (isSelected) 30f else 24f
                                isFakeBoldText = isSelected
                                isAntiAlias = true
                                textAlign = android.graphics.Paint.Align.CENTER
                            }

                            // Show label on alternate or all depending on count
                            val shouldShowLabel = dataPoints.size <= 10 || idx % 2 == 0 || isSelected || idx == dataPoints.size - 1
                            if (shouldShowLabel) {
                                drawContext.canvas.nativeCanvas.drawText(
                                    stat.displayLabel,
                                    point.x,
                                    size.height - 4.dp.toPx(),
                                    xLabelPaint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
