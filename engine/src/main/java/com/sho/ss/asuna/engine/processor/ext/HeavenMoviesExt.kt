package com.sho.ss.asuna.engine.processor.ext

import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.core.Request
import com.sho.ss.asuna.engine.core.SpiderListener
import com.sho.ss.asuna.engine.core.selector.Html
import com.sho.ss.asuna.engine.core.utils.HttpConstant
import com.sho.ss.asuna.engine.constant.ErrorFlag
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.common.BaseThirdLevelPageProcessor
import com.sho.ss.asuna.engine.utils.JsonPathUtils
import com.sho.ss.asuna.engine.utils.RegexHelper
import com.sho.ss.asuna.engine.utils.SpiderUtils
import com.sho.ss.asuna.engine.utils.UserAgentLibrary

/**
 * @project QiYuanVideo
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2025/3/7 21:49
 * @description 天堂电影网视频扩展处理器-https://tiantangdianyingw.com
 **/
class HeavenMoviesExt(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : BaseThirdLevelPageProcessor(video, videoSource, episode, listener) {
    private val parseApi by lazy { videoSource.extras?.get("parseApi") }
    private val urlRegex by lazy { videoSource.extras?.get("urlRegex") }
    private val timeRegex by lazy { videoSource.extras?.get("timeRegex") }
    private val vKeyRegex by lazy { videoSource.extras?.get("vKeyRegex") }
    private val keyRegex by lazy { videoSource.extras?.get("keyRegex") }
    private val resultPath by lazy { videoSource.extras?.get("resultPath") }

    override fun handleVideoUrl(page: Page, config: String) {
        if (parameterValidator(
                ErrorFlag.EXTRAS_MISSING,
                parseApi to "源extras@parseApi缺失",
                urlRegex to "源extras@urlRegex缺失",
                timeRegex to "源extras@timeRegex缺失",
                vKeyRegex to "源extras@vKeyRegex缺失",
                keyRegex to "源extras@keyRegex缺失",
            )
        ) {
            val extractFailCallback = { msg: String ->
                notifyOnFailed(ErrorFlag.REGEX_EXTRACT_ERROR, msg)
            }
            val url = RegexHelper.extractParamsWithRegex(config, urlRegex!!, extractFailCallback)
                ?.get("value")
            if (url.isNullOrEmpty()) return
            val time = RegexHelper.extractParamsWithRegex(config, timeRegex!!, extractFailCallback)
                ?.get("value")
            if (time.isNullOrEmpty()) return
            val vkey = RegexHelper.extractParamsWithRegex(config, vKeyRegex!!, extractFailCallback)
                ?.get("value")
            if (vkey.isNullOrEmpty()) return
            //key为空，但避免将来该值不为空需要增加适配，此处尝试抽取，没有则使用空字符串。
            val key = RegexHelper.extractParamsWithRegex(config, keyRegex!!)
                ?.get("value") ?: ""
//            println("api参数解析完毕：url:$url, time:$time, vkey:$vkey, key:$key")
            val params = mutableMapOf("url" to url, "time" to time, "vkey" to vkey, "key" to key)
            val request = Request(toAbsoluteUrl(page.url.get(), parseApi!!))
            SpiderUtils.initRequest(
                request,
                UserAgentLibrary().proxyUserAgent,
                page.url.get(),
                null,
                null
            )
            SpiderUtils.applyMethod(request, HttpConstant.Method.POST)
            SpiderUtils.buildRequestParams(request, params)
            val spider = SpiderUtils.buildSpider(this, request, 1)
            if (null != spider) {
                SpiderUtils.addListenerForSpider(spider, object : SpiderListener {
                    override fun onError(request: Request, e: Exception) {
                        e.printStackTrace()
                        notifyOnFailed(
                            ErrorFlag.EXCEPTION_WHEN_PARSING,
                            e.message ?: "视频解析失败啦"
                        )
                    }
                })
                spider.runAsync()
            }
        }
    }

    override fun doParseThirdPage(page: Page, html: Html, curPageUrl: String) {
//        println("接口响应数据：${page.rawText}")
        if (parameterValidator(ErrorFlag.EXTRAS_MISSING, resultPath to "extras@resultPath缺失") &&
            parameterValidator(ErrorFlag.NO_PARSED_DATA, page.rawText to "视频解析失败，接口无数据响应")) {
            val url = JsonPathUtils.selAsString(page.rawText, resultPath!!)
//            println("解析的视频链接：$url")
            if(parameterValidator(ErrorFlag.EMPTY_VIDEO_URL, url to "视频解析失败")) {
                notifyOnCompleted(url!!)
            }
        }
    }
}