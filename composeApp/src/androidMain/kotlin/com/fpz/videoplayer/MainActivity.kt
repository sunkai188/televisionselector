package com.fpz.videoplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.fpz.videoplayer.player.createPlayerController
import com.fpz.videoplayer.player.initPlayerPlatform

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initPlayerPlatform(this, lifecycle)
        val controller = createPlayerController()
        setContent {
            App(controller)
        }
    }
}
