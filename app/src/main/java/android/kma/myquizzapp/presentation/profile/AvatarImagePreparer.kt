package android.kma.myquizzapp.presentation.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.domain.profile.UpdateAvatarUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Android-only URI/bitmap boundary; no crop UI and no upload/network orchestration here. */
class AvatarImagePreparer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun prepare(uriString: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        var bitmap: Bitmap? = null
        try {
            val uri = Uri.parse(uriString)
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext unreadable()
            var sample = 1
            while (bounds.outWidth / sample > MAX_EDGE || bounds.outHeight / sample > MAX_EDGE) sample *= 2
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@withContext unreadable()
            bitmap = decoded
            ensureActive()
            val orientation = resolver.openInputStream(uri)?.use { stream ->
                try { ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
                catch (_: java.io.IOException) { ExifInterface.ORIENTATION_NORMAL }
            } ?: ExifInterface.ORIENTATION_NORMAL
            val oriented = orient(decoded, orientation)
            if (oriented !== decoded) decoded.recycle()
            bitmap = oriented
            // JPEG has no alpha. Composite transparent sources onto white rather than black.
            if (oriented.hasAlpha()) {
                val opaque = Bitmap.createBitmap(oriented.width, oriented.height, Bitmap.Config.ARGB_8888)
                Canvas(opaque).apply { drawColor(Color.WHITE); drawBitmap(oriented, 0f, 0f, null) }
                oriented.recycle()
                bitmap = opaque
            }
            while (true) {
                ensureActive()
                val current = checkNotNull(bitmap)
                for (quality in listOf(90, 80, 70, 60, 50, 40)) {
                    ensureActive()
                    val bytes = ByteArrayOutputStream().use { output ->
                        if (!current.compress(Bitmap.CompressFormat.JPEG, quality, output)) return@withContext unreadable()
                        output.toByteArray()
                    }
                    if (bytes.isNotEmpty() && bytes.size <= UpdateAvatarUseCase.MAX_BYTES) {
                        return@withContext Result.Success(bytes)
                    }
                }
                if (current.width <= 64 || current.height <= 64) {
                    return@withContext Result.Error(AppError.Api("FILE_TOO_LARGE"))
                }
                val smaller = Bitmap.createScaledBitmap(
                    current, (current.width * 0.75).toInt().coerceAtLeast(1),
                    (current.height * 0.75).toInt().coerceAtLeast(1), true
                )
                if (smaller !== current) current.recycle()
                bitmap = smaller
            }
            @Suppress("UNREACHABLE_CODE")
            unreadable()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            unreadable()
        } finally {
            bitmap?.takeUnless { it.isRecycled }?.recycle()
        }
    }

    private fun unreadable(): Result<ByteArray> = Result.Error(AppError.Api("CLIENT_IMAGE_UNREADABLE"))

    private fun orient(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(270f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
            }
        }
        return if (matrix.isIdentity) source else Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private companion object { const val MAX_EDGE = 1280 }
}
