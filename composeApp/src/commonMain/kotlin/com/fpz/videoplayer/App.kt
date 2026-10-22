package com.fpz.videoplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fpz.videoplayer.player.IPlayerController

@Composable
fun App(controller: IPlayerController) {
    val library = remember { createVideoLibrary() }
    var selected by remember { mutableStateOf<VideoItem?>(null) }

    LaunchedEffect(Unit) {
        controller.initialize()
    }

    val video = selected
    if (video == null) {
        VideoLibraryScreen(
            library = library,
            initialDirectory = defaultVideoDirectory(),
            onVideoClick = { selected = it },
        )
    } else {
        PlayerScreen(
            controller = controller,
            video = video,
            onBack = { selected = null },
        )
    }
}

@Composable
private fun PlayerScreen(
    controller: IPlayerController,
    video: VideoItem,
    onBack: () -> Unit,
) {
    val state by controller.playbackState.collectAsState()

    LaunchedEffect(video.uri) {
        controller.load(video.uri)
        controller.play()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onBack) { Text("返回") }
        }

        Spacer(Modifier.height(8.dp))

        VideoSurface(
            controller = controller,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )

        Spacer(Modifier.height(12.dp))

        Text(text = video.name, style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { controller.play() }) { Text("播放") }
            Button(onClick = { controller.pause() }) { Text("暂停") }
        }

        Spacer(Modifier.height(12.dp))

        Text(text = "播放状态：$state", style = MaterialTheme.typography.titleMedium)
    }
}

