package com.sho.ss.asuna.engine.interfaces

import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2024/6/23 10:51
 * @description 批量解析剧集播放链接的监听器
 **/
interface EpisodeParseBatchListener : ParseListener<MutableList<Episode>> {

    @Deprecated("Deprecated")
    override fun onCompleted(v: MutableList<Episode>) {

    }

    @Deprecated("Deprecated", replaceWith = ReplaceWith("use VideoParseBatchListener#onFailedBatch(Video, Int, MutableList<Episode>, Int, String) to instead."))
    override fun onFail(flag: Int, errMsg: String?) {

    }

    @Deprecated("Deprecated")
    override fun onStarted() {

    }

    /**
     * 批量解析开始回调
     */
    fun onStartBatch(video: Video, nodeIndex: Int, episodes: MutableList<Episode>)

    /**
     * 在单个解析开始时回调
     */
    fun onTargetStart(video: Video, nodeIndex: Int, ep: Episode)

    /**
     * 发生错误时回调，与[@link onFailedBatch(Video, Int, MutableList<Episode>, Int, String)]的区别在于，该方法是在单个解析错误时回调，而onFailedBatch是所有任务出错时回调
     */
    fun onTargetError(video: Video?, nodeIndex: Int?, ep: Episode?, flag: Int, msg: String)

    /**
     * 单个任务解析完成时回调
     * @param video Video
     * @param nodeIndex 节点下标
     * @param ep 解析完成的剧集
     */
    fun onTargetComplete(video: Video, nodeIndex: Int, ep: Episode)

    /**
     * 批量处理完成
     * @param video 完成的Video
     * @param nodeIndex 节点下标
     * @param episodes 完成的剧集列表
     */
    fun onCompletedBatch(video: Video, nodeIndex: Int, episodes: MutableList<Episode>)

    /**
     * 批量解析全部失败
     * @param video Video
     * @param nodeIndex 节点下标
     * @param episodes 剧集列表
     * @param flag 错误标志
     * @param msg 错误信息
     */
    fun onFailedBatch(video: Video, nodeIndex: Int, episodes: MutableList<Episode>, flag: Int, msg: String)
}