package dev.ujhhgtg.wekit.utils

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.UUID
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.isRegularFile
import kotlin.io.path.outputStream

object TelegramStickerConverter {
    private const val TAG = "TelegramStickerConverter"
    private const val VIDEO_FPS = 15
    private const val MAX_VIDEO_FRAMES = 90
    private const val VIDEO_FRAME_BATCH_SIZE = 8

    fun tgsToGif(input: Path, output: Path): Result<Unit> = runCatching {
        output.parent?.createDirectories()
        tgsToGifNative(input.toString(), output.toString())?.let(::error)
        require(output.isRegularFile() && output.fileSize() > 0L) { "TGS 转换未生成 GIF" }
    }

    suspend fun webmToGif(input: Path, output: Path): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            output.parent?.createDirectories()
            val framesDir = output.resolveSibling("${output.fileName}.frames-${UUID.randomUUID()}")
                .also { it.createDirectories() }
            try {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(input.toString())
                    val durationMs = retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()
                        ?.takeIf { it > 0L }
                    val sourceFrameCount = retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)
                        ?.toIntOrNull()
                        ?.takeIf { it > 1 }
                    val captureFrameRate = retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                        ?.toDoubleOrNull()
                        ?.takeIf { it > 0.0 }

                    val indexedFrames = sourceFrameCount?.let { count ->
                        runCatching {
                            extractFramesByIndex(
                                retriever = retriever,
                                framesDir = framesDir,
                                sourceFrameCount = count,
                                durationMs = durationMs,
                                captureFrameRate = captureFrameRate,
                            )
                        }.onFailure {
                            if (it is CancellationException) throw it
                            WeLogger.w(TAG, "indexed WebM frame extraction failed: ${it.javaClass.simpleName}")
                        }.getOrNull()
                    }
                    val usedIndexedFrames = indexedFrames?.isAnimated == true
                    val frames = indexedFrames?.takeIf { it.isAnimated } ?: run {
                        clearFrameDirectory(framesDir)
                        extractFramesByTime(retriever, framesDir, durationMs)
                    }
                    require(frames.frameCount > 1) { "Telegram 视频表情只解码出一帧" }
                    require(frames.distinctFrameCount > 1) { "Telegram 视频表情解码结果没有动画" }
                    WeLogger.i(
                        TAG,
                        "WebM frames extracted: mode=${if (usedIndexedFrames) "index" else "time"}, " +
                                "source=${sourceFrameCount ?: -1}, output=${frames.frameCount}, " +
                                "distinct=${frames.distinctFrameCount}, durationMs=${durationMs ?: -1}",
                    )
                    pngFramesToGifNative(
                        framesDir.toString(),
                        output.toString(),
                        frames.delayMs,
                    )?.let(::error)
                } finally {
                    retriever.release()
                }
                require(output.isRegularFile() && output.fileSize() > 0L) { "视频表情转换未生成 GIF" }
            } finally {
                framesDir.toFile().deleteRecursively()
            }
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    private external fun tgsToGifNative(inputPath: String, outputPath: String): String?

    private external fun pngFramesToGifNative(
        framesDir: String,
        outputPath: String,
        delayMs: Int,
    ): String?

    private suspend fun extractFramesByIndex(
        retriever: MediaMetadataRetriever,
        framesDir: Path,
        sourceFrameCount: Int,
        durationMs: Long?,
        captureFrameRate: Double?,
    ): ExtractedVideoFrames {
        val sampleStep = ((sourceFrameCount + MAX_VIDEO_FRAMES - 1) / MAX_VIDEO_FRAMES)
            .coerceAtLeast(1)
        val digests = hashSetOf<String>()
        var written = 0
        var batchStart = 0
        while (batchStart < sourceFrameCount && written < MAX_VIDEO_FRAMES) {
            currentCoroutineContext().ensureActive()
            val batchSize = minOf(VIDEO_FRAME_BATCH_SIZE, sourceFrameCount - batchStart)
            val bitmaps = retriever.getFramesAtIndex(batchStart, batchSize)
            require(bitmaps.isNotEmpty()) { "无法按索引解码 Telegram 视频表情" }
            try {
                bitmaps.forEachIndexed { offset, bitmap ->
                    val sourceIndex = batchStart + offset
                    if (sourceIndex % sampleStep == 0 && written < MAX_VIDEO_FRAMES) {
                        digests += writeFrame(framesDir, written++, bitmap)
                    }
                }
            } finally {
                bitmaps.forEach { bitmap -> if (!bitmap.isRecycled) bitmap.recycle() }
            }
            batchStart += batchSize
        }
        val delayMs = when {
            durationMs != null -> (durationMs / written.coerceAtLeast(1)).toInt()
            captureFrameRate != null -> (1_000.0 * sampleStep / captureFrameRate).toInt()
            else -> 1_000 / VIDEO_FPS
        }.coerceAtLeast(1)
        return ExtractedVideoFrames(written, digests.size, delayMs)
    }

    private suspend fun extractFramesByTime(
        retriever: MediaMetadataRetriever,
        framesDir: Path,
        durationMs: Long?,
    ): ExtractedVideoFrames {
        val duration = durationMs ?: error("无法读取 Telegram 视频表情时长和帧数")
        val frameCount = ((duration * VIDEO_FPS + 999L) / 1_000L)
            .toInt()
            .coerceIn(2, MAX_VIDEO_FRAMES)
        val durationUs = duration * 1_000L
        val digests = hashSetOf<String>()
        repeat(frameCount) { index ->
            currentCoroutineContext().ensureActive()
            val timestampUs = ((index + 0.5) * durationUs / frameCount)
                .toLong()
                .coerceAtMost((durationUs - 1L).coerceAtLeast(0L))
            val bitmap = retriever.getFrameAtTime(
                timestampUs,
                MediaMetadataRetriever.OPTION_CLOSEST,
            ) ?: error("无法解码 Telegram 视频表情第 ${index + 1} 帧")
            try {
                digests += writeFrame(framesDir, index, bitmap)
            } finally {
                bitmap.recycle()
            }
        }
        return ExtractedVideoFrames(
            frameCount = frameCount,
            distinctFrameCount = digests.size,
            delayMs = (duration / frameCount).toInt().coerceAtLeast(1),
        )
    }

    private fun writeFrame(framesDir: Path, index: Int, bitmap: Bitmap): String {
        val framePath = framesDir / "%04d.png".format(index)
        framePath.outputStream().use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                "无法写入 Telegram 视频表情帧"
            }
        }
        val digest = MessageDigest.getInstance("SHA-256")
        framePath.inputStream().use { stream ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun clearFrameDirectory(framesDir: Path) {
        Files.newDirectoryStream(framesDir).use { paths ->
            paths.forEach { it.deleteIfExists() }
        }
    }

    private data class ExtractedVideoFrames(
        val frameCount: Int,
        val distinctFrameCount: Int,
        val delayMs: Int,
    ) {
        val isAnimated: Boolean get() = frameCount > 1 && distinctFrameCount > 1
    }
}
