package com.fpz.videoplayer

/** 人物分组。 */
data class PersonGroup(
    val id: Int,
    val repUri: String,
    val videos: List<VideoItem>,
)

/** 分析结果。 */
data class AnalysisResult(
    val duplicates: Set<String>,
    val personGroups: List<PersonGroup>,
    val uncategorized: List<VideoItem>,
)
