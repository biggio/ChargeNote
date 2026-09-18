package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AppThemeConfig
import com.example.data.model.ChargingRecord
import com.example.data.model.VehicleSettings
import com.example.data.repository.ChargingRepository
import com.example.data.repository.SettingsRepository
import com.example.util.CalculationUtils
import com.example.util.ChartMetric
import com.example.util.OperatorMetricTab
import com.example.util.OperatorStats
import com.example.util.OverallMetrics
import com.example.util.PeriodGrouping
import com.example.util.PeriodStats
import com.example.util.RecordIntervalStats
import com.example.util.SampleData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.util.CloudflareSyncService
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EVViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val chargingRepo = ChargingRepository(database.chargingRecordDao())
    private val settingsRepo = SettingsRepository(application)

    val vehicleSettings: StateFlow<VehicleSettings> = settingsRepo.vehicleSettings
    val themeConfig: StateFlow<AppThemeConfig> = settingsRepo.themeConfig
    val customGeminiApiKey: StateFlow<String> = settingsRepo.customGeminiApiKey
    val customOperators: StateFlow<List<String>> = settingsRepo.customOperators

    val cloudflareWorkerUrl: StateFlow<String> = settingsRepo.cloudflareWorkerUrl
    val cloudflareSyncSecret: StateFlow<String> = settingsRepo.cloudflareSyncSecret
    val cloudflareAutoSync: StateFlow<Boolean> = settingsRepo.cloudflareAutoSync
    val cloudflareLastSyncTime: StateFlow<Long> = settingsRepo.cloudflareLastSyncTime

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    val records: StateFlow<List<ChargingRecord>> = chargingRepo.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recordsAsc: StateFlow<List<ChargingRecord>> = chargingRepo.allRecordsAsc
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _periodGrouping = MutableStateFlow(PeriodGrouping.MONTHLY)
    val periodGrouping: StateFlow<PeriodGrouping> = _periodGrouping.asStateFlow()

    private val _chartMetric = MutableStateFlow(ChartMetric.KM_PER_KWH)
    val chartMetric: StateFlow<ChartMetric> = _chartMetric.asStateFlow()

    private val _selectedPeriodKey = MutableStateFlow<String?>(null)
    val selectedPeriodKey: StateFlow<String?> = _selectedPeriodKey.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _historyFilter = MutableStateFlow("ALL") // "ALL", "DC", "AC", "HOME"
    val historyFilter: StateFlow<String> = _historyFilter.asStateFlow()

    private val _operatorMetricTab = MutableStateFlow(OperatorMetricTab.ENERGY)
    val operatorMetricTab: StateFlow<OperatorMetricTab> = _operatorMetricTab.asStateFlow()

    // Derived Interval Map
    val recordIntervalMap: StateFlow<Map<Long, RecordIntervalStats>> = combine(
        recordsAsc,
        vehicleSettings
    ) { recs, settings ->
        CalculationUtils.computeRecordIntervals(recs, settings.usableBatteryKwh)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Overall Metrics
    val overallMetrics: StateFlow<OverallMetrics> = combine(
        records,
        vehicleSettings
    ) { recs, settings ->
        CalculationUtils.computeOverallMetrics(recs, settings)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        OverallMetrics(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    )

    // Operator Breakdown Stats
    val operatorStatsList: StateFlow<List<OperatorStats>> = records.map { recs ->
        CalculationUtils.computeOperatorStats(recs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Period Stats List
    val periodStatsList: StateFlow<List<PeriodStats>> = combine(
        records,
        _periodGrouping,
        vehicleSettings,
        overallMetrics
    ) { recs, grouping, settings, overall ->
        val list = CalculationUtils.computePeriodStats(recs, grouping, settings, overall.avgEfficiencyKmPerKwh)
        if (_selectedPeriodKey.value == null && list.isNotEmpty()) {
            _selectedPeriodKey.value = list.last().periodKey
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedPeriodStats: StateFlow<PeriodStats?> = combine(
        periodStatsList,
        _selectedPeriodKey
    ) { list, key ->
        if (key != null) list.find { it.periodKey == key } ?: list.lastOrNull()
        else list.lastOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Check if database is empty on first launch, load initial sample data for rich experience
        viewModelScope.launch {
            val existing = chargingRepo.getAllRecords()
            if (existing.isEmpty()) {
                chargingRepo.insertAll(SampleData.getSampleRecords())
            }
        }
    }

    fun setPeriodGrouping(grouping: PeriodGrouping) {
        _periodGrouping.value = grouping
        _selectedPeriodKey.value = null
    }

    fun setChartMetric(metric: ChartMetric) {
        _chartMetric.value = metric
    }

    fun setOperatorMetricTab(tab: OperatorMetricTab) {
        _operatorMetricTab.value = tab
    }

    fun selectPeriod(key: String) {
        _selectedPeriodKey.value = key
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setHistoryFilter(filter: String) {
        _historyFilter.value = filter
    }

    fun insertRecord(record: ChargingRecord) {
        viewModelScope.launch {
            val toSave = if (record.updatedAt == 0L) record.copy(updatedAt = System.currentTimeMillis()) else record
            chargingRepo.insert(toSave)
            triggerAutoSyncIfEnabled()
        }
    }

    fun updateRecord(record: ChargingRecord) {
        viewModelScope.launch {
            val toSave = record.copy(updatedAt = System.currentTimeMillis())
            chargingRepo.update(toSave)
            triggerAutoSyncIfEnabled()
        }
    }

    fun deleteRecord(record: ChargingRecord) {
        viewModelScope.launch {
            chargingRepo.delete(record)
        }
    }

    fun deleteAllRecords() {
        viewModelScope.launch {
            chargingRepo.deleteAll()
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            chargingRepo.deleteAll()
            chargingRepo.insertAll(SampleData.getSampleRecords())
        }
    }

    fun saveVehicleSettings(settings: VehicleSettings) {
        settingsRepo.saveVehicleSettings(settings)
    }

    fun saveThemeConfig(config: AppThemeConfig) {
        settingsRepo.saveThemeConfig(config)
    }

    fun saveCustomGeminiApiKey(key: String) {
        settingsRepo.saveCustomGeminiApiKey(key)
    }

    fun getEffectiveApiKey(): String {
        return settingsRepo.getEffectiveApiKey()
    }

    suspend fun recognizeChargingImage(bitmap: android.graphics.Bitmap): Result<com.example.util.RecognizedChargingInfo> {
        val apiKey = settingsRepo.getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return Result.failure(Exception("尚未設定 Gemini API Key！請先至「設定」頁面填入個人 Google AI Studio API Key。"))
        }
        return com.example.util.GeminiRecognitionService.recognizeImage(bitmap, apiKey)
    }

    fun addOperator(name: String): Boolean = settingsRepo.addOperator(name)

    fun removeOperator(name: String) = settingsRepo.removeOperator(name)

    fun resetOperators() = settingsRepo.resetOperators()

    fun saveCloudflareConfig(url: String, secret: String, autoSync: Boolean) {
        settingsRepo.saveCloudflareConfig(url, secret, autoSync)
    }

    suspend fun testCloudflareConnection(
        url: String = cloudflareWorkerUrl.value,
        secret: String = cloudflareSyncSecret.value
    ): Pair<Boolean, String> {
        val res = CloudflareSyncService.testConnection(url, secret)
        return if (res.isSuccess) {
            Pair(true, res.getOrNull() ?: "連線成功！Cloudflare Worker 與 D1 服務正常運作中。")
        } else {
            Pair(false, res.exceptionOrNull()?.localizedMessage ?: "連線失敗")
        }
    }

    suspend fun performCloudSync(): Pair<Boolean, String> {
        val url = cloudflareWorkerUrl.value
        val secret = cloudflareSyncSecret.value
        if (url.isBlank()) {
            return Pair(false, "尚未設定 Cloudflare Worker 網址，請先於下方填寫")
        }

        _isCloudSyncing.value = true
        return try {
            val localRecords = chargingRepo.getAllRecords()
            val lastSync = cloudflareLastSyncTime.value
            val res = CloudflareSyncService.syncRecords(url, secret, localRecords, lastSync)

            if (res.isSuccess) {
                val syncResult = res.getOrThrow()
                // Merge downloaded server records into local database
                for (srv in syncResult.downloadedRecords) {
                    val local = chargingRepo.getBySyncId(srv.syncId)
                    if (local != null) {
                        if (srv.updatedAt > local.updatedAt) {
                            chargingRepo.update(srv.copy(id = local.id))
                        }
                    } else {
                        chargingRepo.insert(srv.copy(id = 0))
                    }
                }
                settingsRepo.updateLastSyncTime(syncResult.serverTimestamp)
                Pair(true, syncResult.message)
            } else {
                Pair(false, res.exceptionOrNull()?.localizedMessage ?: "同步失敗")
            }
        } catch (e: Exception) {
            Pair(false, "同步發生異常：${e.localizedMessage ?: "未知錯誤"}")
        } finally {
            _isCloudSyncing.value = false
        }
    }

    suspend fun restoreFromCloudflare(): Pair<Boolean, String> {
        val url = cloudflareWorkerUrl.value
        val secret = cloudflareSyncSecret.value
        if (url.isBlank()) {
            return Pair(false, "尚未設定 Cloudflare Worker 網址")
        }

        _isCloudSyncing.value = true
        return try {
            val res = CloudflareSyncService.restoreFromCloud(url, secret)
            if (res.isSuccess) {
                val cloudRecords = res.getOrThrow()
                if (cloudRecords.isEmpty()) {
                    Pair(false, "Cloudflare D1 雲端資料庫目前為空，沒有可還原的紀錄")
                } else {
                    chargingRepo.deleteAll()
                    chargingRepo.insertAll(cloudRecords)
                    settingsRepo.updateLastSyncTime(System.currentTimeMillis())
                    Pair(true, "成功從 Cloudflare D1 還原 ${cloudRecords.size} 筆紀錄！")
                }
            } else {
                Pair(false, res.exceptionOrNull()?.localizedMessage ?: "雲端還原失敗")
            }
        } catch (e: Exception) {
            Pair(false, "還原發生異常：${e.localizedMessage ?: "未知錯誤"}")
        } finally {
            _isCloudSyncing.value = false
        }
    }

    fun triggerAutoSyncIfEnabled() {
        if (cloudflareAutoSync.value && cloudflareWorkerUrl.value.isNotBlank() && !_isCloudSyncing.value) {
            viewModelScope.launch {
                performCloudSync()
            }
        }
    }

    fun exportJson(): String {
        val currentRecords = recordsAsc.value
        val jsonArray = JSONArray()
        for (r in currentRecords) {
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("timestamp", r.timestamp)
            obj.put("dateTime", CalculationUtils.formatDate(r.timestamp))
            obj.put("totalOdometer", r.odometerKm)
            obj.put("odometerKm", r.odometerKm)
            obj.put("chargedKwh", r.energyKwh)
            obj.put("energyKwh", r.energyKwh)
            obj.put("amount", r.cost)
            obj.put("cost", r.cost)
            obj.put("chargingType", r.chargeType)
            obj.put("chargeType", r.chargeType)
            obj.put("operator", r.operator)
            obj.put("startSoc", r.startSocPercent ?: JSONObject.NULL)
            obj.put("startSocPercent", r.startSocPercent ?: JSONObject.NULL)
            obj.put("endSoc", r.socPercent)
            obj.put("socPercent", r.socPercent)
            obj.put("locationAddress", r.locationName)
            obj.put("locationName", r.locationName)
            obj.put("latitude", r.latitude ?: JSONObject.NULL)
            obj.put("longitude", r.longitude ?: JSONObject.NULL)
            obj.put("skipIntervalCalculation", r.skipEfficiencyCalc)
            obj.put("skipEfficiencyCalc", r.skipEfficiencyCalc)
            obj.put("notes", r.notes)
            obj.put("syncId", r.syncId)
            obj.put("updatedAt", r.updatedAt)
            jsonArray.put(obj)
        }
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("vehicleSettings", JSONObject().apply {
            put("vehicleName", vehicleSettings.value.vehicleName)
            put("batteryCapacityKwh", vehicleSettings.value.usableBatteryKwh)
            put("usableBatteryKwh", vehicleSettings.value.usableBatteryKwh)
            put("officialRangeKm", vehicleSettings.value.officialRangeKm)
            put("rangeStandard", vehicleSettings.value.ratingStandard)
            put("distanceUnit", vehicleSettings.value.distanceUnit)
            put("currency", vehicleSettings.value.currencyUnit)
        })
        root.put("vehicle", JSONObject().apply {
            put("name", vehicleSettings.value.vehicleName)
            put("usableBatteryKwh", vehicleSettings.value.usableBatteryKwh)
            put("officialRangeKm", vehicleSettings.value.officialRangeKm)
            put("ratingStandard", vehicleSettings.value.ratingStandard)
        })
        root.put("records", jsonArray)
        return root.toString(2)
    }

    fun exportCsv(): String {
        val currentRecords = recordsAsc.value
        val sb = StringBuilder()
        sb.append("ID,時間,儀表總里程(km),充入度數(kWh),費用(TWD),充電類型,充電營運商,充飽電量(SoC %),充電前電量(SoC %),地點備註,略過計算,備忘備註\n")
        for (r in currentRecords) {
            sb.append("${r.id},")
            sb.append("\"${CalculationUtils.formatDate(r.timestamp)}\",")
            sb.append("${r.odometerKm},")
            sb.append("${r.energyKwh},")
            sb.append("${r.cost},")
            sb.append("${r.chargeType},")
            sb.append("\"${r.operator}\",")
            sb.append("${r.socPercent},")
            sb.append("${r.startSocPercent ?: ""},")
            sb.append("\"${r.locationName.replace("\"", "\"\"")}\",")
            sb.append("${r.skipEfficiencyCalc},")
            sb.append("\"${r.notes.replace("\"", "\"\"")}\"\n")
        }
        return sb.toString()
    }

    fun parseJsonPreview(jsonStr: String): Pair<Boolean, String> {
        return try {
            val root = if (jsonStr.trim().startsWith("[")) {
                JSONObject().put("records", JSONArray(jsonStr))
            } else {
                JSONObject(jsonStr)
            }
            val recordsArray = if (root.has("records")) root.getJSONArray("records") else JSONArray()
            var vehicleInfo = ""
            if (root.has("vehicleSettings")) {
                val v = root.getJSONObject("vehicleSettings")
                val vName = v.optString("vehicleName", "")
                val cap = v.optDouble("batteryCapacityKwh", v.optDouble("usableBatteryKwh", 0.0))
                if (vName.isNotEmpty()) vehicleInfo = "車輛：$vName (${cap} kWh) · "
            } else if (root.has("vehicle")) {
                val v = root.getJSONObject("vehicle")
                val vName = v.optString("name", "")
                val cap = v.optDouble("usableBatteryKwh", 0.0)
                if (vName.isNotEmpty()) vehicleInfo = "車輛：$vName (${cap} kWh) · "
            }
            Pair(true, "${vehicleInfo}共 ${recordsArray.length()} 筆充電紀錄")
        } catch (e: Exception) {
            Pair(false, "無效的 JSON 格式：${e.localizedMessage}")
        }
    }

    fun importJson(jsonStr: String, replaceExisting: Boolean = true): Pair<Boolean, String> {
        return try {
            val root = if (jsonStr.trim().startsWith("[")) {
                JSONObject().put("records", JSONArray(jsonStr))
            } else {
                JSONObject(jsonStr)
            }

            // Restore Vehicle Settings if present
            if (root.has("vehicleSettings")) {
                val vObj = root.getJSONObject("vehicleSettings")
                val vName = vObj.optString("vehicleName", vehicleSettings.value.vehicleName)
                val cap = if (vObj.has("batteryCapacityKwh")) vObj.getDouble("batteryCapacityKwh")
                else if (vObj.has("usableBatteryKwh")) vObj.getDouble("usableBatteryKwh")
                else vehicleSettings.value.usableBatteryKwh
                val rKm = if (vObj.has("officialRangeKm")) vObj.getDouble("officialRangeKm") else vehicleSettings.value.officialRangeKm
                val rStd = if (vObj.has("rangeStandard")) vObj.getString("rangeStandard")
                else if (vObj.has("ratingStandard")) vObj.getString("ratingStandard")
                else vehicleSettings.value.ratingStandard
                val dUnit = vObj.optString("distanceUnit", vehicleSettings.value.distanceUnit)
                val cUnit = if (vObj.has("currency")) vObj.getString("currency")
                else vObj.optString("currencyUnit", vehicleSettings.value.currencyUnit)

                saveVehicleSettings(
                    VehicleSettings(
                        vehicleName = vName,
                        usableBatteryKwh = cap,
                        officialRangeKm = rKm,
                        ratingStandard = rStd,
                        distanceUnit = dUnit,
                        currencyUnit = cUnit
                    )
                )
            } else if (root.has("vehicle")) {
                val vObj = root.getJSONObject("vehicle")
                saveVehicleSettings(
                    VehicleSettings(
                        vehicleName = vObj.optString("name", vObj.optString("vehicleName", vehicleSettings.value.vehicleName)),
                        usableBatteryKwh = vObj.optDouble("usableBatteryKwh", vObj.optDouble("batteryCapacityKwh", vehicleSettings.value.usableBatteryKwh)),
                        officialRangeKm = vObj.optDouble("officialRangeKm", vehicleSettings.value.officialRangeKm),
                        ratingStandard = vObj.optString("ratingStandard", vObj.optString("rangeStandard", vehicleSettings.value.ratingStandard))
                    )
                )
            }

            val recordsArray = if (root.has("records")) root.getJSONArray("records") else JSONArray()
            val parsedList = mutableListOf<ChargingRecord>()

            val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

            for (i in 0 until recordsArray.length()) {
                val obj = recordsArray.getJSONObject(i)

                val odo = if (obj.has("totalOdometer")) obj.getDouble("totalOdometer")
                else if (obj.has("odometerKm")) obj.getDouble("odometerKm")
                else if (obj.has("odometer")) obj.getDouble("odometer")
                else 0.0

                val energy = if (obj.has("chargedKwh")) obj.getDouble("chargedKwh")
                else if (obj.has("energyKwh")) obj.getDouble("energyKwh")
                else if (obj.has("kwh")) obj.getDouble("kwh")
                else 0.0

                val costVal = if (obj.has("amount")) obj.getDouble("amount")
                else if (obj.has("cost")) obj.getDouble("cost")
                else 0.0

                val cType = if (obj.has("chargingType")) obj.getString("chargingType")
                else if (obj.has("chargeType")) obj.getString("chargeType")
                else "DC"
                val normChargeType = if (cType.equals("AC", ignoreCase = true)) "AC" else "DC"

                var op = obj.optString("operator", "").trim()
                if (op.isEmpty()) {
                    op = if (normChargeType == "AC") "家用充電" else "其他"
                }

                val endSoc = if (obj.has("endSoc")) obj.getInt("endSoc")
                else if (obj.has("socPercent")) obj.getInt("socPercent")
                else 80

                val startSoc = if (obj.has("startSoc") && !obj.isNull("startSoc")) obj.getInt("startSoc")
                else if (obj.has("startSocPercent") && !obj.isNull("startSocPercent")) obj.getInt("startSocPercent")
                else null

                val loc = if (obj.has("locationAddress")) obj.optString("locationAddress", "")
                else if (obj.has("locationName")) obj.optString("locationName", "")
                else ""

                val lat = if (obj.has("latitude") && !obj.isNull("latitude")) obj.getDouble("latitude") else null
                val lng = if (obj.has("longitude") && !obj.isNull("longitude")) obj.getDouble("longitude") else null

                val skip = if (obj.has("skipIntervalCalculation")) obj.getBoolean("skipIntervalCalculation")
                else if (obj.has("skipEfficiencyCalc")) obj.getBoolean("skipEfficiencyCalc")
                else false

                val ts = if (obj.has("timestamp")) {
                    obj.getLong("timestamp")
                } else if (obj.has("dateTime")) {
                    try {
                        dateFormat.parse(obj.getString("dateTime"))?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }
                } else {
                    System.currentTimeMillis()
                }

                val notes = obj.optString("notes", "")

                val sId = if (obj.has("syncId")) obj.optString("syncId", "")
                else if (obj.has("sync_id")) obj.optString("sync_id", "")
                else ""
                val finalSyncId = if (sId.isNotBlank()) sId else java.util.UUID.randomUUID().toString()
                val upd = if (obj.has("updatedAt")) obj.optLong("updatedAt", ts)
                else if (obj.has("updated_at")) obj.optLong("updated_at", ts)
                else ts

                parsedList.add(
                    ChargingRecord(
                        id = 0, // Auto generated
                        timestamp = ts,
                        odometerKm = odo,
                        energyKwh = energy,
                        cost = costVal,
                        chargeType = normChargeType,
                        operator = op,
                        socPercent = endSoc,
                        startSocPercent = startSoc,
                        locationName = loc,
                        latitude = lat,
                        longitude = lng,
                        skipEfficiencyCalc = skip,
                        notes = notes,
                        syncId = finalSyncId,
                        updatedAt = upd
                    )
                )
            }

            if (parsedList.isEmpty()) {
                return Pair(false, "JSON 檔案中未發現任何有效的充電紀錄")
            }

            viewModelScope.launch {
                if (replaceExisting) {
                    chargingRepo.deleteAll()
                }
                chargingRepo.insertAll(parsedList)
            }

            Pair(true, "成功還原 ${parsedList.size} 筆充電紀錄！")
        } catch (e: Exception) {
            Pair(false, "匯入失敗：${e.localizedMessage ?: "格式不正確"}")
        }
    }
}
