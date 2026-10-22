package com.fpz.videoplayer

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.fpz.videoplayer.player.DesktopPlayerController
import com.fpz.videoplayer.player.IPlayerController

@Composable
actual fun VideoSurface(controller: IPlayerController, modifier: Modifier) {
    val component = (controller as? DesktopPlayerController)?.videoComponent
    if (component != null) {
        // 规则 3：用 SwingPanel 嵌入 VLCJ 渲染组件（禁 JavaFX）
        SwingPanel(
            modifier = modifier,
            factory = { component },
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("桌面端视频渲染不可用")
        }
    }
}
