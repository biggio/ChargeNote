package com.example.util

import com.example.data.model.ChargingRecord
import com.example.data.model.VehicleSettings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class RecordIntervalStats(
    val record: ChargingRecord,
    val deltaKm: Double,
    val netEnergyKwh: Double,
    val efficiencyKmPerKwh: Double,
    val consumptionKwhPer100Km: Double,
    val costPerKwh: Double,
    val costPerKm: Double
)

data class OverallMetrics(
    val totalRecordsCount: Int,
    val totalDistanceKm: Double,
    val totalChargedKwh: Double,
    val totalCost: Double,
    val avgEfficiencyKmPerKwh: Double,
    val avgConsumptionKwhPer100Km: Double,
    val avgCostPerKm: Double,
    val avgCostPerKwh: Double,
    val estimatedFullRangeKm: Double,
    val rangeAchievementRate: Double
)

enum class PeriodGrouping(val label: String) {
    MONTHLY("每月"),
    HALF_YEARLY("每半年"),
    YEARLY("每年")
}

enum class ChartMetric(val label: String, val unit: String) {
    KM_PER_KWH("度電里程", "km/kWh"),
    KWH_PER_100KM("百公里能耗", "kWh/100km"),
    COST_PER_KM("每公里成本", "TWD/km")
}

enum class OperatorMetricTab(val label: String, val unit: String) {
    ENERGY("電量佔比", "kWh"),
    SESSIONS("次數佔比", "次"),
    COST("花費佔比", "TWD")
}

data class OperatorStats(
    val operatorName: String,
    val totalChargedKwh: Double,
    val kwhPercentage: Double,
    val sessionCount: Int,
    val countPercentage: Double,
    val totalCost: Double,
    val costPercentage: Double,
    val avgCostPerKwh: Double,
    val dcCount: Int,
    val acCount: Int
)

data class PeriodStats(
    val periodKey: String, // e.g., "2026/08", "2026 H1", "2026"
    val displayLabel: String, // e.g., "08月", "26 H1", "2026"
    val timestampOrder: Long,
    val startOdometer: Double,
    val endOdometer: Double,
    val totalDistanceKm: Double,
    val totalChargedKwh: Double,
    val dcChargedKwh: Double,
    val acChargedKwh: Double,
    val totalNetEnergyKwh: Double,
    val totalCost: Double,
    val chargeCount: Int,
    val dcCount: Int,
    val acCount: Int,
    val efficiencyKmPerKwh: Double,
    val consumptionKwhPer100Km: Double,
    val costPerKm: Double,
    val costPerKwh: Double,
    val efficiencyDiffPercent: Double // difference relative to overall mean
)

object CalculationUtils {

    fun formatNumber(value: Double, decimals: Int = 1): String {
        return if (value.isNaN() || value.isInfinite()) {
            "0.0"
        } else {
            String.format(Locale.US, "%,.${decimals}f", value)
        }
    }

    fun formatCurrency(value: Double): String {
        return if (value.isNaN() || value.isInfinite()) {
            "0"
        } else {
            String.format(Locale.US, "%,.0f", value)
        }
    }

    fun formatDate(timestamp: Long, pattern: String = "yyyy/MM/dd HH:mm"): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun computeRecordIntervals(
        recordsAsc: List<ChargingRecord>,
        batteryCapacity: Double
    ): Map<Long, RecordIntervalStats> {
        val resultMap = mutableMapOf<Long, RecordIntervalStats>()
        if (recordsAsc.isEmpty()) return resultMap

        for (i in recordsAsc.indices) {
            val curr = recordsAsc[i]
            if (i == 0) {
                // First record baseline
                val costPerKwh = if (curr.energyKwh > 0) curr.cost / curr.energyKwh else 0.0
                resultMap[curr.id] = RecordIntervalStats(
                    record = curr,
                    deltaKm = 0.0,
                    netEnergyKwh = curr.energyKwh,
                    efficiencyKmPerKwh = 0.0,
                    consumptionKwhPer100Km = 0.0,
                    costPerKwh = costPerKwh,
                    costPerKm = 0.0
                )
            } else {
                val prev = recordsAsc[i - 1]
                val deltaKm = (curr.odometerKm - prev.odometerKm).coerceAtLeast(0.0)
                val costPerKwh = if (curr.energyKwh > 0) curr.cost / curr.energyKwh else 0.0
                val costPerKm = if (deltaKm > 0) curr.cost / deltaKm else 0.0

                val deltaSoc = (curr.socPercent - prev.socPercent) / 100.0
                val deltaSocKwh = deltaSoc * batteryCapacity
                val netEnergy = (curr.energyKwh - deltaSocKwh).coerceAtLeast(0.1)

                val efficiency = if (!curr.skipEfficiencyCalc && deltaKm > 0 && netEnergy > 0) {
                    deltaKm / netEnergy
                } else if (deltaKm > 0 && curr.energyKwh > 0) {
                    deltaKm / curr.energyKwh
                } else {
                    0.0
                }

                val consumption = if (efficiency > 0) (100.0 / efficiency) else 0.0

                resultMap[curr.id] = RecordIntervalStats(
                    record = curr,
                    deltaKm = deltaKm,
                    netEnergyKwh = netEnergy,
                    efficiencyKmPerKwh = efficiency,
                    consumptionKwhPer100Km = consumption,
                    costPerKwh = costPerKwh,
                    costPerKm = costPerKm
                )
            }
        }
        return resultMap
    }

