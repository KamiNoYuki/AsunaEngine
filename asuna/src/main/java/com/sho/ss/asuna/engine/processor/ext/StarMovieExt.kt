package com.sho.ss.asuna.engine.processor.ext

import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.constant.ErrorFlag
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor
import com.sho.ss.asuna.engine.utils.AESUtils
import com.sho.ss.asuna.engine.utils.JsonPathUtils

/**
 * @project  启源视频
 * @author   Sho Tan.
 * @e-mail   2943343823@qq.com
 * @created 2023/7/6 11:19:33
 * @description  星影-https://www.xy1080.net
 **/
class StarMovieExt(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : CommonSecondaryPageProcessor(video, videoSource, episode, listener) {

    override fun handleVideoUrl(page: Page, videoUrl: String) {
        videoSource.extras.takeIf {
            whenNullNotifyFail(it, ErrorFlag.EXTRAS_MISSING, "源extras缺失!") &&
                    whenNullNotifyFail(
                        it!!["urlPath"] as String,
                        ErrorFlag.EXTRAS_MISSING,
                        "extras@urlPath缺失!"
                    ) &&
                    whenNullNotifyFail(
                        it["uidPath"] as String,
                        ErrorFlag.EXTRAS_MISSING,
                        "extras@uidPath缺失!"
                    ) &&
                    whenNullNotifyFail(it["key"] as String, ErrorFlag.EXTRAS_MISSING, "extras@key缺失!") &&
                    whenNullNotifyFail(it["iv"] as String, ErrorFlag.EXTRAS_MISSING, "extras@iv缺失!")
        }?.let {
            videoUrl.runCatching {
                val url = JsonPathUtils.selAsString(this, it["urlPath"] as String)
                val uid = JsonPathUtils.selAsString(this, it["uidPath"] as String)
                val key = (it["key"] as String).takeIf {
                    it.isNotBlank() && !uid.isNullOrBlank()
                }?.replace("{uid}", uid!!)
                val iv = (it["iv"] as String)
                println("url: $url \nuid: $uid, key: $key, iv: $iv")
                if (whenNullNotifyFail(
                        url,
                        ErrorFlag.EMPTY_VIDEO_URL,
                        "未解析到视频链接!"
                    ) &&
                    whenNullNotifyFail(
                        uid,
                        ErrorFlag.EXCEPTION_WHEN_PARSING,
                        "未解析到解密uid!"
                    )
                ) {
                    //尝试解密链接
                    AESUtils.decrypt(url!!, key!!, iv).takeIf {
                        whenNullNotifyFail(it, ErrorFlag.EMPTY_VIDEO_URL, "视频链接解密失败!")
                    }?.let {
                        println("decrypted url: $it")
                        super.handleVideoUrl(page, it)
                    }
                }
            }.onFailure {
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, it.message)
            }
        }
    }

}