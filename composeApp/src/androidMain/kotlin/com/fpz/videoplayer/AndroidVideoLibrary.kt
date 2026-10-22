package com.fpz.videoplayer

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AndroidVideoLibrary : IVideoLibrary {

    private val extensions = setOf(
        "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg", "ts", "3gp",
    )

    override suspend fun listVideos(directory: String): List<VideoItem> = withContext(Dispatchers.IO) {
        val dir = File(directory)
        if (!dir.isDirectory) return@withContext emptyList()
        dir.listFiles { f -> f.isFile && f.extension.lowercase() in extensions }
            ?.sortedBy { it.name }
            ?.map { VideoItem(it.absolutePath, it.name) }
            ?: emptyList()
    }

    override suspend fun loadCover(video: VideoItem, strategy: CoverStrategy): ImageBitmap? = withContext(Dispatchers.IO) {
        // Android 端暂未接入 ML 封面，忽略 strategy，用 MediaMetadataRetriever 抓帧
        runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(video.uri)
                val frame: Bitmap? = retriever.getFrameAtTime(1_000_000) // 1 秒处，单位微秒
                frame?.asImageBitmap()
            } finally {
                retriever.release()
            }
        }.getOrNull()
    }
}
