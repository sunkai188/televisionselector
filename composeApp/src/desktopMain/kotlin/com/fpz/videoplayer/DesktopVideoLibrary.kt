package com.fpz.videoplayer

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.awt.Canvas
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

class DesktopVideoLibrary : IVideoLibrary {

    private val factory = MediaPlayerFactory(
        "--no-video-title-show",
        "--avcodec-hw=any",
    )

    private val httpClient: HttpClient = HttpClient.newBuilder().build()
    private val serviceBase = "http://127.0.0.1:8765/cover"

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

    override suspend fun loadCover(video: VideoItem, strategy: CoverStrategy): ImageBitmap? =
        withContext(Dispatchers.IO) {
            // 先尝试 Python 侧车服务
            val fromService = runCatching {
                fetchCoverFromService(video.uri, strategy.wireName)?.toComposeImageBitmap()
            }.getOrNull()
            if (fromService != null) {
                fromService
            } else {
                // 服务未启动或失败时，回退到 VLCJ 抓帧（整帧）
                runCatching {
                    captureFrame(video.uri)?.toComposeImageBitmap()
                }.getOrNull()
            }
        }

    private fun fetchCoverFromService(uri: String, strategy: String): BufferedImage? {
        val encoded = URLEncoder.encode(uri, StandardCharsets.UTF_8)
        val url = URI.create("$serviceBase?path=$encoded&strategy=$strategy")
        val request = HttpRequest.newBuilder(url)
            .timeout(Duration.ofSeconds(120))
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() != 200) return null
        return ImageIO.read(ByteArrayInputStream(response.body()))
    }

    private fun captureFrame(uri: String): BufferedImage? {
        val player = factory.mediaPlayers().newEmbeddedMediaPlayer()
        return try {
            player.videoSurface().set(factory.videoSurfaces().newVideoSurface(Canvas()))

            val latch = CountDownLatch(1)
            player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
                override fun videoOutput(mediaPlayer: MediaPlayer, newCount: Int) {
                    if (newCount > 0) latch.countDown()
                }
            })

            player.media().prepare(uri)
            player.controls().play()
            player.controls().setTime(1000)

            if (latch.await(5, TimeUnit.SECONDS)) {
                player.snapshots().get(320, 0)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            player.release()
        }
    }
}

