package com.fpz.videoplayer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fpz.videoplayer.player.IPlayerController

/** 平台相关的视频渲染画面（expect/actual 桥接，规则 1）。 */
@Composable
expect fun VideoSurface(controller: IPlayerController, modifier: Modifier)