    fun computeOverallMetrics(
        records: List<ChargingRecord>,
        vehicleSettings: VehicleSettings
    ): OverallMetrics {
        if (records.isEmpty()) {
            return OverallMetrics(
                totalRecordsCount = 0,
                totalDistanceKm = 0.0,
                totalChargedKwh = 0.0,
                totalCost = 0.0,
                avgEfficiencyKmPerKwh = 0.0,
                avgConsumptionKwhPer100Km = 0.0,
                avgCostPerKm = 0.0,
                avgCostPerKwh = 0.0,
                estimatedFullRangeKm = 0.0,
                rangeAchievementRate = 0.0
            )
        }

        val sortedAsc = records.sortedWith(compareBy({ it.timestamp }, { it.odometerKm }))
        val intervals = computeRecordIntervals(sortedAsc, vehicleSettings.usableBatteryKwh)

        val totalRecordsCount = sortedAsc.size
        val minOdo = sortedAsc.first().odometerKm
        val maxOdo = sortedAsc.last().odometerKm
        val totalDistanceKm = (maxOdo - minOdo).coerceAtLeast(0.0)

        val totalChargedKwh = sortedAsc.sumOf { it.energyKwh }
        val totalCost = sortedAsc.sumOf { it.cost }

        var validDistance = 0.0
        var validNetEnergy = 0.0

        intervals.values.forEach { stat ->
            if (!stat.record.skipEfficiencyCalc && stat.deltaKm > 0 && stat.netEnergyKwh > 0) {
                validDistance += stat.deltaKm
                validNetEnergy += stat.netEnergyKwh
            }
        }

        val avgEfficiency = if (validDistance > 0 && validNetEnergy > 0) {
            validDistance / validNetEnergy
        } else if (totalDistanceKm > 0 && totalChargedKwh > 0) {
            totalDistanceKm / totalChargedKwh
        } else {
            0.0
        }

        val avgConsumption = if (avgEfficiency > 0) (100.0 / avgEfficiency) else 0.0
        val avgCostPerKm = if (totalDistanceKm > 0) totalCost / totalDistanceKm else 0.0
        val avgCostPerKwh = if (totalChargedKwh > 0) totalCost / totalChargedKwh else 0.0

        val estimatedFullRangeKm = vehicleSettings.usableBatteryKwh * avgEfficiency
        val rangeAchievementRate = if (vehicleSettings.officialRangeKm > 0) {
            (estimatedFullRangeKm / vehicleSettings.officialRangeKm) * 100.0
        } else {
            0.0
        }

        return OverallMetrics(
            totalRecordsCount = totalRecordsCount,
            totalDistanceKm = totalDistanceKm,
            totalChargedKwh = totalChargedKwh,
            totalCost = totalCost,
            avgEfficiencyKmPerKwh = avgEfficiency,
            avgConsumptionKwhPer100Km = avgConsumption,
            avgCostPerKm = avgCostPerKm,
            avgCostPerKwh = avgCostPerKwh,
            estimatedFullRangeKm = estimatedFullRangeKm,
            rangeAchievementRate = rangeAchievementRate
        )
    }

