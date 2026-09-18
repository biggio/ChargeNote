package com.example.data.repository

import com.example.data.db.ChargingRecordDao
import com.example.data.model.ChargingRecord
import kotlinx.coroutines.flow.Flow

class ChargingRepository(private val dao: ChargingRecordDao) {
    val allRecords: Flow<List<ChargingRecord>> = dao.getAllRecordsFlow()
    val allRecordsAsc: Flow<List<ChargingRecord>> = dao.getAllRecordsAscFlow()

    suspend fun getAllRecords(): List<ChargingRecord> = dao.getAllRecords()

    suspend fun getRecordById(id: Long): ChargingRecord? = dao.getRecordById(id)

    suspend fun insert(record: ChargingRecord): Long = dao.insertRecord(record)

    suspend fun insertAll(records: List<ChargingRecord>) = dao.insertAll(records)

    suspend fun update(record: ChargingRecord) = dao.updateRecord(record)

    suspend fun delete(record: ChargingRecord) = dao.deleteRecord(record)

    suspend fun deleteById(id: Long) = dao.deleteRecordById(id)

    suspend fun deleteAll() = dao.deleteAllRecords()

    suspend fun getBySyncId(syncId: String): ChargingRecord? = dao.getBySyncId(syncId)

    suspend fun getUpdatedSince(since: Long): List<ChargingRecord> = dao.getUpdatedSince(since)

    suspend fun deleteBySyncId(syncId: String) = dao.deleteBySyncId(syncId)
}
