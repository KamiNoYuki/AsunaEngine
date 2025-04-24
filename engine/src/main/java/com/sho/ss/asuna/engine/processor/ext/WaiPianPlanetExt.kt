package com.sho.ss.asuna.engine.processor.ext

import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.VideoProcessor
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/10/6 23:01:25
 * @description 歪片星球-https://waipian9.com/
 */
class WaiPianPlanetExt(
    entity: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : CommonSecondaryPageProcessor(entity, videoSource, episode, listener) {

    override fun getNormalVideoProcessor(page: Page,listener: ParseListener<Episode>): VideoProcessor {
        return WaiPianNormalVideoProcessor(entity,videoSource,episode,listener)
    }
}