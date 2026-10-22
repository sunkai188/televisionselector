package com.fpz.videoplayer.player

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Android 端播放器实现（规则 2）：
 * - 使用 Media3 ExoPlayer 作为播放后端；
 * - 通过 MediaCodecSelector 强制优先走 MediaCodec 硬件加速；
 * - 通过 DefaultLifecycleObserver 处理 Android 生命周期（onPause 暂停 / onResume 恢复 / onDestroy 释放）。
 */
class AndroidExoPlayerController(
    private val context: Context,
    lifecycle: Lifecycle? = null,
) : IPlayerController, DefaultLifecycleObserver {

    private var player: ExoPlayer? = null
    private var playWhenReady = false

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _playerState = MutableStateFlow<ExoPlayer?>(null)
    val playerState: StateFlow<ExoPlayer?> = _playerState.asStateFlow()

    init {
        lifecycle?.addObserver(this)
    }

    /** 异步初始化：引擎实例化放到 Dispatchers.IO，避免冷启动卡顿（性能清单）。 */
    override suspend fun initialize() {
        if (player != null) return
        val built = withContext(Dispatchers.IO) {
            val renderersFactory = DefaultRenderersFactory(context).apply {
                setEnableDecoderFallback(true)
                setMediaCodecSelector(HARDWARE_FIRST_SELECTOR)
            }
            ExoPlayer.Builder(context, renderersFactory).build()
        }
        player = built
        _playerState.value = built
        built.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                _playbackState.value = playbackState.toDomain()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) _playbackState.value = PlaybackState.PLAYING
            }
        })
    }

    override suspend fun load(uri: String) {
        player?.setMediaItem(MediaItem.fromUri(uri))
        player?.prepare()
    }

    override fun play() {
        playWhenReady = true
        player?.play()
    }

    override fun pause() {
        playWhenReady = false
        player?.pause()
    }

    override fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }

    override fun release() {
        player?.release()
        player = null
        _playerState.value = null
    }

    // 生命周期回调（Android 特有）
    override fun onPause(owner: LifecycleOwner) {
        player?.pause()
    }

    override fun onResume(owner: LifecycleOwner) {
        if (playWhenReady) player?.play()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        release()
    }

    private fun Int.toDomain(): PlaybackState = when (this) {
        Player.STATE_IDLE -> PlaybackState.IDLE
        Player.STATE_BUFFERING -> PlaybackState.BUFFERING
        Player.STATE_READY -> PlaybackState.READY
        Player.STATE_ENDED -> PlaybackState.ENDED
        else -> PlaybackState.IDLE
    }

    companion object {
        /** MediaCodec 硬解优先选择器：把 hardwareAccelerated=true 的解码器排到最前。 */
        private val HARDWARE_FIRST_SELECTOR =
            MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                MediaCodecUtil.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
                    .sortedByDescending { it.hardwareAccelerated }
            }
    }
}
