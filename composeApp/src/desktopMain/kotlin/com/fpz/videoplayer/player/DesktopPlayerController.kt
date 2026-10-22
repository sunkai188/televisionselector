package com.fpz.videoplayer.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent

/**
 * 桌面端播放器实现（规则 3）：使用 VLCJ 作为后端，Swing 组件渲染，并开启硬件解码。
 */
class DesktopPlayerController : IPlayerController {

    private val component = EmbeddedMediaPlayerComponent(
        "--no-video-title-show",
        "--avcodec-hw=any", // 开启硬件解码（规则 3 约束）
    )
    private val mediaPlayer: MediaPlayer = component.mediaPlayer()
    private var playWhenReady = false

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    /** 供 VideoSurface 通过 SwingPanel 嵌入渲染。 */
    val videoComponent: java.awt.Component get() = component

    init {
        mediaPlayer.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer) {
                _playbackState.value = PlaybackState.PLAYING
            }

            override fun paused(mediaPlayer: MediaPlayer) {
                _playbackState.value = PlaybackState.PAUSED
            }

            override fun buffering(mediaPlayer: MediaPlayer, newCache: Float) {
                _playbackState.value = PlaybackState.BUFFERING
            }

            override fun finished(mediaPlayer: MediaPlayer) {
                _playbackState.value = PlaybackState.ENDED
            }

            override fun stopped(mediaPlayer: MediaPlayer) {
                _playbackState.value = PlaybackState.IDLE
            }
        })
    }

    override suspend fun initialize() {
        // VLCJ 在构造时已完成初始化
    }

    override suspend fun load(uri: String) {
        mediaPlayer.media().prepare(uri)
    }

    override fun play() {
        playWhenReady = true
        mediaPlayer.controls().play()
    }

    override fun pause() {
        playWhenReady = false
        mediaPlayer.controls().pause()
    }

    override fun seekTo(positionMs: Long) {
        mediaPlayer.controls().setTime(positionMs)
    }

    override fun release() {
        component.release()
    }
}
