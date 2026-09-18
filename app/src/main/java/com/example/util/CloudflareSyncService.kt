package com.example.util

import com.example.data.model.ChargingRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SyncResult(
    val success: Boolean,
    val uploadedCount: Int,
    val downloadedRecords: List<ChargingRecord>,
    val serverTimestamp: Long,
    val message: String
)

object CloudflareSyncService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private fun normalizeUrl(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://$clean"
        }
        return clean.trimEnd('/')
    }

    suspend fun testConnection(workerUrl: String, syncSecret: String): Result<String> = withContext(Dispatchers.IO) {
        val base = normalizeUrl(workerUrl)
        if (base.isBlank() || base == "https://") {
            return@withContext Result.failure(IllegalArgumentException("請輸入有效的 Cloudflare Worker 網址"))
        }

        try {
            val reqBuilder = Request.Builder()
                .url("$base/api/health")
                .get()

            if (syncSecret.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer ${syncSecret.trim()}")
            }

            val response = client.newCall(reqBuilder.build()).execute()
            val code = response.code
            val body = response.body?.string() ?: ""

            if (code == 200) {
                Result.success("連線成功！Cloudflare Worker 與 D1 服務正常運作中。")
            } else if (code == 401) {
                Result.failure(Exception("驗證失敗 (HTTP 401)：同步金鑰（Sync Secret）不相符"))
            } else {
                Result.failure(Exception("連線異常 (HTTP $code)：$body"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("無法連線至 Worker：${e.localizedMessage ?: "請檢查網路或網址是否正確"}"))
        }
    }

    suspend fun syncRecords(
        workerUrl: String,
        syncSecret: String,
        localRecords: List<ChargingRecord>,
        lastSyncTime: Long
    ): Result<SyncResult> = withContext(Dispatchers.IO) {
        val base = normalizeUrl(workerUrl)
        if (base.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("尚未設定 Cloudflare Worker 網址"))
        }

        try {
            // Build payload
            val root = JSONObject()
            root.put("since", lastSyncTime)

            val recordsArray = JSONArray()
            for (r in localRecords) {
                val obj = JSONObject()
                obj.put("sync_id", r.syncId)
                obj.put("timestamp", r.timestamp)
                obj.put("odometer_km", r.odometerKm)
                obj.put("energy_kwh", r.energyKwh)
                obj.put("cost", r.cost)
                obj.put("charge_type", r.chargeType)
                obj.put("operator", r.operator)
                obj.put("soc_percent", r.socPercent)
                if (r.startSocPercent != null) obj.put("start_soc_percent", r.startSocPercent)
                else obj.put("start_soc_percent", JSONObject.NULL)
                obj.put("location_name", r.locationName)
                if (r.latitude != null) obj.put("latitude", r.latitude)
                else obj.put("latitude", JSONObject.NULL)
                if (r.longitude != null) obj.put("longitude", r.longitude)
                else obj.put("longitude", JSONObject.NULL)
                obj.put("skip_efficiency_calc", if (r.skipEfficiencyCalc) 1 else 0)
                obj.put("notes", r.notes)
                obj.put("updated_at", r.updatedAt)
                recordsArray.put(obj)
            }
            root.put("records", recordsArray)

            val reqBody = root.toString().toRequestBody(JSON_MEDIA_TYPE)
            val reqBuilder = Request.Builder()
                .url("$base/api/sync")
                .post(reqBody)

            if (syncSecret.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer ${syncSecret.trim()}")
            }

            val response = client.newCall(reqBuilder.build()).execute()
            val code = response.code
            val respBodyStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                if (code == 401) {
                    return@withContext Result.failure(Exception("同步失敗 (HTTP 401)：同步金鑰（Sync Secret）不正確"))
                }
                return@withContext Result.failure(Exception("伺服器錯誤 (HTTP $code)：$respBodyStr"))
            }

            val respJson = JSONObject(respBodyStr)
            val serverTimestamp = respJson.optLong("serverTimestamp", System.currentTimeMillis())
            val serverRecordsJson = respJson.optJSONArray("serverRecords") ?: JSONArray()
            val downloadedList = mutableListOf<ChargingRecord>()

            for (i in 0 until serverRecordsJson.length()) {
                val item = serverRecordsJson.getJSONObject(i)
                val sId = item.optString("sync_id", "")
                if (sId.isNotBlank()) {
                    downloadedList.add(
                        ChargingRecord(
                            id = 0, // Auto-generated or matched locally
                            syncId = sId,
                            timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                            odometerKm = item.optDouble("odometer_km", 0.0),
                            energyKwh = item.optDouble("energy_kwh", 0.0),
                            cost = item.optDouble("cost", 0.0),
                            chargeType = item.optString("charge_type", "DC"),
                            operator = item.optString("operator", "其他"),
                            socPercent = item.optInt("soc_percent", 80),
                            startSocPercent = if (item.has("start_soc_percent") && !item.isNull("start_soc_percent")) item.getInt("start_soc_percent") else null,
                            locationName = item.optString("location_name", ""),
                            latitude = if (item.has("latitude") && !item.isNull("latitude")) item.getDouble("latitude") else null,
                            longitude = if (item.has("longitude") && !item.isNull("longitude")) item.getDouble("longitude") else null,
                            skipEfficiencyCalc = item.optInt("skip_efficiency_calc", 0) == 1,
                            notes = item.optString("notes", ""),
                            updatedAt = item.optLong("updated_at", System.currentTimeMillis())
                        )
                    )
                }
            }

            Result.success(
                SyncResult(
                    success = true,
                    uploadedCount = localRecords.size,
                    downloadedRecords = downloadedList,
                    serverTimestamp = serverTimestamp,
                    message = "同步完成！上傳 ${localRecords.size} 筆，從雲端同步 ${downloadedList.size} 筆變更。"
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("同步過程發生錯誤：${e.localizedMessage ?: "網路逾時"}"))
        }
    }

    suspend fun restoreFromCloud(workerUrl: String, syncSecret: String): Result<List<ChargingRecord>> = withContext(Dispatchers.IO) {
        val base = normalizeUrl(workerUrl)
        if (base.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("尚未設定 Cloudflare Worker 網址"))
        }

        try {
            val reqBuilder = Request.Builder()
                .url("$base/api/backup")
                .get()

            if (syncSecret.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer ${syncSecret.trim()}")
            }

            val response = client.newCall(reqBuilder.build()).execute()
            val code = response.code
            val respBodyStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                if (code == 401) {
                    return@withContext Result.failure(Exception("還原失敗 (HTTP 401)：同步金鑰（Sync Secret）不正確"))
                }
                return@withContext Result.failure(Exception("伺服器錯誤 (HTTP $code)：$respBodyStr"))
            }

            val respJson = JSONObject(respBodyStr)
            val recordsJson = respJson.optJSONArray("records") ?: JSONArray()
            val resultList = mutableListOf<ChargingRecord>()

            for (i in 0 until recordsJson.length()) {
                val item = recordsJson.getJSONObject(i)
                val sId = item.optString("sync_id", java.util.UUID.randomUUID().toString())
                resultList.add(
                    ChargingRecord(
                        id = 0,
                        syncId = sId,
                        timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                        odometerKm = item.optDouble("odometer_km", 0.0),
                        energyKwh = item.optDouble("energy_kwh", 0.0),
                        cost = item.optDouble("cost", 0.0),
                        chargeType = item.optString("charge_type", "DC"),
                        operator = item.optString("operator", "其他"),
                        socPercent = item.optInt("soc_percent", 80),
                        startSocPercent = if (item.has("start_soc_percent") && !item.isNull("start_soc_percent")) item.getInt("start_soc_percent") else null,
                        locationName = item.optString("location_name", ""),
                        latitude = if (item.has("latitude") && !item.isNull("latitude")) item.getDouble("latitude") else null,
                        longitude = if (item.has("longitude") && !item.isNull("longitude")) item.getDouble("longitude") else null,
                        skipEfficiencyCalc = item.optInt("skip_efficiency_calc", 0) == 1,
                        notes = item.optString("notes", ""),
                        updatedAt = item.optLong("updated_at", System.currentTimeMillis())
                    )
                )
            }

            Result.success(resultList)
        } catch (e: Exception) {
            Result.failure(Exception("從雲端還原失敗：${e.localizedMessage ?: "網路連線異常"}"))
        }
    }
}
