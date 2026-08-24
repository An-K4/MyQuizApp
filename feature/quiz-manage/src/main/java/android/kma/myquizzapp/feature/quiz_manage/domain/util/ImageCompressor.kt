package android.kma.myquizzapp.feature.quiz_manage.domain.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Resize + nén ảnh (JPEG) trước khi upload (N15) — bắt buộc vì backend giới hạn
 * MAX_FILE_SIZE = 2MB (storage.schema.ts) và ảnh chụp trực tiếp từ camera
 * thường vượt xa mữc này.
 *
 * Chiến lược: downscale theo cạnh dài nhất (MAX_DIMENSION) bằng inSampleSize
 * lúc decode (tránh OOM với ảnh gốc lớn), rồi nén JPEG giảm dần quality tới
 * khi nằm dưới MAX_BYTES.
 */
class ImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val MAX_DIMENSION = 1280
        private const val MAX_BYTES = 2 * 1024 * 1024 - 51_200 // dư ~50KB dưới mức backend cho phép
        private const val START_QUALITY = 90
        private const val QUALITY_STEP = 10
        private const val MIN_QUALITY = 40
    }

    /** @return bytes JPEG đã nén, hoặc null nếu không đọc/giải mã được ảnh từ [uri]. */
    fun compress(uri: Uri): ByteArray? {
        val bitmap = decodeDownscaled(uri) ?: return null
        try {
            var quality = START_QUALITY
            var bytes = bitmap.toJpegBytes(quality)
            while (bytes.size > MAX_BYTES && quality > MIN_QUALITY) {
                quality -= QUALITY_STEP
                bytes = bitmap.toJpegBytes(quality)
            }
            return bytes
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeDownscaled(uri: Uri): Bitmap? {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) return null

        var sampleSize = 1
        while (width / sampleSize > MAX_DIMENSION || height / sampleSize > MAX_DIMENSION) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
    }

    private fun Bitmap.toJpegBytes(quality: Int): ByteArray =
        ByteArrayOutputStream().use { stream ->
            compress(Bitmap.CompressFormat.JPEG, quality, stream)
            stream.toByteArray()
        }
}
