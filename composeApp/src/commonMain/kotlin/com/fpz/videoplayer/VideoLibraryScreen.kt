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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun VideoLibraryScreen(
    library: IVideoLibrary,
    initialDirectory: String,
    onVideoClick: (VideoItem) -> Unit,
) {
    var directory by remember { mutableStateOf(initialDirectory) }
    var strategy by remember { mutableStateOf(CoverStrategy.LOW) }
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var analysis by remember { mutableStateOf<AnalysisResult?>(null) }
    var selectedGroup by remember { mutableStateOf<PersonGroup?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        StrategySelector(strategy) { strategy = it }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = directory,
                onValueChange = { directory = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("视频目录") },
            )
            Button(onClick = {
                scope.launch {
                    videos = library.listVideos(directory)
                    analysis = null
                    selectedGroup = null
                }
            }) { Text("检索") }
            Button(onClick = {
                scope.launch { if (videos.isNotEmpty()) analysis = library.analyze(videos) }
            }) { Text("分析") }
        }

        val result = analysis
        val group = selectedGroup
        when {
            group != null && result != null ->
                PersonVideoScreen(library, group, result.duplicates, strategy, onVideoClick) { selectedGroup = null }

            result != null ->
                PersonScreen(library, result, strategy) { selectedGroup = it }

            videos.isNotEmpty() ->
                VideoGrid(library, videos, emptySet(), strategy, onVideoClick)

            else ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("点「检索」搜索视频，再点「分析」按人物/重复分组", color = Color.Gray)
                }
        }
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
private fun PersonScreen(
    library: IVideoLibrary,
    result: AnalysisResult,
    strategy: CoverStrategy,
    onGroupClick: (PersonGroup) -> Unit,
) {
    val uncategorized = result.uncategorized
    val groups = if (uncategorized.isNotEmpty()) {
        result.personGroups + PersonGroup(-1, uncategorized.firstOrNull()?.uri ?: "", uncategorized)
    } else {
        result.personGroups
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(groups, key = { it.id }) { g ->
            val rep = g.videos.firstOrNull { it.uri == g.repUri } ?: g.videos.firstOrNull()
            if (rep != null) {
                val label = if (g.id == -1) "未分类(${g.videos.size})" else "人物${g.id + 1}(${g.videos.size})"
                ThumbnailCard(library, rep, strategy, false, label) { onGroupClick(g) }
            }
        }
    }
}

@Composable
private fun PersonVideoScreen(
    library: IVideoLibrary,
    group: PersonGroup,
    duplicates: Set<String>,
    strategy: CoverStrategy,
    onVideoClick: (VideoItem) -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onBack) { Text("返回") }
        }
        VideoGrid(library, group.videos, duplicates, strategy, onVideoClick)
    }
}

@Composable
private fun VideoGrid(
    library: IVideoLibrary,
    videos: List<VideoItem>,
    duplicates: Set<String>,
    strategy: CoverStrategy,
    onVideoClick: (VideoItem) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(videos, key = { it.uri }) { video ->
            ThumbnailCard(library, video, strategy, video.uri in duplicates, video.name) { onVideoClick(video) }
        }
    }
}

@Composable
private fun ThumbnailCard(
    library: IVideoLibrary,
    video: VideoItem,
    strategy: CoverStrategy,
    isDuplicate: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val thumbnail by produceState<ImageBitmap?>(initialValue = null, video, strategy) {
        value = library.loadCover(video, strategy)
    }

    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Box {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                val img = thumbnail
                if (img != null) {
                    Image(
                        bitmap = img,
                        contentDescription = label,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text("…", color = Color.White)
                }
            }
            // 绿色薄雾：重复/相似视频
            if (isDuplicate) {
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color(0x6600FF00)))
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(4.dp),
        )
    }
}

