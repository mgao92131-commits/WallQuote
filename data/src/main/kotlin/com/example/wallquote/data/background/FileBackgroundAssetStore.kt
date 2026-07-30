package com.example.wallquote.data.background

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.wallquote.domain.background.BackgroundAssetId
import com.example.wallquote.domain.background.BackgroundAssetStore
import com.example.wallquote.domain.background.BackgroundLimits
import com.example.wallquote.domain.background.StagedBackgroundAsset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class BackgroundImportException(message: String, cause: Throwable? = null) : Exception(message, cause)

@Singleton
class FileBackgroundAssetStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : BackgroundAssetStore {

    private val formalDir: File
        get() = File(context.filesDir, FORMAL_DIR).also { it.mkdirs() }

    private val stagingDir: File
        get() = File(context.cacheDir, STAGING_DIR).also { it.mkdirs() }

    override suspend fun importToStaging(
        sourceUri: String,
        draftId: String,
    ): StagedBackgroundAsset = withContext(Dispatchers.IO) {
        val uri = Uri.parse(sourceUri)
        val mime = context.contentResolver.getType(uri)
        if (mime == null || mime !in SUPPORTED_MIME) {
            throw BackgroundImportException("unsupported_mime")
        }
        val token = "draft_${UUID.randomUUID()}"
        val tempFile = File(stagingDir, "$token.tmp")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            } ?: throw BackgroundImportException("open_failed")

            if (tempFile.length() <= 0L) {
                throw BackgroundImportException("empty_file")
            }

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(tempFile.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                throw BackgroundImportException("undecodable")
            }
            val pixels = bounds.outWidth.toLong() * bounds.outHeight.toLong()
            if (pixels > BackgroundLimits.MAX_DECODE_PIXELS * 16L) {
                // Extremely large sources still allowed as files; decode will subsample later.
            }

            // Smoke-decode a tiny sample to catch truncated payloads.
            val sample = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = largestSampleSize(bounds.outWidth, bounds.outHeight, 64, 64)
            }
            val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath, sample)
                ?: throw BackgroundImportException("undecodable")
            bitmap.recycle()

            StagedBackgroundAsset(draftId = draftId, stagingToken = token)
        } catch (error: BackgroundImportException) {
            tempFile.delete()
            throw error
        } catch (error: Throwable) {
            tempFile.delete()
            throw BackgroundImportException("import_failed", error)
        }
    }

    override suspend fun commit(stagedAsset: StagedBackgroundAsset): BackgroundAssetId =
        withContext(Dispatchers.IO) {
            val staged = stagingFile(stagedAsset.stagingToken)
            if (!staged.exists()) throw BackgroundImportException("staging_missing")
            val assetId = "bg_${UUID.randomUUID().toString().replace("-", "")}"
            val target = File(formalDir, "$assetId.jpg")
            // Normalize to JPEG with EXIF orientation baked in for stable wallpaper decode.
            decodeAndWriteNormalized(staged, target)
            staged.delete()
            BackgroundAssetId(assetId)
        }

    override suspend fun discardStaging(stagedAsset: StagedBackgroundAsset) =
        withContext(Dispatchers.IO) {
            stagingFile(stagedAsset.stagingToken).delete()
            Unit
        }

    override suspend fun delete(assetId: BackgroundAssetId) = withContext(Dispatchers.IO) {
        resolveFile(assetId)?.delete()
        Unit
    }

    override suspend fun exists(assetId: BackgroundAssetId): Boolean = withContext(Dispatchers.IO) {
        resolveFile(assetId)?.exists() == true
    }

    override suspend fun resolvePath(assetId: BackgroundAssetId): String? = withContext(Dispatchers.IO) {
        resolveFile(assetId)?.takeIf { it.exists() }?.absolutePath
    }

    override suspend fun resolveStagingPath(stagedAsset: StagedBackgroundAsset): String? =
        withContext(Dispatchers.IO) {
            stagingFile(stagedAsset.stagingToken).takeIf { it.exists() }?.absolutePath
        }

    override suspend fun cleanupOrphans(referencedAssetIds: Set<String>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        formalDir.listFiles()?.forEach { file ->
            val id = file.nameWithoutExtension
            if (id !in referencedAssetIds && now - file.lastModified() > BackgroundLimits.ORPHAN_ASSET_RETENTION_MILLIS) {
                file.delete()
            }
        }
        stagingDir.listFiles()?.forEach { file ->
            if (now - file.lastModified() > BackgroundLimits.STAGING_RETENTION_MILLIS) {
                file.delete()
            }
        }
        Unit
    }

    fun stagingFileForTests(token: String): File = stagingFile(token)

    fun formalFileForTests(assetId: String): File = File(formalDir, "$assetId.jpg")

    private fun stagingFile(token: String): File = File(stagingDir, "$token.tmp")

    private fun resolveFile(assetId: BackgroundAssetId): File? {
        val id = assetId.value.trim()
        if (id.isEmpty()) return null
        val jpg = File(formalDir, "$id.jpg")
        if (jpg.exists()) return jpg
        val png = File(formalDir, "$id.png")
        if (png.exists()) return png
        val webp = File(formalDir, "$id.webp")
        if (webp.exists()) return webp
        return null
    }

    private fun decodeAndWriteNormalized(source: File, target: File) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        val sampleSize = largestSampleSize(
            bounds.outWidth,
            bounds.outHeight,
            BackgroundLimits.MAX_DECODE_EDGE_PX,
            BackgroundLimits.MAX_DECODE_EDGE_PX,
        )
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(source.absolutePath, options)
            ?: throw BackgroundImportException("undecodable")
        val oriented = applyExifOrientation(source, decoded)
        try {
            FileOutputStream(target).use { out ->
                if (!oriented.compress(Bitmap.CompressFormat.JPEG, 92, out)) {
                    throw BackgroundImportException("compress_failed")
                }
            }
        } finally {
            if (oriented !== decoded) decoded.recycle()
            oriented.recycle()
        }
    }

    private fun applyExifOrientation(source: File, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            ExifInterface(source.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = android.graphics.Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    companion object {
        const val FORMAL_DIR = "backgrounds"
        const val STAGING_DIR = "background_staging"

        private val SUPPORTED_MIME = setOf(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
        )

        fun largestSampleSize(
            width: Int,
            height: Int,
            targetWidth: Int,
            targetHeight: Int,
        ): Int {
            var sample = 1
            if (width <= 0 || height <= 0) return 1
            while (
                width / (sample * 2) >= targetWidth &&
                height / (sample * 2) >= targetHeight
            ) {
                sample *= 2
            }
            // Also respect max pixels.
            while (
                (width / sample).toLong() * (height / sample) > BackgroundLimits.MAX_DECODE_PIXELS &&
                sample < 64
            ) {
                sample *= 2
            }
            return sample.coerceAtLeast(1)
        }
    }
}
