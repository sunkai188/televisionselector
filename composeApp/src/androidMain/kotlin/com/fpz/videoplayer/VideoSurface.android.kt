package com.fpz.videoplayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.fpz.videoplayer.player.AndroidExoPlayerController
import com.fpz.videoplayer.player.IPlayerController
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
actual fun VideoSurface(controller: IPlayerController, modifier: Modifier) {
    val exoController = controller as? AndroidExoPlayerController
    val player by (exoController?.playerState ?: MutableStateFlow<ExoPlayer?>(null)).collectAsState()

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
        update = { view ->
            view.player = player
        },
    )
}
