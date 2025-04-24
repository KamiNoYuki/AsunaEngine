package com.sho.ss.asuna.engine.processor.ext

import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.constant.ErrorFlag
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor
import com.sho.ss.asuna.engine.utils.AESUtils

/**
 * @project  启源视频
 * @author   Sho Tan.
 * @e-mail   2943343823@qq.com
 * @created 2023/7/6 15:50:48
 * @description  樱花动漫2-https://www.857dmw.com
 **/
class YingHuaDongMan(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : CommonSecondaryPageProcessor(video, videoSource, episode, listener) {
    override fun handleVideoUrl(page: Page, videoUrl: String) {
        videoSource.extras.takeIf {
            whenNullNotifyFail(it, ErrorFlag.EXTRAS_MISSING, "源Extras缺失")
        }?.runCatching {
            val key = this["key"] as String
            val ivRule = this["ivRule"] as String
            //抽取iv值的正则表达式
            val ivRegex = this["ivRegex"] as String
            println("key: $key, ivRule: $ivRule, ivRegex: $ivRegex")
            if (whenNullNotifyFail(
                    key,
                    ErrorFlag.EXTRAS_MISSING,
                    "Extras@key缺失"
                ) && whenNullNotifyFail(
                    ivRule,
                    ErrorFlag.EXTRAS_MISSING,
                    "Extras@ivRule缺失"
                ) && whenNullNotifyFail(ivRegex, ErrorFlag.EXTRAS_MISSING, "Extras@ivRegex缺失")
            ) {
                `$`(ivRule, page.rawText).takeIf {
                    whenNullNotifyFail(it, ErrorFlag.NO_PARSED_DATA, "未解析到iv信息")
                }?.let { ivStr ->
                    println("ivStr: $ivStr")
                    //如果正则抽取值时出错会回调失败，因此此处无需考虑结果为空的情况
                    val iv = extractUrlWithRegex(ivStr, ivRegex)
                    if(!iv.isNullOrBlank()) {
                        println("iv: $iv")
                        AESUtils.decrypt(videoUrl, key, iv).takeIf {
                            whenNullNotifyFail(it,ErrorFlag.EXCEPTION_WHEN_PARSING,"视频链接解密失败")
                        }?.let {
                            println("decrypted url: $it")
                            super.handleVideoUrl(page, it)
                        }
                    }
                }
            }
        }?.onFailure {
            notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "解密链接时出错")
        }
    }
}