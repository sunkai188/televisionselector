package com.fpz.videoplayer

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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
import java.security.MessageDigest
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
    private val serviceBase = "http://127.0.0.1:8765"

    /** 封面缓存目录（相对运行目录）。 */
    private val cacheDir = File("cover_cache").apply { mkdirs() }

    private val extensions = setOf(
        "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg", "ts", "3gp",
    )

    override suspend fun listVideos(directory: String): List<VideoItem> = withContext(Dispatchers.IO) {
        val dir = File(directory)
        if (!dir.isDirectory) return@withContext emptyList()
        dir.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in extensions }
            .sortedBy { it.name }
            .map { VideoItem(it.absolutePath, it.name) }
            .toList()
    }

    override suspend fun analyze(videos: List<VideoItem>): AnalysisResult = withContext(Dispatchers.IO) {
        if (videos.isEmpty()) return@withContext AnalysisResult(emptySet(), emptyList(), emptyList())
        val body = JSONObject().put("paths", JSONArray(videos.map { it.uri })).toString()
        val request = HttpRequest.newBuilder(URI.create("$serviceBase/analyze"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofMinutes(30))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = runCatching {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        }.getOrNull()
        if (response == null || response.statusCode() != 200) {
            return@withContext AnalysisResult(emptySet(), emptyList(), videos)
        }
        parseAnalysis(response.body(), videos)
    }

    private fun parseAnalysis(body: String, videos: List<VideoItem>): AnalysisResult {
        val uriToVideo = videos.associateBy { it.uri }
        return try {
            val root = JSONObject(body)
            val duplicates = mutableSetOf<String>()
            root.optJSONArray("duplicates")?.let { dupArr ->
                for (i in 0 until dupArr.length()) {
                    val pair = dupArr.optJSONArray(i) ?: continue
                    for (j in 0 until pair.length()) duplicates.add(pair.getString(j))
                }
            }
            val groups = mutableListOf<PersonGroup>()
            root.optJSONArray("persons")?.let { personsArr ->
                for (i in 0 until personsArr.length()) {
                    val obj = personsArr.optJSONObject(i) ?: continue
                    val id = obj.optInt("id", i)
                    val rep = obj.optString("rep", "")
                    val videoList = mutableListOf<VideoItem>()
                    obj.optJSONArray("videos")?.let { vArr ->
                        for (j in 0 until vArr.length()) {
                            uriToVideo[vArr.getString(j)]?.let { videoList.add(it) }
                        }
                    }
                    groups.add(PersonGroup(id, rep, videoList))
                }
            }
            val uncategorized = mutableListOf<VideoItem>()
            root.optJSONArray("uncategorized")?.let { uncatArr ->
                for (i in 0 until uncatArr.length()) {
                    uriToVideo[uncatArr.getString(i)]?.let { uncategorized.add(it) }
                }
            }
            AnalysisResult(duplicates, groups, uncategorized)
        } catch (e: Exception) {
            AnalysisResult(emptySet(), emptyList(), videos)
        }
    }

    override suspend fun loadCover(video: VideoItem, strategy: CoverStrategy): ImageBitmap? =
        withContext(Dispatchers.IO) {
            val cacheFile = cacheFileFor(video, strategy)
            // 1. 命中缓存直接读
            if (cacheFile.exists()) {
                return@withContext runCatching {
                    ImageIO.read(cacheFile)?.toComposeImageBitmap()
                }.getOrNull()
            }
            // 2. 提取（先服务，失败回退 VLCJ 抓帧）
            val image = runCatching {
                fetchCoverFromService(video.uri, strategy.wireName)
            }.getOrNull() ?: captureFrame(video.uri)
            // 3. 写入缓存
            if (image != null) {
                runCatching { ImageIO.write(image, "png", cacheFile) }
            }
            image?.toComposeImageBitmap()
        }

    private fun cacheFileFor(video: VideoItem, strategy: CoverStrategy): File {
        val key = video.uri + "|" + strategy.wireName
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return File(cacheDir, hash + ".png")
    }

    private fun fetchCoverFromService(uri: String, strategy: String): BufferedImage? {
        val encoded = URLEncoder.encode(uri, StandardCharsets.UTF_8)
        val url = URI.create("$serviceBase/cover?path=$encoded&strategy=$strategy")
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
        val canvas = Canvas()
        return try {
            // 关键：让 Canvas 拥有原生 peer，否则 VLC 无法渲染、videoOutput 不会触发
            canvas.addNotify()
            player.videoSurface().set(factory.videoSurfaces().newVideoSurface(canvas))

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

