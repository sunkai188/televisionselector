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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fpz.videoplayer.player.IPlayerController
import kotlinx.coroutines.launch

private const val DEMO_URL = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

@Composable
fun App(controller: IPlayerController) {
    val state by controller.playbackState.collectAsState()
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(DEMO_URL) }

    LaunchedEffect(Unit) {
        controller.initialize()
        controller.load(DEMO_URL)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VideoSurface(
            controller = controller,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("视频地址") },
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { scope.launch { controller.load(url) } }) { Text("加载") }
            Button(onClick = { controller.play() }) { Text("播放") }
            Button(onClick = { controller.pause() }) { Text("暂停") }
        }

        Spacer(Modifier.height(12.dp))

        Text(text = "播放状态：$state", style = MaterialTheme.typography.titleMedium)
    }
}
