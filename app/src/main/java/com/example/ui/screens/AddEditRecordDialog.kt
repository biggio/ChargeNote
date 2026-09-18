package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.model.ChargingRecord
import com.example.data.repository.SettingsRepository
import com.example.ui.theme.LocalExtendedColors
import com.example.ui.viewmodel.EVViewModel
import com.example.util.CalculationUtils
import com.example.util.GeminiRecognitionService
import com.example.util.ImageUtils
import com.example.util.MapUtils
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditRecordDialog(
    initialRecord: ChargingRecord? = null,
    latestOdometer: Double = 0.0,
    viewModel: EVViewModel? = null,
    onDismiss: () -> Unit,
    onSave: (ChargingRecord) -> Unit
) {
    val extendedColors = LocalExtendedColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val customOperatorsState = viewModel?.customOperators?.collectAsState()
    val availableOperators = remember(customOperatorsState?.value) {
        (customOperatorsState?.value ?: SettingsRepository.DEFAULT_OPERATORS).filter { it != "其他" }
    }
    val operatorList = remember(availableOperators) {
        availableOperators + "其他"
    }

    val customApiKey by viewModel?.customGeminiApiKey?.collectAsState() ?: remember { mutableStateOf("") }
    val hasApiKey = customApiKey.isNotBlank()

    var isRecognizing by remember { mutableStateOf(false) }
    var recognitionSuccessMessage by remember { mutableStateOf<String?>(null) }
    var recognitionErrorMessage by remember { mutableStateOf<String?>(null) }

    var timestamp by remember { mutableLongStateOf(initialRecord?.timestamp ?: System.currentTimeMillis()) }
    var odometerText by remember {
        mutableStateOf(
            if (initialRecord != null) initialRecord.odometerKm.toString()
            else ""
        )
    }
    var energyText by remember { mutableStateOf(initialRecord?.energyKwh?.toString() ?: "") }
    var costText by remember { mutableStateOf(initialRecord?.cost?.toString() ?: "") }
    var chargeType by remember { mutableStateOf(initialRecord?.chargeType ?: "DC") }
    var operator by remember {
        mutableStateOf(
            if (initialRecord != null) {
                if (availableOperators.contains(initialRecord.operator)) initialRecord.operator
                else "其他"
            } else {
                if (initialRecord?.chargeType == "AC") "家用充電"
                else (availableOperators.firstOrNull { it != "家用充電" } ?: "特爾電力")
            }
        )
    }
    var customOperatorText by remember {
        mutableStateOf(
            if (initialRecord != null && !availableOperators.contains(initialRecord.operator) && initialRecord.operator != "其他") {
                initialRecord.operator
            } else {
                ""
            }
        )
    }
    var socPercent by remember { mutableIntStateOf(initialRecord?.socPercent ?: 80) }
    var startSocPercentText by remember { mutableStateOf(initialRecord?.startSocPercent?.toString() ?: "") }
    var locationName by remember { mutableStateOf(initialRecord?.locationName ?: "") }
    var latitude by remember { mutableStateOf(initialRecord?.latitude) }
    var longitude by remember { mutableStateOf(initialRecord?.longitude) }
    var skipEfficiencyCalc by remember { mutableStateOf(initialRecord?.skipEfficiencyCalc ?: false) }
    var notes by remember { mutableStateOf(initialRecord?.notes ?: "") }
    var isLocating by remember { mutableStateOf(false) }

    var odometerHasFocused by remember { mutableStateOf(false) }
    var energyHasFocused by remember { mutableStateOf(false) }
    var costHasFocused by remember { mutableStateOf(false) }

    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { previewBitmap: Bitmap? ->
        if (previewBitmap != null) {
            scope.launch {
                isRecognizing = true
                recognitionSuccessMessage = null
                recognitionErrorMessage = null
                val processed = ImageUtils.processBitmap(previewBitmap, 1280)
                val result = if (viewModel != null) {
                    viewModel.recognizeChargingImage(processed)
                } else {
                    GeminiRecognitionService.recognizeImage(processed, "")
                }
                isRecognizing = false
                result.fold(
                    onSuccess = { info ->
                        if (info.energyKwh != null) energyText = info.energyKwh.toString()
                        if (info.cost != null) costText = if (info.cost % 1.0 == 0.0) info.cost.toInt().toString() else info.cost.toString()
                        if (info.operator != null) {
                            val matched = availableOperators.find { it.equals(info.operator, ignoreCase = true) }
                            if (matched != null) {
                                operator = matched
                                customOperatorText = ""
                            } else {
                                operator = "其他"
                                customOperatorText = info.operator
                            }
                        }
                        if (info.chargeType != null) chargeType = info.chargeType
                        if (info.startSocPercent != null) startSocPercentText = info.startSocPercent.toString()
                        if (info.endSocPercent != null) socPercent = info.endSocPercent
                        if (info.locationName != null) locationName = info.locationName
                        if (info.odometerKm != null) odometerText = if (info.odometerKm % 1.0 == 0.0) info.odometerKm.toInt().toString() else info.odometerKm.toString()
                        if (!info.notes.isNullOrBlank()) {
                            notes = if (notes.isBlank()) info.notes else "$notes\n${info.notes}"
                        }
                        recognitionSuccessMessage = info.rawSummary
                    },
                    onFailure = { err ->
                        recognitionErrorMessage = err.localizedMessage ?: "辨識失敗，請檢查網路或 API Key"
                    }
                )
            }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isRecognizing = true
                recognitionSuccessMessage = null
                recognitionErrorMessage = null
                val (processedBitmap, err) = ImageUtils.processImageUri(context, uri, 1280)
                if (processedBitmap == null) {
                    isRecognizing = false
                    recognitionErrorMessage = err ?: "無法讀取選取的圖片"
                    return@launch
                }
                val result = if (viewModel != null) {
                    viewModel.recognizeChargingImage(processedBitmap)
                } else {
                    GeminiRecognitionService.recognizeImage(processedBitmap, "")
                }
                isRecognizing = false
                result.fold(
                    onSuccess = { info ->
                        if (info.energyKwh != null) energyText = info.energyKwh.toString()
                        if (info.cost != null) costText = if (info.cost % 1.0 == 0.0) info.cost.toInt().toString() else info.cost.toString()
                        if (info.operator != null) {
                            val matched = availableOperators.find { it.equals(info.operator, ignoreCase = true) }
                            if (matched != null) {
                                operator = matched
                                customOperatorText = ""
                            } else {
                                operator = "其他"
                                customOperatorText = info.operator
                            }
                        }
                        if (info.chargeType != null) chargeType = info.chargeType
                        if (info.startSocPercent != null) startSocPercentText = info.startSocPercent.toString()
                        if (info.endSocPercent != null) socPercent = info.endSocPercent
                        if (info.locationName != null) locationName = info.locationName
                        if (info.odometerKm != null) odometerText = if (info.odometerKm % 1.0 == 0.0) info.odometerKm.toInt().toString() else info.odometerKm.toString()
                        if (!info.notes.isNullOrBlank()) {
                            notes = if (notes.isBlank()) info.notes else "$notes\n${info.notes}"
                        }
                        recognitionSuccessMessage = info.rawSummary
                    },
                    onFailure = { err ->
                        recognitionErrorMessage = err.localizedMessage ?: "辨識失敗，請檢查網路或 API Key"
                    }
                )
            }
        }
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isLocating = true
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                    isLocating = false
                    if (loc != null) {
                        latitude = loc.latitude
                        longitude = loc.longitude
                        scope.launch(Dispatchers.IO) {
                            try {
                                val geocoder = Geocoder(context, Locale.TAIWAN)
                                val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                                if (!addresses.isNullOrEmpty()) {
                                    val addr = addresses[0]
                                    val locStr = (addr.subLocality ?: addr.locality ?: "") + " " + (addr.thoroughfare ?: "")
                                    withContext(Dispatchers.Main) {
                                        if (locStr.isNotBlank()) locationName = locStr.trim()
                                    }
                                }
                            } catch (e: Exception) {
                                // fallback
                            }
                        }
                    }
                }
            } catch (e: SecurityException) {
                isLocating = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, extendedColors.cardBorder, RoundedCornerShape(20.dp))
                .testTag("add_edit_record_dialog"),
            color = extendedColors.cardBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialRecord == null) "⚡ 新增充電紀錄" else "✏️ 編輯充電紀錄",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.textPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = extendedColors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Gemini AI Recognition Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            1.dp,
                            extendedColors.accentPrimary.copy(alpha = 0.35f),
                            RoundedCornerShape(14.dp)
                        ),
                    color = extendedColors.accentPrimary.copy(alpha = 0.08f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini AI",
                                tint = extendedColors.accentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Gemini 智慧拍照辨識",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = extendedColors.accentPrimary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (isRecognizing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = extendedColors.accentPrimary,
                                    strokeWidth = 2.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "拍照或上傳充電樁螢幕、App結算畫面或發票，自動提取度數、金額與站點！",
                            fontSize = 11.sp,
                            color = extendedColors.textSecondary,
                            lineHeight = 15.sp
                        )

                        if (!hasApiKey) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "⚠️ 尚未設定 API Key，請先至「設定」輸入個人 Gemini Key",
                                fontSize = 11.sp,
                                color = Color(0xFFFF9800),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (!hasApiKey) {
                                        recognitionErrorMessage = "尚未設定 Gemini API Key！請先至「設定」頁面輸入您的個人 Google AI Studio API Key。"
                                    } else {
                                        takePhotoLauncher.launch(null)
                                    }
                                },
                                enabled = !isRecognizing,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = extendedColors.accentPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Camera",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("📷 拍照", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (!hasApiKey) {
                                        recognitionErrorMessage = "尚未設定 Gemini API Key！請先至「設定」頁面輸入您的個人 Google AI Studio API Key。"
                                    } else {
                                        pickImageLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                },
                                enabled = !isRecognizing,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, extendedColors.accentPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Gallery",
                                    tint = extendedColors.accentPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "🖼️ 選相片",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = extendedColors.accentPrimary
                                )
                            }
                        }

                        // Success / Error status banners
                        if (isRecognizing) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⏳ 正在傳輸圖片並請 Gemini AI 進行視覺分析，請稍候...",
                                fontSize = 11.sp,
                                color = extendedColors.accentPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        } else if (recognitionSuccessMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "✅ $recognitionSuccessMessage",
                                fontSize = 11.sp,
                                color = extendedColors.accentSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        } else if (recognitionErrorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⚠️ $recognitionErrorMessage",
                                fontSize = 11.sp,
                                color = Color(0xFFE53935),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Date & Time Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "充電時間",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = extendedColors.textSecondary
                    )
                    Text(
                        text = "⏱️ 設為現在",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.accentPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { timestamp = System.currentTimeMillis() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(extendedColors.subCardBackground)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date Selector
                    Row(
                        modifier = Modifier
                            .weight(1.15f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(extendedColors.cardBackground)
                            .border(1.dp, extendedColors.cardBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        cal.timeInMillis = timestamp
                                        cal.set(Calendar.YEAR, year)
                                        cal.set(Calendar.MONTH, month)
                                        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        timestamp = cal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Select Date",
                            tint = extendedColors.accentPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "日期",
                                fontSize = 10.sp,
                                color = extendedColors.textSecondary
                            )
                            Text(
                                text = CalculationUtils.formatDate(timestamp, "yyyy/MM/dd"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = extendedColors.textPrimary
                            )
                        }
                    }

                    // Time Selector
                    Row(
                        modifier = Modifier
                            .weight(0.95f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(extendedColors.cardBackground)
                            .border(1.dp, extendedColors.cardBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        cal.timeInMillis = timestamp
                                        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        cal.set(Calendar.MINUTE, minute)
                                        timestamp = cal.timeInMillis
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    true
                                ).show()
                            }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Select Time",
                            tint = extendedColors.accentPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "時間",
                                fontSize = 10.sp,
                                color = extendedColors.textSecondary
                            )
                            Text(
                                text = CalculationUtils.formatDate(timestamp, "HH:mm"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = extendedColors.textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Charge Type Selector: DC vs AC
                Text(
                    text = "充電型態",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = extendedColors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(extendedColors.subCardBackground)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // DC Fast Charge
                    val isDc = chargeType == "DC"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDc) extendedColors.dcColor else Color.Transparent)
                            .clickable {
                                chargeType = "DC"
                                if (operator == "家用充電") {
                                    operator = availableOperators.firstOrNull { it != "家用充電" } ?: "特爾電力"
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⚡", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "DC 快充",
                                fontSize = 14.sp,
                                fontWeight = if (isDc) FontWeight.Bold else FontWeight.Medium,
                                color = if (isDc) Color.White else extendedColors.textSecondary
                            )
                        }
                    }

                    // AC Slow Charge
                    val isAc = chargeType == "AC"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isAc) extendedColors.acColor else Color.Transparent)
                            .clickable {
                                chargeType = "AC"
                                if (availableOperators.contains("家用充電")) {
                                    operator = "家用充電"
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🔌", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AC 慢充 / 家充",
                                fontSize = 14.sp,
                                fontWeight = if (isAc) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAc) Color.White else extendedColors.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Odometer TextField
                OutlinedTextField(
                    value = odometerText,
                    onValueChange = { odometerText = it },
                    label = { Text("儀表總里程 (km)") },
                    placeholder = {
                        Text(
                            if (latestOdometer > 0) "前次: ${CalculationUtils.formatNumber(latestOdometer, 1)}"
                            else "請輸入總里程 (km)"
                        )
                    },
                    trailingIcon = {
                        if (odometerText.isNotEmpty()) {
                            IconButton(onClick = { odometerText = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = extendedColors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("odometer_input")
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused && initialRecord == null && !odometerHasFocused) {
                                odometerHasFocused = true
                                odometerText = ""
                            }
                        },
                    shape = RoundedCornerShape(10.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = extendedColors.subCardBackground,
                        unfocusedContainerColor = extendedColors.subCardBackground,
                        focusedTextColor = extendedColors.textPrimary,
                        unfocusedTextColor = extendedColors.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Energy & Cost in a 2-col row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = energyText,
                        onValueChange = { energyText = it },
                        label = { Text("充入度數 (kWh)") },
                        placeholder = { Text("例如 35.0") },
                        trailingIcon = {
                            if (energyText.isNotEmpty()) {
                                IconButton(onClick = { energyText = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = extendedColors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("energy_input")
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && initialRecord == null && !energyHasFocused) {
                                    energyHasFocused = true
                                    energyText = ""
                                }
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = extendedColors.subCardBackground,
                            unfocusedContainerColor = extendedColors.subCardBackground,
                            focusedTextColor = extendedColors.textPrimary,
                            unfocusedTextColor = extendedColors.textPrimary
                        )
                    )

                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it },
                        label = { Text("本次費用 (TWD)") },
                        placeholder = { Text("例如 280") },
                        trailingIcon = {
                            if (costText.isNotEmpty()) {
                                IconButton(onClick = { costText = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = extendedColors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cost_input")
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && initialRecord == null && !costHasFocused) {
                                    costHasFocused = true
                                    costText = ""
                                }
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = extendedColors.subCardBackground,
                            unfocusedContainerColor = extendedColors.subCardBackground,
                            focusedTextColor = extendedColors.textPrimary,
                            unfocusedTextColor = extendedColors.textPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SoC Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "充飽/充電後電量 (SoC)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = extendedColors.textSecondary
                    )
                    Text(
                        text = "$socPercent%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.accentSecondary
                    )
                }
                Slider(
                    value = socPercent.toFloat(),
                    onValueChange = { socPercent = it.toInt() },
                    valueRange = 0f..100f,
                    steps = 100,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = extendedColors.accentSecondary,
                        activeTrackColor = extendedColors.accentPrimary,
                        inactiveTrackColor = Color(0xFF334155)
                    )
                )

                // Quick SoC presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(70, 80, 85, 90, 100).forEach { p ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (socPercent == p) extendedColors.accentPrimary else extendedColors.subCardBackground)
                                .clickable { socPercent = p }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$p%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (socPercent == p) Color.White else extendedColors.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Charging Operator Chips
                Text(
                    text = "充電營運商",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = extendedColors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    operatorList.forEach { op ->
                        val isSelected = operator == op
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) extendedColors.accentPrimary.copy(alpha = 0.25f) else extendedColors.subCardBackground)
                                .border(
                                    1.dp,
                                    if (isSelected) extendedColors.accentPrimary else extendedColors.cardBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { operator = op }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = op,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) extendedColors.accentSecondary else extendedColors.textSecondary
                            )
                        }
                    }
                }

                if (operator == "其他") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customOperatorText,
                        onValueChange = { customOperatorText = it },
                        label = { Text("自訂營運商名稱 (例如 台亞充電)") },
                        placeholder = { Text("輸入新品牌，儲存時將自動加入常用清單") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_operator_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = extendedColors.subCardBackground,
                            unfocusedContainerColor = extendedColors.subCardBackground,
                            focusedTextColor = extendedColors.textPrimary,
                            unfocusedTextColor = extendedColors.textPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Location with GPS auto-locate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = locationName,
                        onValueChange = { locationName = it },
                        label = { Text("站點/地名備註 (例如 台北內湖旗艦站)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = extendedColors.accentSecondary
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("location_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = extendedColors.subCardBackground,
                            unfocusedContainerColor = extendedColors.subCardBackground,
                            focusedTextColor = extendedColors.textPrimary,
                            unfocusedTextColor = extendedColors.textPrimary
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            val finePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                            val coarsePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                            if (finePerm == PackageManager.PERMISSION_GRANTED || coarsePerm == PackageManager.PERMISSION_GRANTED) {
                                isLocating = true
                                fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                                    isLocating = false
                                    if (loc != null) {
                                        latitude = loc.latitude
                                        longitude = loc.longitude
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                val geocoder = Geocoder(context, Locale.TAIWAN)
                                                val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                                                if (!addresses.isNullOrEmpty()) {
                                                    val addr = addresses[0]
                                                    val locStr = (addr.subLocality ?: addr.locality ?: "") + " " + (addr.thoroughfare ?: "")
                                                    withContext(Dispatchers.Main) {
                                                        if (locStr.isNotBlank()) locationName = locStr.trim()
                                                    }
                                                }
                                            } catch (e: Exception) {}
                                        }
                                    }
                                }
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(extendedColors.subCardBackground)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Locate",
                            tint = if (isLocating) extendedColors.accentSecondary else extendedColors.textSecondary
                        )
                    }
                }

                // If GPS or location available, show quick map preview
                if (latitude != null && longitude != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(extendedColors.subCardBackground)
                            .clickable {
                                MapUtils.openInGoogleMaps(
                                    context = context,
                                    latitude = latitude,
                                    longitude = longitude,
                                    locationName = locationName
                                )
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Map Preview",
                            tint = extendedColors.accentSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "GPS: ${CalculationUtils.formatNumber(latitude!!, 4)}, ${CalculationUtils.formatNumber(longitude!!, 4)} · 點擊地圖預覽 ↗",
                            fontSize = 11.sp,
                            color = extendedColors.accentSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Skip Efficiency Calculation Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { skipEfficiencyCalc = !skipEfficiencyCalc }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = skipEfficiencyCalc,
                        onCheckedChange = { skipEfficiencyCalc = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = extendedColors.accentPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "略過本筆能耗計算",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = extendedColors.textPrimary
                        )
                        Text(
                            text = "勾選時不計入與前筆之區間行駛電耗（例如首筆基準、換胎或長途補登）",
                            fontSize = 11.sp,
                            color = extendedColors.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notes TextField
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("備忘備註 (選填)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = extendedColors.subCardBackground,
                        unfocusedContainerColor = extendedColors.subCardBackground,
                        focusedTextColor = extendedColors.textPrimary,
                        unfocusedTextColor = extendedColors.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("取消", color = extendedColors.textSecondary)
                    }

                    Button(
                        onClick = {
                            val odo = odometerText.toDoubleOrNull() ?: 0.0
                            val energy = energyText.toDoubleOrNull() ?: 0.0
                            val cost = costText.toDoubleOrNull() ?: 0.0
                            val startSoc = startSocPercentText.toIntOrNull()

                            val finalOperator = if (operator == "其他") {
                                val trimmed = customOperatorText.trim()
                                if (trimmed.isNotBlank()) {
                                    viewModel?.addOperator(trimmed)
                                    trimmed
                                } else {
                                    "其他"
                                }
                            } else {
                                operator
                            }

                            val record = ChargingRecord(
                                id = initialRecord?.id ?: 0,
                                timestamp = timestamp,
                                odometerKm = odo,
                                energyKwh = energy,
                                cost = cost,
                                chargeType = chargeType,
                                operator = finalOperator,
                                socPercent = socPercent,
                                startSocPercent = startSoc,
                                locationName = locationName,
                                latitude = latitude,
                                longitude = longitude,
                                skipEfficiencyCalc = skipEfficiencyCalc,
                                notes = notes,
                                syncId = initialRecord?.syncId ?: java.util.UUID.randomUUID().toString(),
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(record)
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("save_record_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = extendedColors.accentPrimary
                        )
                    ) {
                        Text(
                            text = "💾 儲存紀錄",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
