package com.sho.ss.asuna.engine.processor.ext

import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.constant.ErrorFlag
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor
import com.sho.ss.asuna.engine.utils.DecryptUtils
import com.sho.ss.asuna.engine.utils.SpiderUtils

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/10/17 21:57:07
 * @description 奈落影院-https://newfii.com/
 */
class NaiLuoYingYuanExt(
    entity: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : CommonSecondaryPageProcessor(entity, videoSource, episode, listener) {
    private val rc4Data by lazy {
        videoSource.extras.runCatching {
            this?.get("rc4Key") as String to get("rc4T")?.toInt()
        }.getOrElse {
            "202205051426239465" to 1
        }
    }

    /**
     * 该源需要rc4解密后播放
     *
     * @param page     page
     * @param videoUrl 视频解析参数
     */
    override fun handleVideoUrl(page: Page,videoUrl: String) {
        videoUrl.runCatching {
            if (!SpiderUtils.isVideoFileBySuffix(videoUrl)) {
                //尝试解密链接
                DecryptUtils.rc4(this, rc4Data.first, rc4Data.second ?: 1)
            } else videoUrl
        }.onSuccess {
            if(whenNullNotifyFail(it, ErrorFlag.EMPTY_VIDEO_URL, "视频解析失败！")) {
                super.handleVideoUrl(page, it!!)
            }
        }.onFailure {
            println("解密视频链接时出错: ${it.message}")
            notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING,"视频解密失败")
        }
    }
}