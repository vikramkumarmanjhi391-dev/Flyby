package com.example.animation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.LruCache

data class MediaValidationResult(
    val isValid: Boolean,
    val mimeType: String = "image/png",
    val displayName: String = "Custom Sticker",
    val fileSizeBytes: Long = 0L,
    val errorMessage: String? = null
)

object CustomMediaBitmapCache {

    private const val MAX_FILE_SIZE_BYTES = 12L * 1024L * 1024L // 12 MB limit
    private val supportedMimeTypes = setOf(
        "image/png",
        "image/webp",
        "image/gif",
        "image/jpeg"
    )

    private val bitmapCache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.allocationByteCount
        }
    }

    fun validateMediaUri(context: Context, uri: Uri): MediaValidationResult {
        return try {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri)?.lowercase() ?: "image/png"
            if (mimeType !in supportedMimeTypes) {
                return MediaValidationResult(
                    isValid = false,
                    mimeType = mimeType,
                    errorMessage = "Unsupported file format ($mimeType). Please select a PNG, WebP, or GIF sticker."
                )
            }

            var displayName = "Custom Drifter"
            var fileSize = 0L
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) {
                        displayName = cursor.getString(nameIdx)?.substringBeforeLast(".") ?: "Custom Drifter"
                    }
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                        fileSize = cursor.getLong(sizeIdx)
                    }
                }
            }

            if (fileSize > MAX_FILE_SIZE_BYTES) {
                val mb = fileSize / (1024f * 1024f)
                return MediaValidationResult(
                    isValid = false,
                    mimeType = mimeType,
                    fileSizeBytes = fileSize,
                    errorMessage = String.format("File is too large (%.1f MB). Maximum supported sticker size is 12 MB.", mb)
                )
            }

            // Decode bounds to verify image integrity
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return MediaValidationResult(
                    isValid = false,
                    mimeType = mimeType,
                    errorMessage = "Selected file appears corrupt or could not be decoded as an image."
                )
            }

            // Pre-warm bitmap into cache
            val decoded = loadOrGetBitmap(context, uri.toString())
            if (decoded == null) {
                return MediaValidationResult(
                    isValid = false,
                    mimeType = mimeType,
                    errorMessage = "Unable to decode image bitmap from selected URI."
                )
            }

            MediaValidationResult(
                isValid = true,
                mimeType = mimeType,
                displayName = displayName.take(28),
                fileSizeBytes = fileSize
            )
        } catch (e: SecurityException) {
            MediaValidationResult(
                isValid = false,
                errorMessage = "Permission denied reading the selected file. Please pick another image."
            )
        } catch (e: Exception) {
            MediaValidationResult(
                isValid = false,
                errorMessage = "Error validating media: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    fun getCachedBitmap(uriString: String?): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        val cached = bitmapCache.get(uriString)
        return if (cached != null && !cached.isRecycled) cached else null
    }

    fun loadOrGetBitmap(context: Context, uriString: String?): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        getCachedBitmap(uriString)?.let { return it }

        return try {
            val uri = Uri.parse(uriString)
            val resolver = context.contentResolver

            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

            val maxDimension = 512
            var sampleSize = 1
            while (
                boundsOptions.outWidth / sampleSize > maxDimension ||
                boundsOptions.outHeight / sampleSize > maxDimension
            ) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val bitmap = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }

            if (bitmap != null) {
                bitmapCache.put(uriString, bitmap)
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun evict(uriString: String?) {
        if (!uriString.isNullOrBlank()) {
            bitmapCache.remove(uriString)
        }
    }
}
