package com.example.ui.screens

import com.example.BuildConfig
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import com.example.ui.components.AppUpdateDialog
import com.example.ui.viewmodel.AppUpdateUiState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AccentTheme
import com.example.data.model.AppThemeConfig
import com.example.data.model.ThemeMode
import com.example.data.model.VehicleSettings
import com.example.ui.theme.LocalExtendedColors
import com.example.ui.viewmodel.EVViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: EVViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalExtendedColors.current
    val context = LocalContext.current
    val vehicleSettings by viewModel.vehicleSettings.collectAsState()
    val themeConfig by viewModel.themeConfig.collectAsState()
    val customApiKey by viewModel.customGeminiApiKey.collectAsState()
    val customOperators by viewModel.customOperators.collectAsState()
    val cloudflareWorkerUrl by viewModel.cloudflareWorkerUrl.collectAsState()
    val cloudflareSyncSecret by viewModel.cloudflareSyncSecret.collectAsState()
    val cloudflareAutoSync by viewModel.cloudflareAutoSync.collectAsState()
    val cloudflareLastSyncTime by viewModel.cloudflareLastSyncTime.collectAsState()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsState()
    val autoCheckUpdate by viewModel.autoCheckUpdate.collectAsState()
    val lastUpdateCheckTime by viewModel.lastUpdateCheckTime.collectAsState()
    val updateUiState by viewModel.updateUiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var apiKeyInput by remember(customApiKey) { mutableStateOf(customApiKey) }
    var isApiKeySaved by remember { mutableStateOf(false) }
    var newOperatorInput by remember { mutableStateOf("") }

    var workerUrlInput by remember(cloudflareWorkerUrl) { mutableStateOf(cloudflareWorkerUrl) }
    var syncSecretInput by remember(cloudflareSyncSecret) { mutableStateOf(cloudflareSyncSecret) }
    var autoSyncChecked by remember(cloudflareAutoSync) { mutableStateOf(cloudflareAutoSync) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var syncStatusMessage by remember { mutableStateOf<String?>(null) }
    var syncStatusIsError by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    var vehicleName by remember(vehicleSettings) { mutableStateOf(vehicleSettings.vehicleName) }
    var usableBatteryText by remember(vehicleSettings) { mutableStateOf(vehicleSettings.usableBatteryKwh.toString()) }
    var officialRangeText by remember(vehicleSettings) { mutableStateOf(vehicleSettings.officialRangeKm.toString()) }
    var ratingStandard by remember(vehicleSettings) { mutableStateOf(vehicleSettings.ratingStandard) }
    var distanceUnit by remember(vehicleSettings) { mutableStateOf(vehicleSettings.distanceUnit) }
    var currencyUnit by remember(vehicleSettings) { mutableStateOf(vehicleSettings.currencyUnit) }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportContent by remember { mutableStateOf("") }
    var exportTypeTitle by remember { mutableStateOf("") }

    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var replaceExistingData by remember { mutableStateOf(true) }

    val jsonFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (content.isNotBlank()) {
                    selectedFileName = uri.lastPathSegment?.substringAfterLast('/') ?: "選取的 JSON 檔案"
                    importText = content
                    showImportDialog = true
                }
            } catch (e: Exception) {
                Toast.makeText(context, "讀取檔案失敗：${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val saveJsonDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(exportContent.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                }
                Toast.makeText(context, "✅ JSON 檔案已成功儲存至指定資料夾！", Toast.LENGTH_LONG).show()
                showExportDialog = false
            } catch (e: Exception) {
                Toast.makeText(context, "儲存失敗：${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val saveCsvDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(exportContent.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                }
                Toast.makeText(context, "✅ CSV 報表已成功儲存至指定資料夾！", Toast.LENGTH_LONG).show()
                showExportDialog = false
            } catch (e: Exception) {
                Toast.makeText(context, "儲存失敗：${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showDemoConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(extendedColors.cardBackground)
                        .testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = extendedColors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "系統與車輛設定",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.textPrimary
                    )
                    Text(
                        text = "調整車輛規格、外觀主題與備份還原",
                        fontSize = 12.sp,
                        color = extendedColors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Vehicle Specs Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(extendedColors.cardBackground)
                    .border(1.dp, extendedColors.cardBorder, RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(extendedColors.accentPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "Vehicle Specs",
                                tint = extendedColors.accentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "車輛規格設定",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = extendedColors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = vehicleName,
                        onValueChange = { vehicleName = it },
                        label = { Text("車輛名稱 / 車型") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vehicle_name_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = extendedColors.subCardBackground,
                            unfocusedContainerColor = extendedColors.subCardBackground,
                            focusedTextColor = extendedColors.textPrimary,
                            unfocusedTextColor = extendedColors.textPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = usableBatteryText,
                            onValueChange = { usableBatteryText = it },
                            label = { Text("可用電池 (kWh)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("usable_battery_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = extendedColors.subCardBackground,
                                unfocusedContainerColor = extendedColors.subCardBackground,
                                focusedTextColor = extendedColors.textPrimary,
                                unfocusedTextColor = extendedColors.textPrimary
                            )
                        )

                        OutlinedTextField(
                            value = officialRangeText,
                            onValueChange = { officialRangeText = it },
                            label = { Text("標定續航 (km)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("official_range_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = extendedColors.subCardBackground,
                                unfocusedContainerColor = extendedColors.subCardBackground,
                                focusedTextColor = extendedColors.textPrimary,
                                unfocusedTextColor = extendedColors.textPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "標定續航標準",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = extendedColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("NEDC", "WLTP", "EPA", "CLTC").forEach { std ->
                            val isSelected = ratingStandard == std
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) extendedColors.accentPrimary else extendedColors.subCardBackground)
                                    .clickable { ratingStandard = std }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = std,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else extendedColors.textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val battery = usableBatteryText.toDoubleOrNull() ?: 57.7
                            val range = officialRangeText.toDoubleOrNull() ?: 300.0
                            viewModel.saveVehicleSettings(
                                VehicleSettings(
                                    vehicleName = vehicleName.trim(),
                                    usableBatteryKwh = battery,
                                    officialRangeKm = range,
                                    ratingStandard = ratingStandard,
                                    distanceUnit = distanceUnit,
                                    currencyUnit = currencyUnit
                                )
                            )
                            Toast.makeText(context, "車輛規格已儲存！", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_vehicle_settings_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = extendedColors.accentPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "儲存車輛規格", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Theme & Appearance Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(extendedColors.cardBackground)
                    .border(1.dp, extendedColors.cardBorder, RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(extendedColors.accentPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ColorLens,
                                contentDescription = "Theme",
                                tint = extendedColors.accentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "外觀與色彩主題",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = extendedColors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dark / Light / System Mode
                    Text(
                        text = "顯示模式",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = extendedColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            ThemeMode.LIGHT to "☀️ 日間明亮",
                            ThemeMode.DARK to "🌙 深色夜間",
                            ThemeMode.SYSTEM to "⚙️ 跟隨系統"
                        ).forEach { (mode, label) ->
                            val isSelected = themeConfig.themeMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) extendedColors.accentPrimary else extendedColors.subCardBackground)
                                    .clickable {
                                        viewModel.saveThemeConfig(themeConfig.copy(themeMode = mode))
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else extendedColors.textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Accent Colors
                    Text(
                        text = "主色調風格",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = extendedColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    AccentTheme.values().forEach { accent ->
                        val isSelected = themeConfig.accentTheme == accent
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(accent.primaryHex).copy(alpha = 0.15f) else extendedColors.subCardBackground)
                                .border(
                                    1.dp,
                                    if (isSelected) Color(accent.primaryHex) else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.saveThemeConfig(themeConfig.copy(accentTheme = accent))
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(accent.primaryHex))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = accent.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = extendedColors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Text("✓", color = Color(accent.primaryHex), fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gemini AI Vision Settings Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(extendedColors.cardBackground)
                    .border(1.dp, extendedColors.cardBorder, RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(extendedColors.accentPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Gemini AI Settings",
                                    tint = extendedColors.accentPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Gemini AI 智慧視覺設定",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = extendedColors.textPrimary
                            )
                        }

                        val effectiveKey = viewModel.getEffectiveApiKey()
                        val isConfigured = effectiveKey.isNotBlank()
                        val statusText = if (isConfigured) "已配置金鑰" else "未配置金鑰"
                        val statusBg = if (isConfigured) extendedColors.accentSecondary.copy(alpha = 0.15f) else Color(0xFFFF5252).copy(alpha = 0.15f)
                        val statusColor = if (isConfigured) extendedColors.accentSecondary else Color(0xFFFF5252)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(statusBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "支援透過 Google Gemini 視覺 AI 自動辨識充電樁螢幕、各家充電 App 結算畫面截圖或發票。本程式不內建任何金鑰，請點選「免費領取」前往 Google AI Studio 申請個人專屬免費 API Key 後填入儲存。",
                        fontSize = 12.sp,
                        color = extendedColors.textSecondary,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            isApiKeySaved = false
                        },
                        label = { Text("Google Gemini API Key") },
                        placeholder = { Text("請貼上您的 API Key (例如 AIzaSy...)") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "API Key",
                                tint = extendedColors.accentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = extendedColors.subCardBackground,
                            unfocusedContainerColor = extendedColors.subCardBackground,
                            focusedTextColor = extendedColors.textPrimary,
                            unfocusedTextColor = extendedColors.textPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveCustomGeminiApiKey(apiKeyInput.trim())
                                isApiKeySaved = true
                                Toast.makeText(context, "Gemini API Key 設定已儲存", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = extendedColors.accentPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isApiKeySaved) "已儲存" else "儲存金鑰",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        if (customApiKey.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    apiKeyInput = ""
                                    viewModel.saveCustomGeminiApiKey("")
                                    isApiKeySaved = false
                                    Toast.makeText(context, "已清除 Gemini API Key", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("清除", fontSize = 13.sp, color = extendedColors.textSecondary)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "無法開啟瀏覽器", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Get Key",
                                modifier = Modifier.size(14.dp),
                                tint = extendedColors.accentPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("免費領取", fontSize = 12.sp, color = extendedColors.accentPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Charging Operators Management Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(extendedColors.cardBackground)
                    .border(1.dp, extendedColors.cardBorder, RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(extendedColors.accentPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Operator Management",
                                    tint = extendedColors.accentPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "充電營運商管理",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = extendedColors.textPrimary
                            )
                        }

                        TextButton(
                            onClick = {
                                viewModel.resetOperators()
                                Toast.makeText(context, "已還原為預設營運商清單", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("恢復預設", fontSize = 12.sp, color = extendedColors.accentPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "自訂記帳時的營運商快捷選單。點擊「✕」可移除不常去的站點，或在下方自行新增新品牌。",
                        fontSize = 12.sp,
                        color = extendedColors.textSecondary,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Existing Operators Chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        customOperators.forEach { op ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(extendedColors.subCardBackground)
                                    .border(1.dp, extendedColors.cardBorder, RoundedCornerShape(8.dp))
                                    .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = op,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = extendedColors.textPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(extendedColors.cardBackground)
                                        .clickable {
                                            viewModel.removeOperator(op)
                                            Toast.makeText(context, "已移除「$op」", Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove $op",
                                        tint = extendedColors.textSecondary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Add new operator input row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newOperatorInput,
                            onValueChange = { newOperatorInput = it },
                            placeholder = { Text("新增品牌 (例如 台亞充電)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = extendedColors.subCardBackground,
                                unfocusedContainerColor = extendedColors.subCardBackground,
                                focusedTextColor = extendedColors.textPrimary,
                                unfocusedTextColor = extendedColors.textPrimary
                            )
                        )

                        Button(
                            onClick = {
                                val trimmed = newOperatorInput.trim()
                                if (trimmed.isNotBlank()) {
                                    val added = viewModel.addOperator(trimmed)
                                    if (added) {
                                        Toast.makeText(context, "已新增「$trimmed」", Toast.LENGTH_SHORT).show()
                                        newOperatorInput = ""
                                    } else {
                                        Toast.makeText(context, "「$trimmed」已在清單中", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = newOperatorInput.isNotBlank(),
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = extendedColors.accentPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("新增", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cloudflare D1 Cloud Sync Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(extendedColors.cardBackground)
                    .border(1.dp, extendedColors.cardBorder, RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF38020).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Cloudflare D1",
                                tint = Color(0xFFF38020),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "☁️ Cloudflare D1 雲端同步",
                                color = extendedColors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "離線可讀寫，跨裝置雙向同步與雲端備份",
                                color = extendedColors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Worker URL input
                    Text(
                        text = "Worker 部署網址 (URL)",
                        color = extendedColors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = workerUrlInput,
                        onValueChange = { workerUrlInput = it },
                        placeholder = { Text("https://chargenote.biggio.workers.dev", color = extendedColors.textSecondary.copy(alpha = 0.5f), fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Cloud, contentDescription = null, tint = extendedColors.textSecondary, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = extendedColors.cardBackground,
                            unfocusedContainerColor = extendedColors.cardBackground,
                            focusedTextColor = extendedColors.textPrimary,
                            unfocusedTextColor = extendedColors.textPrimary,
                            focusedIndicatorColor = Color(0xFFF38020),
                            unfocusedIndicatorColor = extendedColors.cardBorder
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sync Secret input
                    Text(
                        text = "同步金鑰 (X-Sync-Secret)",
                        color = extendedColors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = syncSecretInput,
                        onValueChange = { syncSecretInput = it },
                        placeholder = { Text("請輸入自訂安全密碼（需與 Worker 一致）", color = extendedColors.textSecondary.copy(alpha = 0.5f), fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, tint = extendedColors.textSecondary, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = extendedColors.cardBackground,
                            unfocusedContainerColor = extendedColors.cardBackground,
                            focusedTextColor = extendedColors.textPrimary,
                            unfocusedTextColor = extendedColors.textPrimary,
                            focusedIndicatorColor = Color(0xFFF38020),
                            unfocusedIndicatorColor = extendedColors.cardBorder
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons: 測試連線 & 儲存設定
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val trimmedUrl = workerUrlInput.trim()
                                val trimmedSecret = syncSecretInput.trim()
                                if (trimmedUrl.isBlank()) {
                                    syncStatusMessage = "請先輸入 Cloudflare Worker 網址"
                                    syncStatusIsError = true
                                    return@OutlinedButton
                                }
                                isTestingConnection = true
                                syncStatusMessage = null
                                coroutineScope.launch {
                                    val (success, msg) = viewModel.testCloudflareConnection(trimmedUrl, trimmedSecret)
                                    isTestingConnection = false
                                    syncStatusIsError = !success
                                    syncStatusMessage = msg
                                    if (success) {
                                        viewModel.saveCloudflareConfig(trimmedUrl, trimmedSecret, autoSyncChecked)
                                    }
                                }
                            },
                            enabled = !isTestingConnection && !isCloudSyncing,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = extendedColors.accentPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("測試中...", fontSize = 13.sp)
                            } else {
                                Text("測試連線", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Button(
                            onClick = {
                                val trimmedUrl = workerUrlInput.trim()
                                val trimmedSecret = syncSecretInput.trim()
                                viewModel.saveCloudflareConfig(trimmedUrl, trimmedSecret, autoSyncChecked)
                                Toast.makeText(context, "✅ Cloudflare 設定已儲存", Toast.LENGTH_SHORT).show()
                                syncStatusMessage = "設定已更新儲存"
                                syncStatusIsError = false
                            },
                            enabled = !isTestingConnection && !isCloudSyncing,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF38020)),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("儲存設定", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Status Message Display
                    if (syncStatusMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (syncStatusIsError) Color(0xFFEF4444).copy(alpha = 0.12f)
                                    else Color(0xFF10B981).copy(alpha = 0.12f)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = syncStatusMessage!!,
                                color = if (syncStatusIsError) Color(0xFFEF4444) else Color(0xFF10B981),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = extendedColors.cardBorder.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto-sync Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "自動背景同步",
                                color = extendedColors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "新增或修改充電紀錄後，自動在背景同步至雲端 D1",
                                color = extendedColors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = autoSyncChecked,
                            onCheckedChange = {
                                autoSyncChecked = it
                                viewModel.saveCloudflareConfig(workerUrlInput.trim(), syncSecretInput.trim(), it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFF38020)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sync time display
                    val lastSyncText = if (cloudflareLastSyncTime > 0L) {
                        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                        "上次同步時間：${sdf.format(Date(cloudflareLastSyncTime))}"
                    } else {
                        "上次同步時間：尚未同步"
                    }
                    Text(
                        text = lastSyncText,
                        color = extendedColors.textSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Manual Sync & Cloud Restore Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (workerUrlInput.isBlank()) {
                                    Toast.makeText(context, "請先設定 Worker 網址並儲存", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                coroutineScope.launch {
                                    val (success, msg) = viewModel.performCloudSync()
                                    syncStatusIsError = !success
                                    syncStatusMessage = msg
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isCloudSyncing && workerUrlInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = extendedColors.accentPrimary),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            if (isCloudSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("同步中...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("立即同步", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (workerUrlInput.isBlank()) {
                                    Toast.makeText(context, "請先設定 Worker 網址並儲存", Toast.LENGTH_SHORT).show()
                                    return@OutlinedButton
                                }
                                showRestoreConfirmDialog = true
                            },
                            enabled = !isCloudSyncing && workerUrlInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("從雲端還原", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Helper info tip
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(extendedColors.cardBorder.copy(alpha = 0.25f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "💡 Cloudflare D1 為免費全球分散式 SQLite 資料庫。專案目錄下的 cloudflare/ 資料夾提供一鍵部署程式碼與 3 分鐘設定指引。",
                            color = extendedColors.textSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Data Backup, Export & Import Card (Screenshot 4)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(extendedColors.cardBackground)
                    .border(1.dp, extendedColors.cardBorder, RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(extendedColors.accentPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = "Data Management",
                                tint = extendedColors.accentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "資料管理與備份",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = extendedColors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Export JSON & CSV buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                exportContent = viewModel.exportJson()
                                exportTypeTitle = "JSON 備份檔案"
                                showExportDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_json_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = "JSON", modifier = Modifier.size(16.dp), tint = extendedColors.accentPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("匯出 JSON", color = extendedColors.textPrimary, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                exportContent = viewModel.exportCsv()
                                exportTypeTitle = "CSV 報表檔案"
                                showExportDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_csv_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = "CSV", modifier = Modifier.size(16.dp), tint = extendedColors.accentPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("匯出 CSV", color = extendedColors.textPrimary, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Import & Restore JSON file from phone
                    Button(
                        onClick = {
                            jsonFilePickerLauncher.launch("*/*")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_json_file_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = extendedColors.accentPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Pick JSON File", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📂 從手機選取 JSON 備份檔還原", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Import Text / Manual JSON
                    OutlinedButton(
                        onClick = {
                            selectedFileName = null
                            importText = ""
                            showImportDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_json_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = "Import", modifier = Modifier.size(18.dp), tint = extendedColors.rangeCyan)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("手動貼上 JSON 備份內容還原", color = extendedColors.textPrimary, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Load Demo Data
                    OutlinedButton(
                        onClick = { showDemoConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("load_demo_data_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Restore, contentDescription = "Demo Data", modifier = Modifier.size(18.dp), tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("載入示範紀錄 (Luxgen N7 多筆紀錄)", color = extendedColors.textPrimary, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Clear All Records
                    OutlinedButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_all_records_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF4444)
                        )
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Clear", modifier = Modifier.size(18.dp), tint = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("清除全部充電紀錄", color = Color(0xFFEF4444), fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. App Info & Version Update
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(extendedColors.cardBackground)
                    .border(1.dp, extendedColors.cardBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = extendedColors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "電記 v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = extendedColors.textPrimary
                            )
                            Text(
                                text = "專為電動車主打造的高精度電耗與費用分析工具",
                                fontSize = 11.sp,
                                color = extendedColors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = extendedColors.cardBorder.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Auto-check update switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "啟動時自動檢查新版本",
                                color = extendedColors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "自動比對 GitHub Releases 最新釋出版本與更新日誌",
                                color = extendedColors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = autoCheckUpdate,
                            onCheckedChange = { viewModel.saveAutoCheckUpdate(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = extendedColors.accentPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Manual Check Update button & Last check time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val lastCheckStr = if (lastUpdateCheckTime > 0L) {
                            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                            "上次檢查：${sdf.format(Date(lastUpdateCheckTime))}"
                        } else {
                            "上次檢查：尚未檢查"
                        }
                        Text(
                            text = lastCheckStr,
                            color = extendedColors.textSecondary,
                            fontSize = 11.sp
                        )

                        OutlinedButton(
                            onClick = {
                                viewModel.checkForUpdates(isManual = true)
                            },
                            enabled = updateUiState !is AppUpdateUiState.Checking && updateUiState !is AppUpdateUiState.Downloading,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            if (updateUiState is AppUpdateUiState.Checking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = extendedColors.accentPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("檢查中...", fontSize = 12.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = "Check Update",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("檢查新版本", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Export View Dialog
    if (showExportDialog) {
        Dialog(onDismissRequest = { showExportDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = extendedColors.cardBackground
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📋 匯出 $exportTypeTitle",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = extendedColors.textPrimary
                        )
                        IconButton(
                            onClick = { showExportDialog = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = extendedColors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = exportContent,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = extendedColors.subCardBackground,
                            unfocusedContainerColor = extendedColors.subCardBackground,
                            focusedTextColor = extendedColors.textPrimary,
                            unfocusedTextColor = extendedColors.textPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Save to Local Folder Button
                    Button(
                        onClick = {
                            val timeStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                            if (exportTypeTitle.contains("JSON")) {
                                saveJsonDocumentLauncher.launch("ev_charging_backup_$timeStr.json")
                            } else {
                                saveCsvDocumentLauncher.launch("ev_charging_report_$timeStr.csv")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = extendedColors.accentPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save to local folder",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("💾 儲存至本機資料夾", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary Actions: Share & Copy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val isJson = exportTypeTitle.contains("JSON")
                                val ext = if (isJson) "json" else "csv"
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = if (isJson) "application/json" else "text/csv"
                                    putExtra(Intent.EXTRA_SUBJECT, "ev_charging_export.$ext")
                                    putExtra(Intent.EXTRA_TEXT, exportContent)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "分享或儲存備份檔案"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(15.dp),
                                tint = extendedColors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("分享檔案", color = extendedColors.textPrimary, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("EV Export", exportContent)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "已複製到剪貼簿！", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                modifier = Modifier.size(15.dp),
                                tint = extendedColors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("複製內容", color = extendedColors.textPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Import Dialog
    if (showImportDialog) {
        Dialog(onDismissRequest = { showImportDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = extendedColors.cardBackground
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📥 還原 JSON 備份資料",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = extendedColors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Button to pick file
                    OutlinedButton(
                        onClick = {
                            jsonFilePickerLauncher.launch("*/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Browse", modifier = Modifier.size(18.dp), tint = extendedColors.accentPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedFileName != null) "已選取：$selectedFileName (點擊更換)" else "📂 從手機選取 .json 備份檔案",
                            color = extendedColors.textPrimary,
                            fontSize = 12.sp
                        )
                    }

                    if (importText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val (isValid, previewMsg) = viewModel.parseJsonPreview(importText.trim())
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isValid) extendedColors.accentPrimary.copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = if (isValid) "✅ 驗證成功：$previewMsg" else "⚠️ 格式提醒：$previewMsg",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isValid) extendedColors.textPrimary else Color(0xFFEF4444)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = importText,
                        onValueChange = {
                            importText = it
                            selectedFileName = null
                        },
                        placeholder = { Text("可在此處貼上 JSON 檔案內容，或點擊上方按鈕直接選取檔案...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = extendedColors.subCardBackground,
                            unfocusedContainerColor = extendedColors.subCardBackground,
                            focusedTextColor = extendedColors.textPrimary,
                            unfocusedTextColor = extendedColors.textPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Replace existing checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { replaceExistingData = !replaceExistingData }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = replaceExistingData,
                            onCheckedChange = { replaceExistingData = it },
                            colors = CheckboxDefaults.colors(checkedColor = extendedColors.accentPrimary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("覆蓋現有全部紀錄（完整備份還原）", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = extendedColors.textPrimary)
                            Text("取消勾選則將備份中的紀錄附加至現有紀錄", fontSize = 11.sp, color = extendedColors.textSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showImportDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("取消", color = extendedColors.textSecondary)
                        }

                        Button(
                            onClick = {
                                if (importText.isNotBlank()) {
                                    val (success, msg) = viewModel.importJson(importText.trim(), replaceExisting = replaceExistingData)
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    if (success) showImportDialog = false
                                } else {
                                    Toast.makeText(context, "請先選取或貼上 JSON 內容", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = extendedColors.accentPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Restore", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("確認還原", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Clear confirmation dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("清除全部充電紀錄？", color = extendedColors.textPrimary) },
            text = { Text("確定要刪除所有已儲存的充電與電耗紀錄嗎？此動作無法復原！建議您先匯出備份。", color = extendedColors.textSecondary) },
            containerColor = extendedColors.cardBackground,
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllRecords()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "已清除全部紀錄", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("確認清除", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("取消", color = extendedColors.textSecondary)
                }
            }
        )
    }

    // Demo Data confirmation dialog
    if (showDemoConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDemoConfirmDialog = false },
            title = { Text("載入示範資料？", color = extendedColors.textPrimary) },
            text = { Text("載入示範資料將會覆蓋現有紀錄並注入 8 筆真實情境充電紀錄（含特爾電力、家用充電、U-POWER、iCharging 等）。", color = extendedColors.textSecondary) },
            containerColor = extendedColors.cardBackground,
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.loadSampleData()
                        showDemoConfirmDialog = false
                        Toast.makeText(context, "已載入示範充電紀錄！", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("載入", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDemoConfirmDialog = false }) {
                    Text("取消", color = extendedColors.textSecondary)
                }
            }
        )
    }

    // Cloudflare D1 Restore Confirmation Dialog
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("從 Cloudflare D1 還原資料？", color = extendedColors.textPrimary) },
            text = {
                Text(
                    "確定要從雲端下載並還原全部紀錄嗎？若本地已存在相同紀錄將會更新為雲端版本，缺少之紀錄將會補回本地。",
                    color = extendedColors.textSecondary
                )
            },
            containerColor = extendedColors.cardBackground,
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        coroutineScope.launch {
                            val (success, msg) = viewModel.restoreFromCloudflare()
                            syncStatusIsError = !success
                            syncStatusMessage = msg
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("確認還原", color = Color(0xFFF38020), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("取消", color = extendedColors.textSecondary)
                }
            }
        )
    }
}
