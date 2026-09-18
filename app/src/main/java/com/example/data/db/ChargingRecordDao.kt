package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ChargingRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ChargingRecordDao {
    @Query("SELECT * FROM charging_records ORDER BY timestamp DESC, odometerKm DESC")
    fun getAllRecordsFlow(): Flow<List<ChargingRecord>>

    @Query("SELECT * FROM charging_records ORDER BY timestamp ASC, odometerKm ASC")
    fun getAllRecordsAscFlow(): Flow<List<ChargingRecord>>

    @Query("SELECT * FROM charging_records ORDER BY timestamp DESC, odometerKm DESC")
    suspend fun getAllRecords(): List<ChargingRecord>

    @Query("SELECT * FROM charging_records WHERE id = :id")
    suspend fun getRecordById(id: Long): ChargingRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ChargingRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<ChargingRecord>)

    @Update
    suspend fun updateRecord(record: ChargingRecord)

    @Delete
    suspend fun deleteRecord(record: ChargingRecord)

    @Query("DELETE FROM charging_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM charging_records")
    suspend fun deleteAllRecords()

    @Query("SELECT * FROM charging_records WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): ChargingRecord?

    @Query("SELECT * FROM charging_records WHERE updatedAt > :since ORDER BY updatedAt ASC")
    suspend fun getUpdatedSince(since: Long): List<ChargingRecord>

    @Query("DELETE FROM charging_records WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)
}
