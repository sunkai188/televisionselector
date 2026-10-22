package com.fpz.videoplayer

/** 封面提取策略。 */
enum class CoverStrategy(val wireName: String, val label: String) {
    LOW("low", "低"),
    MEDIUM("mid", "中"),
    HIGH("high", "高"),
}
