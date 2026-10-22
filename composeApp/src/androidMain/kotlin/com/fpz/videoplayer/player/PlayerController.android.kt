package com.fpz.videoplayer.player

import android.content.Context
import androidx.lifecycle.Lifecycle

private lateinit var appContext: Context
private var appLifecycle: Lifecycle? = null

/**
 * 在 Application / Activity 启动时注入平台上下文与生命周期，
 * 供 expect/actual 工厂在创建播放器时使用。
 */
fun initPlayerPlatform(context: Context, lifecycle: Lifecycle? = null) {
    appContext = context.applicationContext
    appLifecycle = lifecycle
}

actual fun createPlayerController(): IPlayerController =
    AndroidExoPlayerController(appContext, appLifecycle)
