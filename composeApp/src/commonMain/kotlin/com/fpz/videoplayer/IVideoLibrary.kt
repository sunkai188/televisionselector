package com.fpz.videoplayer

import androidx.compose.ui.graphics.ImageBitmap

/** 视频库接口（平台隔离）。 */
interface IVideoLibrary {
    /** 列出目录下的视频文件。 */
    suspend fun listVideos(directory: String): List<VideoItem>

    /** 按策略生成视频封面（裁剪后的人脸头像）；失败返回 null。 */
    suspend fun loadCover(video: VideoItem, strategy: CoverStrategy): ImageBitmap?
}

