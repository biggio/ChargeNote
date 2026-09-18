package com.example.util

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class RecognizedChargingInfo(
    val energyKwh: Double? = null,
    val cost: Double? = null,
    val operator: String? = null,
    val chargeType: String? = null,
    val startSocPercent: Int? = null,
    val endSocPercent: Int? = null,
    val locationName: String? = null,
    val odometerKm: Double? = null,
    val notes: String? = null,
    val rawSummary: String = ""
)

object GeminiRecognitionService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val PROMPT = """
你是一個專業的台灣電動車（EV）充電資訊解析專家。
請仔細分析這張照片（可能是充電樁螢幕、充電App結算畫面、繳費發票、明細收據或車輛儀表板）。
請提取出充電與行駛的相關數值，並嚴格只輸出為以下結構的 JSON 格式，請勿輸出 Markdown 程式碼標記（如 ```json 等）或任何額外文字：
{
  "energyKwh": 充電充入度數(純數字例如 45.3，單位為 kWh，若找不到填 null),
  "cost": 充電費用金額(純數字例如 380，單位為新台幣TWD，若找不到或免費填 0 或 null),
  "operator": 充電營運商名稱(字串，如 U-POWER, 特爾電力, EVOASIS, iCharging, 華城 EVALUE, Tesla 超充, 星舟快充, 家用充電 等，若找不到填 null),
  "chargeType": 充電規格(字串 "DC" 或 "AC"，若為快充填 "DC"，若為慢充填 "AC"，若無法判斷填 "DC"),
  "startSocPercent": 充電前/起始電量百分比(整數 0-100，若找不到填 null),
  "endSocPercent": 充電後/結束/目前電量百分比(整數 0-100，若找不到填 null),
  "locationName": 充電站點名稱或地點地址(字串，若找不到填 null),
  "odometerKm": 儀表總行駛里程(純數字例如 32540，若找不到填 null),
  "notes": 額外補充備註(字串，例如充電時長、平均功率、槍號等，若無則填 "")
}
"""

    suspend fun recognizeImage(
        bitmap: Bitmap,
        apiKey: String
    ): Result<RecognizedChargingInfo> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("尚未設定 Gemini API Key，請至設定頁面輸入。")
            )
        }

        val base64Image = ImageUtils.toBase64Jpeg(bitmap)

        // Try primary model gemini-3.7-flash, fallback to gemini-3.6-flash and gemini-flash-latest
        val models = listOf("gemini-3.7-flash", "gemini-3.6-flash", "gemini-flash-latest")
        var lastException: Exception? = null

        for (model in models) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val result = callGeminiEndpoint(url, base64Image)
                if (result.isSuccess) {
                    return@withContext result
                } else {
                    lastException = result.exceptionOrNull() as? Exception
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        Result.failure(lastException ?: Exception("未能成功連線至 Gemini 辨識服務"))
    }

    private fun callGeminiEndpoint(url: String, base64Image: String): Result<RecognizedChargingInfo> {
        return try {
            val jsonPayload = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            // Text prompt
                            put(JSONObject().put("text", PROMPT.trim()))
                            // Inline image data
                            put(JSONObject().put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            }))
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("response_mime_type", "application/json")
                    put("thinking_config", JSONObject().apply {
                        put("thinking_budget", 0)
                    })
                })
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(response.code, responseBody)
                return Result.failure(Exception(errorMsg))
            }

            val parsedInfo = parseGeminiResponse(responseBody)
            Result.success(parsedInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(code: Int, body: String): String {
        return try {
            val json = JSONObject(body)
            val error = json.optJSONObject("error")
            val message = error?.optString("message") ?: body
            when (code) {
                400 -> "API 請求格式錯誤或無效的 API Key ($message)"
                403 -> "權限不足，請檢查 API Key 是否啟用 Gemini API ($message)"
                404 -> "找不到指定的 Gemini 模型 ($message)"
                429 -> "超過 Gemini API 配額使用限制，請稍候再試"
                else -> "Gemini API 錯誤 (HTTP $code): $message"
            }
        } catch (e: Exception) {
            "伺服器回應錯誤 (HTTP $code)"
        }
    }

    private fun parseGeminiResponse(responseBody: String): RecognizedChargingInfo {
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates")
            ?: throw IllegalStateException("Gemini 回應中無 candidates 內容")

        if (candidates.length() == 0) {
            throw IllegalStateException("Gemini 回應為空")
        }

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content")
            ?: throw IllegalStateException("無 content 節點")
        val parts = content.optJSONArray("parts")
            ?: throw IllegalStateException("無 parts 節點")
        val rawText = run {
            var found = ""
            for (i in 0 until parts.length()) {
                val p = parts.getJSONObject(i)
                if (p.has("text") && !p.optBoolean("thought", false)) {
                    val t = p.optString("text", "")
                    if (t.isNotBlank()) {
                        found = t
                        break
                    }
                }
            }
            if (found.isBlank()) {
                for (i in 0 until parts.length()) {
                    val t = parts.getJSONObject(i).optString("text", "")
                    if (t.isNotBlank()) {
                        found = t
                        break
                    }
                }
            }
            found
        }
        if (rawText.isBlank()) {
            throw IllegalStateException("未能解析出文字內容")
        }

        // Clean any accidental markdown quotes if present
        var cleanedText = rawText.trim()
        if (cleanedText.startsWith("```json")) {
            cleanedText = cleanedText.removePrefix("```json")
        } else if (cleanedText.startsWith("```")) {
            cleanedText = cleanedText.removePrefix("```")
        }
        if (cleanedText.endsWith("```")) {
            cleanedText = cleanedText.removeSuffix("```")
        }
        cleanedText = cleanedText.trim()

        val dataJson = JSONObject(cleanedText)

        val energyKwh = if (dataJson.has("energyKwh") && !dataJson.isNull("energyKwh")) {
            dataJson.optDouble("energyKwh").takeIf { !it.isNaN() && it > 0 }
        } else null

        val cost = if (dataJson.has("cost") && !dataJson.isNull("cost")) {
            dataJson.optDouble("cost").takeIf { !it.isNaN() && it >= 0 }
        } else null

        val operator = if (dataJson.has("operator") && !dataJson.isNull("operator")) {
            dataJson.optString("operator").trim().takeIf { it.isNotBlank() && it != "null" }
        } else null

        val chargeType = if (dataJson.has("chargeType") && !dataJson.isNull("chargeType")) {
            val ct = dataJson.optString("chargeType").trim().uppercase()
            if (ct == "AC") "AC" else "DC"
        } else null

        val startSocPercent = if (dataJson.has("startSocPercent") && !dataJson.isNull("startSocPercent")) {
            dataJson.optInt("startSocPercent").takeIf { it in 0..100 }
        } else null

        val endSocPercent = if (dataJson.has("endSocPercent") && !dataJson.isNull("endSocPercent")) {
            dataJson.optInt("endSocPercent").takeIf { it in 0..100 }
        } else null

        val locationName = if (dataJson.has("locationName") && !dataJson.isNull("locationName")) {
            dataJson.optString("locationName").trim().takeIf { it.isNotBlank() && it != "null" }
        } else null

        val odometerKm = if (dataJson.has("odometerKm") && !dataJson.isNull("odometerKm")) {
            dataJson.optDouble("odometerKm").takeIf { !it.isNaN() && it > 0 }
        } else null

        val notes = if (dataJson.has("notes") && !dataJson.isNull("notes")) {
            dataJson.optString("notes").trim().takeIf { it.isNotBlank() && it != "null" }
        } else null

        // Summary string for UI feedback
        val summaryParts = mutableListOf<String>()
        if (energyKwh != null) summaryParts.add("度數: ${energyKwh}kWh")
        if (cost != null) summaryParts.add("費用: $${cost.toInt()}")
        if (operator != null) summaryParts.add("營運商: $operator")
        if (chargeType != null) summaryParts.add("規格: $chargeType")
        if (endSocPercent != null) summaryParts.add("電量: $endSocPercent%")
        if (odometerKm != null) summaryParts.add("里程: ${odometerKm.toInt()}km")

        val summary = if (summaryParts.isNotEmpty()) {
            "已辨識：${summaryParts.joinToString(" · ")}"
        } else {
            "已分析圖片，但未能辨識出具體充電數據"
        }

        return RecognizedChargingInfo(
            energyKwh = energyKwh,
            cost = cost,
            operator = operator,
            chargeType = chargeType,
            startSocPercent = startSocPercent,
            endSocPercent = endSocPercent,
            locationName = locationName,
            odometerKm = odometerKm,
            notes = notes,
            rawSummary = summary
        )
    }
}
