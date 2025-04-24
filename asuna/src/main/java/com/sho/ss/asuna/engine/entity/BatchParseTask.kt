package com.sho.ss.asuna.engine.entity

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2024/6/25 10:47
 * @description
 **/
data class BatchParseTask(
    val video: Video,
    val tasks: MutableMap<Int, MutableSet<Episode>> = mutableMapOf()
) {
    val taskCount: Int
        get() = tasks.values.flatten().size
}