package com.example.util

import com.example.data.model.ChargingRecord
import java.util.Calendar

object SampleData {

    fun getSampleRecords(): List<ChargingRecord> {
        val cal = Calendar.getInstance()
        // Anchor to 2026/08
        cal.set(2026, Calendar.AUGUST, 30, 16, 56, 0)
        val now = cal.timeInMillis

        return listOf(
            // Baseline starting record (2026/04/10)
            ChargingRecord(
                timestamp = getTime(2026, 4, 10, 10, 0),
                odometerKm = 30000.0,
                energyKwh = 45.0,
                cost = 380.0,
                chargeType = "DC",
                operator = "U-POWER",
                socPercent = 85,
                locationName = "台北內湖旗艦站",
                skipEfficiencyCalc = true,
                notes = "新車交車首筆充電基準紀錄"
            ),
            // 2026/05/02
            ChargingRecord(
                timestamp = getTime(2026, 5, 2, 14, 20),
                odometerKm = 30580.0,
                energyKwh = 48.5,
                cost = 0.0,
                chargeType = "AC",
                operator = "家用充電",
                socPercent = 90,
                locationName = "自宅慢充樁",
                skipEfficiencyCalc = false,
                notes = "夜間離峰慢充，電耗表現平穩"
            ),
            // 2026/05/25
            ChargingRecord(
                timestamp = getTime(2026, 5, 25, 19, 10),
                odometerKm = 31250.0,
                energyKwh = 42.0,
                cost = 250.0,
                chargeType = "DC",
                operator = "EVOASIS",
                socPercent = 80,
                locationName = "台中五權西旗艦站",
                skipEfficiencyCalc = false,
                notes = "國道長途順路補電"
            ),
            // 2026/06/15
            ChargingRecord(
                timestamp = getTime(2026, 6, 15, 11, 45),
                odometerKm = 31920.0,
                energyKwh = 44.8,
                cost = 350.0,
                chargeType = "DC",
                operator = "iCharging",
                socPercent = 85,
                locationName = "新竹巨城站",
                skipEfficiencyCalc = false,
                notes = "商場購物同時快充"
            ),
            // 2026/07/08
            ChargingRecord(
                timestamp = getTime(2026, 7, 8, 16, 30),
                odometerKm = 32890.0,
                energyKwh = 52.0,
                cost = 0.0,
                chargeType = "AC",
                operator = "家用充電",
                socPercent = 95,
                locationName = "自宅慢充樁",
                skipEfficiencyCalc = false,
                notes = "滿電出發自駕環島"
            ),
            // 2026/08/12
            ChargingRecord(
                timestamp = getTime(2026, 8, 12, 13, 15),
                odometerKm = 33420.0,
                energyKwh = 43.2,
                cost = 320.0,
                chargeType = "DC",
                operator = "特爾電力",
                socPercent = 88,
                locationName = "花蓮光復快充站",
                skipEfficiencyCalc = false,
                notes = "東部快充站補電，充電功率達 130kW"
            ),
            // 2026/08/26
            ChargingRecord(
                timestamp = getTime(2026, 8, 26, 18, 41),
                odometerKm = 33949.0,
                energyKwh = 41.01,
                cost = 0.0,
                chargeType = "AC",
                operator = "家用充電",
                socPercent = 100,
                locationName = "文山里 精科七路",
                skipEfficiencyCalc = false,
                notes = "慢充充至 100% 進行電池平衡"
            ),
            // 2026/08/29
            ChargingRecord(
                timestamp = getTime(2026, 8, 29, 11, 48),
                odometerKm = 34117.0,
                energyKwh = 27.98,
                cost = 167.0,
                chargeType = "DC",
                operator = "特爾電力",
                socPercent = 91,
                locationName = "南村里 中山路四段",
                skipEfficiencyCalc = false,
                notes = "市區快速補電，電費 5.97/度"
            )
        )
    }

    private fun getTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, day, hour, minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