    fun computePeriodStats(
        records: List<ChargingRecord>,
        grouping: PeriodGrouping,
        vehicleSettings: VehicleSettings,
        overallAvgEfficiency: Double
    ): List<PeriodStats> {
        if (records.isEmpty()) return emptyList()

        val sortedAsc = records.sortedWith(compareBy({ it.timestamp }, { it.odometerKm }))
        val intervals = computeRecordIntervals(sortedAsc, vehicleSettings.usableBatteryKwh)

        val cal = Calendar.getInstance()
        val groups = linkedMapOf<String, MutableList<Pair<ChargingRecord, RecordIntervalStats>>>()

        for (record in sortedAsc) {
            cal.timeInMillis = record.timestamp
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1 // 1..12
            val key = when (grouping) {
                PeriodGrouping.MONTHLY -> String.format(Locale.US, "%04d/%02d", year, month)
                PeriodGrouping.HALF_YEARLY -> if (month <= 6) "$year H1" else "$year H2"
                PeriodGrouping.YEARLY -> "$year"
            }
            val interval = intervals[record.id] ?: RecordIntervalStats(
                record = record,
                deltaKm = 0.0,
                netEnergyKwh = record.energyKwh,
                efficiencyKmPerKwh = 0.0,
                consumptionKwhPer100Km = 0.0,
                costPerKwh = 0.0,
                costPerKm = 0.0
            )
            groups.getOrPut(key) { mutableListOf() }.add(Pair(record, interval))
        }

        val result = mutableListOf<PeriodStats>()

        groups.forEach { (periodKey, list) ->
            val firstRecord = list.first().first
            val lastRecord = list.last().first

            val startOdo = firstRecord.odometerKm
            val endOdo = lastRecord.odometerKm
            val deltaOdo = (endOdo - startOdo).coerceAtLeast(0.0)

            val totalCharged = list.sumOf { it.first.energyKwh }
            val dcCharged = list.filter { it.first.chargeType.equals("DC", ignoreCase = true) }.sumOf { it.first.energyKwh }
            val acCharged = list.filter { !it.first.chargeType.equals("DC", ignoreCase = true) }.sumOf { it.first.energyKwh }

            val totalCost = list.sumOf { it.first.cost }
            val totalCount = list.size
            val dcCount = list.count { it.first.chargeType.equals("DC", ignoreCase = true) }
            val acCount = list.count { !it.first.chargeType.equals("DC", ignoreCase = true) }

            var validDistance = 0.0
            var validNetEnergy = 0.0

            list.forEach { (_, stat) ->
                if (!stat.record.skipEfficiencyCalc && stat.deltaKm > 0 && stat.netEnergyKwh > 0) {
                    validDistance += stat.deltaKm
                    validNetEnergy += stat.netEnergyKwh
                }
            }

            val distance = if (validDistance > 0) validDistance else deltaOdo
            val netEnergy = if (validNetEnergy > 0) validNetEnergy else totalCharged

            val efficiency = if (distance > 0 && netEnergy > 0) {
                distance / netEnergy
            } else {
                0.0
            }

            val consumption = if (efficiency > 0) 100.0 / efficiency else 0.0
            val costPerKm = if (distance > 0) totalCost / distance else 0.0
            val costPerKwh = if (totalCharged > 0) totalCost / totalCharged else 0.0

            val diffPercent = if (overallAvgEfficiency > 0 && efficiency > 0) {
                ((efficiency - overallAvgEfficiency) / overallAvgEfficiency) * 100.0
            } else {
                0.0
            }

            val displayLabel = when (grouping) {
                PeriodGrouping.MONTHLY -> {
                    val parts = periodKey.split("/")
                    if (parts.size == 2) "${parts[1]}月" else periodKey
                }
                PeriodGrouping.HALF_YEARLY -> periodKey
                PeriodGrouping.YEARLY -> "${periodKey}年"
            }

            result.add(
                PeriodStats(
                    periodKey = periodKey,
                    displayLabel = displayLabel,
                    timestampOrder = firstRecord.timestamp,
                    startOdometer = startOdo,
                    endOdometer = endOdo,
                    totalDistanceKm = distance,
                    totalChargedKwh = totalCharged,
                    dcChargedKwh = dcCharged,
                    acChargedKwh = acCharged,
                    totalNetEnergyKwh = netEnergy,
                    totalCost = totalCost,
                    chargeCount = totalCount,
                    dcCount = dcCount,
                    acCount = acCount,
                    efficiencyKmPerKwh = efficiency,
                    consumptionKwhPer100Km = consumption,
                    costPerKm = costPerKm,
                    costPerKwh = costPerKwh,
                    efficiencyDiffPercent = diffPercent
                )
            )
        }

        return result.sortedBy { it.timestampOrder }
    }

    fun computeOperatorStats(records: List<ChargingRecord>): List<OperatorStats> {
        if (records.isEmpty()) return emptyList()

        val totalChargedAll = records.sumOf { it.energyKwh }
        val totalCountAll = records.size
        val totalCostAll = records.sumOf { it.cost }

        val groups = records.groupBy {
            val op = it.operator.trim()
            if (op.isEmpty()) "其他 / 未標註" else op
        }

        return groups.map { (opName, recs) ->
            val kwh = recs.sumOf { it.energyKwh }
            val count = recs.size
            val cost = recs.sumOf { it.cost }
            val dcCount = recs.count { it.chargeType.equals("DC", ignoreCase = true) }
            val acCount = recs.count { !it.chargeType.equals("DC", ignoreCase = true) }

            val kwhPct = if (totalChargedAll > 0) (kwh / totalChargedAll) * 100.0 else 0.0
            val countPct = if (totalCountAll > 0) (count.toDouble() / totalCountAll) * 100.0 else 0.0
            val costPct = if (totalCostAll > 0) (cost / totalCostAll) * 100.0 else 0.0
            val avgCost = if (kwh > 0) cost / kwh else 0.0

            OperatorStats(
                operatorName = opName,
                totalChargedKwh = kwh,
                kwhPercentage = kwhPct,
                sessionCount = count,
                countPercentage = countPct,
                totalCost = cost,
                costPercentage = costPct,
                avgCostPerKwh = avgCost,
                dcCount = dcCount,
                acCount = acCount
            )
        }.sortedByDescending { it.totalChargedKwh }
    }
}
