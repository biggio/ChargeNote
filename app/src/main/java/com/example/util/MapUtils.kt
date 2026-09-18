package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object MapUtils {

    /**
     * Opens the given location or GPS coordinates directly in Google Maps application or Web fallback.
     */
    fun openInGoogleMaps(
        context: Context,
        latitude: Double?,
        longitude: Double?,
        locationName: String? = null
    ) {
        try {
            val hasGps = latitude != null && longitude != null && latitude != 0.0 && longitude != 0.0
            val hasAddress = !locationName.isNullOrBlank()

            if (!hasGps && !hasAddress) {
                Toast.makeText(context, "無可用的 GPS 座標或地址資訊", Toast.LENGTH_SHORT).show()
                return
            }

            val queryLabel = if (hasAddress) Uri.encode(locationName) else Uri.encode("充電地點")
            
            // Prefer geo URI for native mapping apps
            val geoUri = if (hasGps) {
                Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($queryLabel)")
            } else {
                Uri.parse("geo:0,0?q=$queryLabel")
            }

            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            // Verify if Google Maps / map viewer can handle it
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                // Fallback to Google Maps Web search API
                val webUrl = if (hasGps) {
                    "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
                } else {
                    "https://www.google.com/maps/search/?api=1&query=$queryLabel"
                }
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "開啟地圖失敗：${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
