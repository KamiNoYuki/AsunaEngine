package com.sho.ss.asuna.engine.processor.ext

import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.core.selector.Html
import com.sho.ss.asuna.engine.constant.ErrorFlag
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.base.BaseMultiPageProcessor.IParser
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor
import com.sho.ss.asuna.engine.utils.DecryptUtils
import com.sho.ss.asuna.engine.utils.JsonPathUtils

/**
 * @project  启源视频
 * @author   Sho Tan.
 * @e-mail   2943343823@qq.com
 * @created 2023/6/17 12:25:57
 * @description  达达龟-https://www.ddgys.com
 **/
class DaDaGuiExt(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : CommonSecondaryPageProcessor(video, videoSource, episode, listener) {


    override fun getPlayerPageTargetInstance() =
        IParser { page: Page, html: Html, _: String ->
            println("正在解析播放器页面视频信息：" + html.get())
            playerPageJsXpath.takeIf {
                whenNullNotifyFail(
                    it, ErrorFlag.RULE_MISSING,
                    "播放器Js规则缺失"
                )
            }?.let {
                handleVideoConfigJs(page, page.rawText)
            }
        }

    override fun handleVideoConfigJs(page: Page, videoConfigJs: String) {
        videoSource.playerJs.takeIf {
            whenNullNotifyFail(it, ErrorFlag.RULE_MISSING, "播放器解析规则缺失")
        }?.let { playerJsRule ->
            JsonPathUtils.selAsString(videoConfigJs, playerJsRule).takeIf {
                whenNullNotifyFail(
                    it,
                    ErrorFlag.EMPTY_VIDEO_URL,
                    "视频解析失败"
                ) && !it.isNullOrBlank() && !checkVideoUrl(page, it, false)
            }?.let { url ->
                //尝试解码
                decodingUrl(url).takeIf {
                    whenNullNotifyFail(
                        it,
                        ErrorFlag.EMPTY_VIDEO_URL,
                        "视频链接解码失败"
                    )
                }?.let {
                    handleVideoUrl(page, it)
                }
            }
        }
    }

    companion object {
        fun decodingUrl(url: String) =
            url.runCatching {
                val decodeStr = DecryptUtils.base64_decode(url)
                val l1 = mutableListOf(
                    'a', 'b', 'c', 'd', 'e', 'f',
                    'g', 'h', 'i', 'j', 'k', 'l',
                    'm', 'n', 'o', 'p', 'q', 'r',
                    's', 't', 'u', 'v', 'w', 'x',
                    'y', 'z'
                )
                val l2 = l1.asReversed()
                val builder = StringBuilder()
                decodeStr.forEach { item ->
                    val pos = l1.indexOf(item)
                    builder.append(if (pos != -1) l2[pos] else item)
                }
                builder.toString()
            }.getOrNull()
    }
}