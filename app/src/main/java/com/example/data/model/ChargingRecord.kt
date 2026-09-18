package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "charging_records")
data class ChargingRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val odometerKm: Double,
    val energyKwh: Double,
    val cost: Double,
    val chargeType: String, // "DC" or "AC"
    val operator: String,   // e.g. "家用充電", "U-POWER", "特爾電力", "iCharging", etc.
    val socPercent: Int,    // 0..100
    val startSocPercent: Int? = null,
    val locationName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val skipEfficiencyCalc: Boolean = false,
    val notes: String = "",
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis()
)
