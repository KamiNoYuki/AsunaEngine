package com.sho.ss.asuna.engine.processor.ext

import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.core.Request
import com.sho.ss.asuna.engine.core.SpiderListener
import com.sho.ss.asuna.engine.core.utils.HttpConstant
import com.sho.ss.asuna.engine.constant.ErrorFlag
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.base.BaseMultiPageProcessor.IParser
import com.sho.ss.asuna.engine.utils.SpiderUtils
import com.sho.ss.asuna.engine.utils.UserAgentLibrary

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2024/10/4 9:57
 * @description 囧次元视频扩展处理器-https://www.jiongciyuan.org/
 **/
class JiongCiYuanExt(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    l: ParseListener<Episode>
) : NewVoflixExt(video, videoSource, episode, l) {
    private val iframeSrc by lazy {
        videoSource.extras?.get("iframeSrc")
    }
    private val srcTransformer by lazy {
        videoSource.extras?.get("srcTransformer")
    }

    init {
        addParseTarget(2, playerIframeParseInstance())
    }

    private fun playerIframeParseInstance() =
        IParser { page, html, url ->
            if (whenNullNotifyFail(iframeSrc, ErrorFlag.EXTRAS_MISSING, "extras@iframSrc缺失")) {
                val src = `$`(iframeSrc, page.rawText)
                //若未接解析到iframe，尝试补救，直接解析url
                if (null != src) {
                    handleIframeSrc(page, src)
                } else {
                    playerPageTargetInstance.onPageReadied(page, html, url)
                }
            }
        }

    private fun handleIframeSrc(page: Page, src: String) {
        val fullUrl = toAbsoluteUrl(page.url.get(), applySrcTransformer(src))
        println("full iframeUrl: $fullUrl")
        request(Request(fullUrl).apply {
            SpiderUtils.addHeaders(
                this, mutableMapOf(
                    HttpConstant.Header.REFERER to page.url.get(),
                    HttpConstant.Header.USER_AGENT to UserAgentLibrary().USER_AGENT_CHROME1
                )
            )
        }, 1, object : SpiderListener {
            override fun onError(request: Request?, e: Exception?) {
                notifyOnFailed(ErrorFlag.ERROR_ON_REQUEST, e?.message ?: "视频解析失败")
            }
        })
    }

    private fun applySrcTransformer(src: String) =
        srcTransformer?.replace("{src}", src)
            ?.replace("{host}", videoSource.host) ?: src

    override fun handleVideoUrl(page: Page, videoUrl: String) {
        //RC4扩展处理器进行解密
        Rc4Ext(entity, videoSource, episode, listener!!).handleVideoUrl(page, videoUrl)
    }
}