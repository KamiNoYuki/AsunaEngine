package com.sho.ss.asuna.engine.interfaces

import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2024/6/28 9:13
 * @description 批处理任务停止事件监听器
 **/
interface StopParseBatchListener {
    fun onStopped(video: Video, nodePos: Int, ep: Episode)
    fun onStopFailed(video: Video, nodePos: Int, ep: Episode)
}