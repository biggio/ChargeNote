package com.example.ui.components

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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VehicleSettings
import com.example.ui.theme.LocalExtendedColors
import com.example.util.CalculationUtils
import com.example.util.OverallMetrics
import com.example.util.PeriodStats
import kotlin.math.roundToInt

@Composable
fun VehicleHeroCard(
    vehicleSettings: VehicleSettings,
    metrics: OverallMetrics,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalExtendedColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(extendedColors.heroBackground)
            .border(1.dp, extendedColors.cardBorder, RoundedCornerShape(28.dp))
            .padding(20.dp)
            .testTag("vehicle_hero_card")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vehicle Icon Box & Name
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(extendedColors.accentPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Vehicle",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = vehicleSettings.vehicleName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = extendedColors.heroText,
                            letterSpacing = (-0.3).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${CalculationUtils.formatNumber(vehicleSettings.usableBatteryKwh, 1)} kWh 電池 · ${CalculationUtils.formatNumber(vehicleSettings.officialRangeKm, 1)} km (${vehicleSettings.ratingStandard})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = extendedColors.heroText.copy(alpha = 0.75f)
                        )
                    }
                }

                // Records badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(extendedColors.heroSubCardBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${metrics.totalRecordsCount} 筆紀錄",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.heroText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-Cards Row inside Hero (Geometric Balance signature 2-col metric cards)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Estimated Full Range Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(extendedColors.heroSubCardBg)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "實測滿電預估續航",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.heroText.copy(alpha = 0.65f),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = CalculationUtils.formatCurrency(metrics.estimatedFullRangeKm),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = extendedColors.heroText
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "km",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = extendedColors.heroText.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                // Range Achievement Rate Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(extendedColors.heroSubCardBg)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "標定達成率",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.heroText.copy(alpha = 0.65f),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = CalculationUtils.formatNumber(metrics.rangeAchievementRate, 1),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = extendedColors.heroText
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = extendedColors.heroText.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Range Progress Bar with sleek geometric edges
            val progress = (metrics.rangeAchievementRate / 100.0).toFloat().coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(extendedColors.heroSubCardBg)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (progress > 0f) progress else 0.01f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(extendedColors.accentPrimary)
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    subValue: String? = null
) {
    val extendedColors = LocalExtendedColors.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(extendedColors.cardBackground)
            .border(1.dp, extendedColors.cardBorder, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = extendedColors.textSecondary,
                    letterSpacing = 0.3.sp
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = extendedColors.textPrimary,
                    letterSpacing = (-0.5).sp
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = extendedColors.textSecondary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            if (subValue != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subValue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = extendedColors.accentSecondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = extendedColors.textMuted
            )
        }
    }
}

@Composable
fun PeriodDepthAnalysisCard(
    periodStats: PeriodStats,
    currencyUnit: String = "TWD",
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalExtendedColors.current

    val diff = periodStats.efficiencyDiffPercent
    val isPositive = diff >= 0
    val badgeText = if (diff < -5.0) {
        "💨 能耗偏高 (${CalculationUtils.formatNumber(diff, 1)}%)"
    } else if (diff > 5.0) {
        "🌱 節能優秀 (+${CalculationUtils.formatNumber(diff, 1)}%)"
    } else {
        "⚖️ 接近均值 (${if (diff >= 0) "+" else ""}${CalculationUtils.formatNumber(diff, 1)}%)"
    }

    val badgeColor = if (diff < -5.0) Color(0xFFDC2626) else if (diff > 5.0) Color(0xFF16A34A) else extendedColors.accentPrimary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(extendedColors.cardBackground)
            .border(1.dp, extendedColors.cardBorder, RoundedCornerShape(24.dp))
            .padding(18.dp)
            .testTag("period_depth_analysis_card")
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(extendedColors.accentPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📊", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${periodStats.periodKey} 能耗深度解析",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.textPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeColor.copy(alpha = 0.12f))
                        .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2x2 Grid stats with Geometric Balance sub-cards
            Row(modifier = Modifier.fillMaxWidth()) {
                // Col 1: Distance
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(extendedColors.subCardBackground)
                        .padding(12.dp)
                ) {
                    Text(text = "該期總行駛里程", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = extendedColors.textSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "+${CalculationUtils.formatNumber(periodStats.totalDistanceKm, 1)} km",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.rangeCyan
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "儀表 ${CalculationUtils.formatCurrency(periodStats.startOdometer)} → ${CalculationUtils.formatCurrency(periodStats.endOdometer)}",
                        fontSize = 10.sp,
                        color = extendedColors.textMuted
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Col 2: Efficiency
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(extendedColors.subCardBackground)
                        .padding(12.dp)
                ) {
                    Text(text = "該期實測電耗", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = extendedColors.textSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${CalculationUtils.formatNumber(periodStats.efficiencyKmPerKwh, 2)} km/kWh",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.accentSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${CalculationUtils.formatNumber(periodStats.consumptionKwhPer100Km, 1)} kWh/100km",
                        fontSize = 10.sp,
                        color = extendedColors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                // Col 3: Energy
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(extendedColors.subCardBackground)
                        .padding(12.dp)
                ) {
                    Text(text = "該期總用電度數", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = extendedColors.textSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${CalculationUtils.formatNumber(periodStats.totalNetEnergyKwh, 1)} kWh",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "充入 ${CalculationUtils.formatNumber(periodStats.totalChargedKwh, 1)}k (DC ${CalculationUtils.formatCurrency(periodStats.dcChargedKwh)}k / AC ${CalculationUtils.formatCurrency(periodStats.acChargedKwh)}k)",
                        fontSize = 10.sp,
                        color = extendedColors.textMuted
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Col 4: Cost
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(extendedColors.subCardBackground)
                        .padding(12.dp)
                ) {
                    Text(text = "該期充電支出", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = extendedColors.textSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$currencyUnit ${CalculationUtils.formatCurrency(periodStats.totalCost)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.costOrange
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$currencyUnit ${CalculationUtils.formatNumber(periodStats.costPerKm, 2)}/km · $currencyUnit ${CalculationUtils.formatNumber(periodStats.costPerKwh, 2)}/kWh",
                        fontSize = 10.sp,
                        color = extendedColors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Charge frequency breakdown
            val dcPercent = if (periodStats.chargeCount > 0) ((periodStats.dcCount.toDouble() / periodStats.chargeCount) * 100).roundToInt() else 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ 該期共充電 ${periodStats.chargeCount} 次 (DC 快充 ${periodStats.dcCount} 次 · AC 慢充 ${periodStats.acCount} 次)",
                    fontSize = 12.sp,
                    color = extendedColors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "DC: $dcPercent%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = extendedColors.dcColor
                )
            }
        }
    }
}
