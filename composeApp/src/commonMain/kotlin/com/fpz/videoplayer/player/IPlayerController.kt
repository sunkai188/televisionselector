package com.fpz.videoplayer.player

import kotlinx.coroutines.flow.StateFlow

/** 统一的播放状态，UI 层只依赖此枚举（规则 1）。 */
enum class PlaybackState {
    IDLE,
    BUFFERING,
    READY,
    PLAYING,
    PAUSED,
    ENDED,
}

/**
 * 平台无关的播放器抽象接口。
 * UI 层（Compose）只能通过该接口交互，严禁直接引用 ExoPlayer / VLCJ 等平台类（规则 1）。
 */
interface IPlayerController {
    val playbackState: StateFlow<PlaybackState>

    suspend fun initialize()
    suspend fun load(uri: String)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun release()
}

/** 平台实现的工厂（expect/actual 桥接）。 */
expect fun createPlayerController(): IPlayerController
