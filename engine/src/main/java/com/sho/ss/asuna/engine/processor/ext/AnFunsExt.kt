package com.sho.ss.asuna.engine.processor.ext

import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2025/1/21 14:35
 * @description AnFuns动漫的扩展处理器-https://www.anfuns.org
 **/
class AnFunsExt(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : CommonSecondaryPageProcessor(video, videoSource, episode, listener) {
    /**
     * 重写此方法以避免将空白符替换掉，否则会影响部分包含有空白符的视频链接导致无法播放。
     */
    override fun handleWhitespace(url: String): String = url
}