package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val tagName: String,
    val versionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val publishedAt: String,
    val apkDownloadUrl: String,
    val apkFileName: String,
    val apkSizeBytes: Long,
    val isNewer: Boolean
)

object AppUpdateService {
    private const val GITHUB_REPO_OWNER = "biggio"
    private const val GITHUB_REPO_NAME = "EV-Charger-Recorder"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Checks GitHub Releases API for the latest release.
     */
    suspend fun checkForUpdate(
        owner: String = GITHUB_REPO_OWNER,
        repo: String = GITHUB_REPO_NAME
    ): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "EV-Charger-Recorder-Android/${BuildConfig.VERSION_NAME}")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val body = response.body?.string() ?: ""

            if (code == 404) {
                // No releases published yet on GitHub
                return@withContext Result.success(null)
            }

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("GitHub API 回應錯誤 (HTTP $code)：$body"))
            }

            val json = JSONObject(body)
            val tagName = json.optString("tag_name", "").trim()
            val releaseTitle = json.optString("name", tagName).trim()
            val releaseNotes = json.optString("body", "").trim()
            val publishedAt = json.optString("published_at", "")

            // Find APK asset
            var apkUrl = ""
            var apkName = ""
            var apkSize = 0L

            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        apkName = name
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            if (apkUrl.isBlank()) {
                // If release exists but no APK asset attached
                return@withContext Result.success(null)
            }

            val cleanTag = tagName.removePrefix("v").trim()
            val isNewer = isVersionNewer(cleanTag, BuildConfig.VERSION_NAME)

            val info = UpdateInfo(
                tagName = tagName,
                versionName = cleanTag,
                releaseTitle = if (releaseTitle.isNotBlank()) releaseTitle else tagName,
                releaseNotes = releaseNotes,
                publishedAt = publishedAt,
                apkDownloadUrl = apkUrl,
                apkFileName = apkName,
                apkSizeBytes = apkSize,
                isNewer = isNewer
            )

            Result.success(info)
        } catch (e: Exception) {
            Result.failure(Exception("檢查更新失敗：${e.localizedMessage ?: "請檢查網路連線"}"))
        }
    }

    /**
     * Compare semantic versions e.g. "1.6.0" vs "1.5.1"
     */
    fun isVersionNewer(remoteVersion: String, currentVersion: String): Boolean {
        try {
            val remoteParts = remoteVersion.split("-", "+")[0].split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = currentVersion.split("-", "+")[0].split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        } catch (e: Exception) {
            return remoteVersion != currentVersion
        }
    }

    /**
     * Downloads the APK file to the app's cache directory with progress reporting (0..100).
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .addHeader("User-Agent", "EV-Charger-Recorder-Android/${BuildConfig.VERSION_NAME}")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("下載失敗 (HTTP ${response.code})"))
            }

            val responseBody = response.body ?: return@withContext Result.failure(Exception("回應內容為空"))
            val contentLength = responseBody.contentLength()

            val updateDir = File(context.cacheDir, "apk_updates")
            if (!updateDir.exists()) {
                updateDir.mkdirs()
            }
            val apkFile = File(updateDir, "EV-Charger-Recorder-update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            responseBody.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var totalRead = 0L
                    var read: Int
                    var lastReportedPercent = -1

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        totalRead += read
                        if (contentLength > 0) {
                            val percent = ((totalRead * 100) / contentLength).toInt().coerceIn(0, 100)
                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                    output.flush()
                }
            }

            onProgress(100)
            Result.success(apkFile)
        } catch (e: Exception) {
            Result.failure(Exception("下載 APK 失敗：${e.localizedMessage}"))
        }
    }

    /**
     * Checks whether the app has permission to request package installs.
     */
    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Launches the system settings screen for granting REQUEST_INSTALL_PACKAGES permission.
     */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Launches the Android PackageInstaller to install the downloaded APK.
     */
    fun installApk(context: Context, apkFile: File): Result<Unit> {
        try {
            if (!apkFile.exists()) {
                return Result.failure(Exception("找不到下載的安裝檔"))
            }

            if (!canInstallPackages(context)) {
                openInstallPermissionSettings(context)
                return Result.failure(Exception("尚未取得安裝未知應用程式權限，已為您開啟系統設定，請授權後再次點擊安裝"))
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(intent)
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(Exception("啟動安裝程式失敗：${e.localizedMessage}"))
        }
    }
}
