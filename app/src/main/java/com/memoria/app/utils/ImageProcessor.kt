package com.memoria.app.utils

import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.memoria.app.data.model.ImageFormat
import com.memoria.app.data.model.ProcessingType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val TAG = "ImageProcessor"

object ImageProcessor {

    suspend fun processImage(
        context: Context,
        sourcePath: String,
        processingType: ProcessingType,
        outputFormat: ImageFormat,
        quality: Int = 85
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Processing: $processingType | Format: ${outputFormat.name} | Quality: $quality")
            val src = loadBitmapCorrected(sourcePath)
                ?: return@withContext Result.failure(Exception("No se pudo cargar la imagen"))

            val out  = applyProcessing(src, processingType)
            val path = saveImage(context, out, outputFormat, quality)

            Log.d(TAG, "Saved -> $path (${File(path).length() / 1024}KB)")
            Result.success(path)
        } catch (e: Exception) {
            Log.e(TAG, "Processing failed", e)
            Result.failure(e)
        }
    }

    private fun loadBitmapCorrected(path: String): Bitmap? {
        val raw = loadBitmapRaw(path) ?: return null

        return try {
            val exif    = ExifInterface(path)
            val orient  = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val matrix  = Matrix()
            when (orient) {
                ExifInterface.ORIENTATION_ROTATE_90       -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180      -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270      -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL   -> matrix.preScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE       -> { matrix.postRotate(90f); matrix.preScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSVERSE      -> { matrix.postRotate(270f); matrix.preScale(-1f, 1f) }
                else -> return raw
            }
            Log.d(TAG, "EXIF orientation=$orient — applying correction")
            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
        } catch (e: Exception) {
            Log.w(TAG, "Could not read EXIF, using raw bitmap", e)
            raw
        }
    }

    private fun loadBitmapRaw(path: String): Bitmap? = try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        var sample = 1
        while (opts.outWidth / sample > 1920 || opts.outHeight / sample > 1920) sample *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    } catch (e: Exception) {
        Log.e(TAG, "loadBitmapRaw failed: $path", e)
        null
    }

    fun applyProcessing(src: Bitmap, type: ProcessingType): Bitmap = when (type) {
        ProcessingType.NONE       -> src.copy(src.config ?: Bitmap.Config.ARGB_8888, false)
        ProcessingType.GRAYSCALE  -> colorMatrix(src, ColorMatrix().apply { setSaturation(0f) })
        ProcessingType.SEPIA      -> colorMatrix(src, ColorMatrix(floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f,     0f,     0f,     1f, 0f
        )))
        ProcessingType.VINTAGE    -> colorMatrix(src, ColorMatrix(floatArrayOf(
            1.1f,  0.05f, 0f,    0f, -10f,
            0.05f, 1.0f,  0.05f, 0f,  -5f,
            0f,    0f,    0.9f,  0f,   5f,
            0f,    0f,    0f,    1f,   0f
        )))
        ProcessingType.WARM       -> colorMatrix(src, ColorMatrix(floatArrayOf(
            1.2f, 0f,   0f,   0f,  15f,
            0f,   1.0f, 0f,   0f,   5f,
            0f,   0f,   0.8f, 0f, -10f,
            0f,   0f,   0f,   1f,   0f
        )))
        ProcessingType.COOL       -> colorMatrix(src, ColorMatrix(floatArrayOf(
            0.85f, 0f,    0f,   0f, -5f,
            0f,    0.95f, 0f,   0f,  5f,
            0f,    0f,    1.2f, 0f, 15f,
            0f,    0f,    0f,   1f,  0f
        )))
        ProcessingType.BRIGHTNESS -> colorMatrix(src, ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, 40f,
            0f, 1f, 0f, 0f, 40f,
            0f, 0f, 1f, 0f, 40f,
            0f, 0f, 0f, 1f,  0f
        )))
        ProcessingType.CONTRAST   -> colorMatrix(src, ColorMatrix(floatArrayOf(
            1.5f, 0f,   0f,   0f, -50f,
            0f,   1.5f, 0f,   0f, -50f,
            0f,   0f,   1.5f, 0f, -50f,
            0f,   0f,   0f,   1f,   0f
        )))
        ProcessingType.ROTATE_90  -> rotate(src, 90f)
        ProcessingType.FLIP_H     -> flip(src)
        ProcessingType.COMPRESS   -> scale(src, 0.5f)
    }

    private fun colorMatrix(src: Bitmap, matrix: ColorMatrix): Bitmap {
        val out    = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint  = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    private fun rotate(src: Bitmap, degrees: Float): Bitmap =
        Bitmap.createBitmap(src, 0, 0, src.width, src.height, Matrix().apply { postRotate(degrees) }, true)

    private fun flip(src: Bitmap): Bitmap =
        Bitmap.createBitmap(src, 0, 0, src.width, src.height, Matrix().apply { preScale(-1f, 1f) }, true)

    private fun scale(src: Bitmap, factor: Float): Bitmap =
        Bitmap.createScaledBitmap(src,
            (src.width * factor).toInt().coerceAtLeast(1),
            (src.height * factor).toInt().coerceAtLeast(1), true)


    fun saveImage(context: Context, bitmap: Bitmap, format: ImageFormat, quality: Int): String {
        val compFmt = when (format) {
            ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ImageFormat.PNG  -> Bitmap.CompressFormat.PNG
            ImageFormat.WEBP -> Bitmap.CompressFormat.WEBP_LOSSY
        }
        val dir  = File(context.filesDir, "processed").apply { mkdirs() }
        val file = File(dir, "processed_${System.currentTimeMillis()}.${format.ext}")
        FileOutputStream(file).use { bitmap.compress(compFmt, quality, it) }
        return file.absolutePath
    }

    fun getFileSize(path: String): Long = try { File(path).length() } catch (e: Exception) { 0L }

    fun getImageDimensions(path: String): Pair<Int, Int> {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        return Pair(opts.outWidth.coerceAtLeast(0), opts.outHeight.coerceAtLeast(0))
    }
}