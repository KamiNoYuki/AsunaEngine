package com.sho.ss.asuna.engine.processor.ext

import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.VideoProcessor
import com.sho.ss.asuna.engine.utils.StringUtils

/**
 * @project  启源视频
 * @author   Sho Tan.
 * @e-mail   2943343823@qq.com
 * @created 2023/6/19 12:30:52
 * @description 源歪片星球的观看页视频链接中有可能包含unicode和urlEncode混编导致无法解码。
 **/
class WaiPianNormalVideoProcessor(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
): VideoProcessor(video, videoSource, episode, listener) {
    override fun postPlayUrl(page: Page, videoUrl: String) {
        val newUrl = videoUrl.replace("%u","\\u")
        super.postPlayUrl(page, StringUtils.unicodeDecode(newUrl) ?: newUrl)
    }
}