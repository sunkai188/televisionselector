package com.fpz.videoplayer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun VideoLibraryScreen(
    library: IVideoLibrary,
    initialDirectory: String,
    onVideoClick: (VideoItem) -> Unit,
) {
    var directory by remember { mutableStateOf(initialDirectory) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var strategy by remember { mutableStateOf(CoverStrategy.LOW) }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        StrategySelector(strategy = strategy, onSelect = { strategy = it })

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = directory,
                onValueChange = { directory = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("视频目录") },
            )
            Button(onClick = { refreshKey++ }) { Text("刷新") }
        }

        VideoGrid(
            library = library,
            directory = directory,
            refreshKey = refreshKey,
            strategy = strategy,
            onVideoClick = onVideoClick,
        )
    }
}

@Composable
private fun StrategySelector(strategy: CoverStrategy, onSelect: (CoverStrategy) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("封面算法：", style = MaterialTheme.typography.bodyMedium)
        CoverStrategy.entries.forEach { s ->
            if (s == strategy) {
                Button(onClick = { onSelect(s) }) { Text(s.label) }
            } else {
                OutlinedButton(onClick = { onSelect(s) }) { Text(s.label) }
            }
        }
    }
}

@Composable
private fun VideoGrid(
    library: IVideoLibrary,
    directory: String,
    refreshKey: Int,
    strategy: CoverStrategy,
    onVideoClick: (VideoItem) -> Unit,
) {
    val videos by produceState<List<VideoItem>>(initialValue = emptyList(), directory, refreshKey) {
        value = library.listVideos(directory)
    }

    if (videos.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("未找到视频（目录：$directory）", color = Color.Gray)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(videos, key = { it.uri }) { video ->
            VideoCard(
                library = library,
                video = video,
                strategy = strategy,
                onClick = { onVideoClick(video) },
            )
        }
    }
}

@Composable
private fun VideoCard(
    library: IVideoLibrary,
    video: VideoItem,
    strategy: CoverStrategy,
    onClick: () -> Unit,
) {
    val thumbnail by produceState<ImageBitmap?>(initialValue = null, video, strategy) {
        value = library.loadCover(video, strategy)
    }

    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            val img = thumbnail
            if (img != null) {
                Image(
                    bitmap = img,
                    contentDescription = video.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text("…", color = Color.White)
            }
        }
        Text(
            text = video.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(4.dp),
        )
    }
}

