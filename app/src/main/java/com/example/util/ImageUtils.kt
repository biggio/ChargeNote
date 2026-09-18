package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

object ImageUtils {

    /**
     * Decode Bitmap from Uri, auto-rotate according to EXIF orientation,
     * and scale down so that the maximum dimension does not exceed maxDimension.
     */
    fun processImageUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1280
    ): Pair<Bitmap?, String?> {
        return try {
            val contentResolver = context.contentResolver

            // 1. Decode bounds first to check dimensions
            var inputStream: InputStream? = contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) {
                return Pair(null, "無法讀取圖片尺寸")
            }

            // 2. Calculate inSampleSize
            var sampleSize = 1
            val maxEdge = max(srcWidth, srcHeight)
            while ((maxEdge / sampleSize) > maxDimension * 2) {
                sampleSize *= 2
            }

            // 3. Decode actual bitmap
            inputStream = contentResolver.openInputStream(uri)
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decodedBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (decodedBitmap == null) {
                return Pair(null, "圖片解碼失敗")
            }

            // 4. Handle EXIF rotation
            var rotationDegrees = 0
            try {
                contentResolver.openInputStream(uri)?.use { exifStream ->
                    val exif = ExifInterface(exifStream)
                    rotationDegrees = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                }
            } catch (ignored: Exception) {}

            var transformedBitmap = decodedBitmap
            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                transformedBitmap = Bitmap.createBitmap(
                    decodedBitmap, 0, 0, decodedBitmap.width, decodedBitmap.height, matrix, true
                )
                if (transformedBitmap != decodedBitmap) {
                    decodedBitmap.recycle()
                }
            }

            // 5. Final scale down if still larger than maxDimension
            val finalMaxEdge = max(transformedBitmap.width, transformedBitmap.height)
            if (finalMaxEdge > maxDimension) {
                val scale = maxDimension.toFloat() / finalMaxEdge
                val finalWidth = (transformedBitmap.width * scale).toInt()
                val finalHeight = (transformedBitmap.height * scale).toInt()
                val scaled = Bitmap.createScaledBitmap(transformedBitmap, finalWidth, finalHeight, true)
                if (scaled != transformedBitmap) {
                    transformedBitmap.recycle()
                }
                transformedBitmap = scaled
            }

            Pair(transformedBitmap, null)
        } catch (e: Exception) {
            Pair(null, "處理圖片發生錯誤：${e.localizedMessage}")
        }
    }

    /**
     * Scale and prepare Bitmap directly (e.g. from camera thumbnail/preview)
     */
    fun processBitmap(bitmap: Bitmap, maxDimension: Int = 1280): Bitmap {
        val maxEdge = max(bitmap.width, bitmap.height)
        if (maxEdge <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / maxEdge
        val finalWidth = (bitmap.width * scale).toInt()
        val finalHeight = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    /**
     * Compress Bitmap to JPEG and encode to Base64
     */
    fun toBase64Jpeg(bitmap: Bitmap, quality: Int = 85): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
