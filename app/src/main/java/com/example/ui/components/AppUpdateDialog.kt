package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalExtendedColors
import com.example.ui.viewmodel.AppUpdateUiState
import com.example.ui.viewmodel.EVViewModel
import java.util.Locale

@Composable
fun AppUpdateDialog(
    viewModel: EVViewModel,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalExtendedColors.current
    val context = LocalContext.current
    val updateState by viewModel.updateUiState.collectAsState()

    when (val state = updateState) {
        is AppUpdateUiState.Idle -> {
            // Nothing to show
        }

        is AppUpdateUiState.Checking -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateDialog() },
                containerColor = extendedColors.cardBackground,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = extendedColors.accentPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "正在檢查新版本...",
                            color = extendedColors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Text(
                        text = "正在連線至 GitHub Releases 查詢最新發布資訊...",
                        color = extendedColors.textSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                        Text("取消", color = extendedColors.textSecondary)
                    }
                }
            )
        }

        is AppUpdateUiState.Available -> {
            val info = state.info
            val sizeMbText = if (info.apkSizeBytes > 0) {
                String.format(Locale.getDefault(), " (%.1f MB)", info.apkSizeBytes / (1024f * 1024f))
            } else ""

            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateDialog() },
                containerColor = extendedColors.cardBackground,
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(extendedColors.accentPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NewReleases,
                            contentDescription = "New Release",
                            tint = extendedColors.accentPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "發現新版本 ${info.tagName}",
                            color = extendedColors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = info.releaseTitle,
                            color = extendedColors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                },
                text = {
                    Column {
                        if (info.releaseNotes.isNotBlank()) {
                            Text(
                                text = "更新內容：",
                                color = extendedColors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(extendedColors.cardBorder.copy(alpha = 0.2f))
                                    .padding(10.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = info.releaseNotes,
                                    color = extendedColors.textSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        Text(
                            text = "更新檔：${info.apkFileName}$sizeMbText",
                            color = extendedColors.textSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "直接覆蓋更新，所有儲存的本機紀錄皆會完整保留。",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.startDownloadUpdate(context, info)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = extendedColors.accentPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("立即下載更新", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                        Text("稍後提醒", color = extendedColors.textSecondary)
                    }
                }
            )
        }

        is AppUpdateUiState.Downloading -> {
            val progress = state.progress
            AlertDialog(
                onDismissRequest = { /* Don't dismiss while downloading */ },
                containerColor = extendedColors.cardBackground,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = extendedColors.accentPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "正在下載更新...",
                            color = extendedColors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "正在下載 ${state.info.tagName} 安裝套件 ($progress%)",
                            color = extendedColors.textSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = extendedColors.accentPrimary,
                            trackColor = extendedColors.cardBorder.copy(alpha = 0.3f),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "下載完成後系統將自動引導進行更新安裝",
                            color = extendedColors.textSecondary.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.dismissUpdateDialog() }
                    ) {
                        Text("背景下載", color = extendedColors.accentPrimary)
                    }
                }
            )
        }

        is AppUpdateUiState.ReadyToInstall -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateDialog() },
                containerColor = extendedColors.cardBackground,
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Ready to Install",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "更新套件已下載完成",
                        color = extendedColors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "安裝程式已啟動。若您的手機未自動跳出安裝視窗，請點選下方按鈕手動開啟安裝程序。",
                        color = extendedColors.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.installDownloadedApk(context, state.apkFile)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("立即安裝", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                        Text("關閉", color = extendedColors.textSecondary)
                    }
                }
            )
        }

        is AppUpdateUiState.UpToDate -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateDialog() },
                containerColor = extendedColors.cardBackground,
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Up to date",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "目前已是最新版本",
                        color = extendedColors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "您目前使用的是最新版本 (v${state.latestVersion})，無需更新。",
                        color = extendedColors.textSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                        Text("確定", color = extendedColors.accentPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        is AppUpdateUiState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateDialog() },
                containerColor = extendedColors.cardBackground,
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "檢查更新提示",
                        color = extendedColors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = state.message,
                        color = extendedColors.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                        Text("確定", color = extendedColors.accentPrimary)
                    }
                }
            )
        }
    }
}
