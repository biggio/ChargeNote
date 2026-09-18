package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AccentTheme
import com.example.data.model.AppThemeConfig
import com.example.data.model.ThemeMode
import com.example.data.model.VehicleSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ev_settings_prefs", Context.MODE_PRIVATE)

    private val _vehicleSettings = MutableStateFlow(loadVehicleSettings())
    val vehicleSettings: StateFlow<VehicleSettings> = _vehicleSettings.asStateFlow()

    private val _themeConfig = MutableStateFlow(loadThemeConfig())
    val themeConfig: StateFlow<AppThemeConfig> = _themeConfig.asStateFlow()

    private val _customOperators = MutableStateFlow(loadOperators())
    val customOperators: StateFlow<List<String>> = _customOperators.asStateFlow()

    private val _customGeminiApiKey = MutableStateFlow(loadCustomGeminiApiKey())
    val customGeminiApiKey: StateFlow<String> = _customGeminiApiKey.asStateFlow()

    private fun loadCustomGeminiApiKey(): String {
        return prefs.getString("custom_gemini_api_key", "") ?: ""
    }

    fun saveCustomGeminiApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString("custom_gemini_api_key", trimmed).apply()
        _customGeminiApiKey.value = trimmed
    }

    fun getEffectiveApiKey(): String {
        return _customGeminiApiKey.value.trim()
    }

    private val _cloudflareWorkerUrl = MutableStateFlow(prefs.getString("cloudflare_worker_url", "") ?: "")
    val cloudflareWorkerUrl: StateFlow<String> = _cloudflareWorkerUrl.asStateFlow()

    private val _cloudflareSyncSecret = MutableStateFlow(prefs.getString("cloudflare_sync_secret", "") ?: "")
    val cloudflareSyncSecret: StateFlow<String> = _cloudflareSyncSecret.asStateFlow()

    private val _cloudflareAutoSync = MutableStateFlow(prefs.getBoolean("cloudflare_auto_sync", true))
    val cloudflareAutoSync: StateFlow<Boolean> = _cloudflareAutoSync.asStateFlow()

    private val _cloudflareLastSyncTime = MutableStateFlow(prefs.getLong("cloudflare_last_sync_time", 0L))
    val cloudflareLastSyncTime: StateFlow<Long> = _cloudflareLastSyncTime.asStateFlow()

    fun saveCloudflareConfig(url: String, secret: String, autoSync: Boolean) {
        val cleanUrl = url.trim()
        val cleanSecret = secret.trim()
        prefs.edit()
            .putString("cloudflare_worker_url", cleanUrl)
            .putString("cloudflare_sync_secret", cleanSecret)
            .putBoolean("cloudflare_auto_sync", autoSync)
            .apply()
        _cloudflareWorkerUrl.value = cleanUrl
        _cloudflareSyncSecret.value = cleanSecret
        _cloudflareAutoSync.value = autoSync
    }

    fun updateLastSyncTime(timestamp: Long) {
        prefs.edit().putLong("cloudflare_last_sync_time", timestamp).apply()
        _cloudflareLastSyncTime.value = timestamp
    }

    private fun loadVehicleSettings(): VehicleSettings {
        return VehicleSettings(
            vehicleName = prefs.getString("vehicle_name", "Luxgen N7 亮點版7人") ?: "Luxgen N7 亮點版7人",
            usableBatteryKwh = prefs.getFloat("usable_battery_kwh", 57.7f).toDouble(),
            officialRangeKm = prefs.getFloat("official_range_km", 300.0f).toDouble(),
            ratingStandard = prefs.getString("rating_standard", "NEDC") ?: "NEDC",
            distanceUnit = prefs.getString("distance_unit", "km") ?: "km",
            currencyUnit = prefs.getString("currency_unit", "TWD") ?: "TWD"
        )
    }

    fun saveVehicleSettings(settings: VehicleSettings) {
        prefs.edit()
            .putString("vehicle_name", settings.vehicleName)
            .putFloat("usable_battery_kwh", settings.usableBatteryKwh.toFloat())
            .putFloat("official_range_km", settings.officialRangeKm.toFloat())
            .putString("rating_standard", settings.ratingStandard)
            .putString("distance_unit", settings.distanceUnit)
            .putString("currency_unit", settings.currencyUnit)
            .apply()
        _vehicleSettings.value = settings
    }

    private fun loadThemeConfig(): AppThemeConfig {
        val modeStr = prefs.getString("theme_mode", ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        val accentStr = prefs.getString("accent_theme", AccentTheme.TECH_BLUE.name) ?: AccentTheme.TECH_BLUE.name
        val mode = try { ThemeMode.valueOf(modeStr) } catch (e: Exception) { ThemeMode.DARK }
        val accent = try { AccentTheme.valueOf(accentStr) } catch (e: Exception) { AccentTheme.TECH_BLUE }
        return AppThemeConfig(themeMode = mode, accentTheme = accent)
    }

    fun saveThemeConfig(config: AppThemeConfig) {
        prefs.edit()
            .putString("theme_mode", config.themeMode.name)
            .putString("accent_theme", config.accentTheme.name)
            .apply()
        _themeConfig.value = config
    }

    private fun loadOperators(): List<String> {
        val jsonStr = prefs.getString("custom_operators_json", null)
        if (jsonStr.isNullOrBlank()) return DEFAULT_OPERATORS
        return try {
            val arr = org.json.JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val item = arr.getString(i).trim()
                if (item.isNotEmpty() && !list.contains(item)) {
                    list.add(item)
                }
            }
            if (list.isEmpty()) DEFAULT_OPERATORS else list
        } catch (e: Exception) {
            DEFAULT_OPERATORS
        }
    }

    fun saveOperators(list: List<String>) {
        val distinctList = list.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val arr = org.json.JSONArray()
        distinctList.forEach { arr.put(it) }
        prefs.edit().putString("custom_operators_json", arr.toString()).apply()
        _customOperators.value = distinctList
    }

    fun addOperator(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        val current = _customOperators.value.toMutableList()
        if (!current.contains(trimmed)) {
            current.add(trimmed)
            saveOperators(current)
            return true
        }
        return false
    }

    fun removeOperator(name: String) {
        val current = _customOperators.value.toMutableList()
        if (current.remove(name.trim())) {
            saveOperators(current)
        }
    }

    fun resetOperators() {
        prefs.edit().remove("custom_operators_json").apply()
        _customOperators.value = DEFAULT_OPERATORS
    }

    companion object {
        val DEFAULT_OPERATORS = listOf(
            "家用充電",
            "U-POWER",
            "特爾電力",
            "iCharging",
            "EVOASIS",
            "Tesla 超充",
            "華城 EVALUE",
            "星舟快充",
            "台泥 NHOA",
            "中油快充"
        )
    }
}
