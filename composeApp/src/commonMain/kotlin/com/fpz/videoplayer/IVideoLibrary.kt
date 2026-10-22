package com.fpz.videoplayer

import androidx.compose.ui.graphics.ImageBitmap

/** 视频库接口（平台隔离）。 */
interface IVideoLibrary {
    /** 递归列出目录（含子目录）下的视频文件。 */
    suspend fun listVideos(directory: String): List<VideoItem>

    /** 批量分析：视频哈希（重复检测）+ 人物嵌入（分组），结果缓存。 */
    suspend fun analyze(videos: List<VideoItem>): AnalysisResult

    /** 按策略生成视频封面（裁剪后的人脸头像）；失败返回 null。 */
    suspend fun loadCover(video: VideoItem, strategy: CoverStrategy): ImageBitmap?
}


