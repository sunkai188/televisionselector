package com.fpz.videoplayer

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.fpz.videoplayer.player.createPlayerController

fun main() = application {
    val controller = remember { createPlayerController() }
    Window(
        onCloseRequest = {
            controller.release()
            exitApplication()
        },
        title = "FPZ",
    ) {
        App(controller)
    }
}
