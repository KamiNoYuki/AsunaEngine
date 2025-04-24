package com.sho.ss.asuna.engine.processor.ext

import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.constant.ErrorFlag
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor
import com.sho.ss.asuna.engine.utils.DecryptUtils

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2024/2/2 15:41
 * @description 该处理器主要用于处理需要rc4解密的视频源，且为二级常规源
 **/
open class Rc4Ext(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : CommonSecondaryPageProcessor(video, videoSource, episode, listener) {

    private var rc4Key = videoSource.extras?.get("rc4Key")

    /**
     * 执行解密前是否进行base64解码，true则解码再解密，false则解码再进行base64编码
     * 可缺省，缺省默认为true
     */
    private var isBase64Dec = videoSource.extras?.get("base64Dec")?.toBoolean() ?: true

    public override fun handleVideoUrl(page: Page, videoUrl: String) {
        println("raw视频链接：$videoUrl")
        videoUrl.takeUnless {
            checkVideoUrl(page, it, false)
        }?.takeIf {
            whenNullNotifyFail(rc4Key, ErrorFlag.EXTRAS_MISSING, "extras@rc4Key缺失")
        }?.runCatching {
            DecryptUtils.rc4(videoUrl, rc4Key, isBase64Dec)
        }?.onSuccess {
            if(whenNullNotifyFail(it, ErrorFlag.EMPTY_VIDEO_URL, "视频解析失败")) {
                super.handleVideoUrl(page, it!!)
            }
        }?.onFailure {
            notifyOnFailed(ErrorFlag.DECRYPT_ERROR, it.message ?: "rc4解密失败")
        }
    }
}