package com.sho.ss.asuna.engine.processor.ext

import androidx.core.util.Pair
import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.core.Request
import com.sho.ss.asuna.engine.core.SpiderListener
import com.sho.ss.asuna.engine.core.selector.Html
import com.sho.ss.asuna.engine.constant.EngineConstant
import com.sho.ss.asuna.engine.constant.ErrorFlag
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.base.BaseMultiPageProcessor.IParser
import com.sho.ss.asuna.engine.utils.JsonPathUtils.selAsString
import com.sho.ss.asuna.engine.utils.SpiderUtils
import com.sho.ss.asuna.engine.utils.UserAgentLibrary
import java.net.URLDecoder

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2024/5/7 16:12
 * @description 孜然影视扩展爬虫处理器-https://zrys1.top
 **/
class ZiRanExt(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : NewVisionExt(video, videoSource, episode, listener) {

    private val iframeExtras by lazy { videoSource.extras?.get("iframe") }

    init {
        //添加一个解析器到接口解析器之后，用来解析iframe的src
        addParseTarget(2, getIFrameParser())
    }

    private fun getIFrameParser(): IParser =
        IParser { page, html, url ->
            //官源：
            //  观看界面 -> 接口配置js界面 ->
            //      播放器界面 -> 解析iframe@src ->
            //      iframe@src界面 -> Config信息 ->
            //      读取url信息 -> 解密url -> 回调播放
            //解析接口对应的界面并解析iframe的src进行访问
            parseIFrameSrc(page, html, url)
        }

    private fun parseIFrameSrc(page: Page, html: Html, url: String) {
        iframeExtras.takeIf {
            whenNullNotifyFail(it, ErrorFlag.EXTRAS_MISSING, "extras@iframe缺失")
        }?.runCatching {
            `$`(this, html)
        }?.onFailure {
            notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "视频src参数解析失败")
        }?.onSuccess {
            if(whenNullNotifyFail(it, ErrorFlag.EMPTY_VIDEO_URL, "视频src参数无效")) {
                println("iframe@src = $it")
                //请求iframe@src的页面
                Request(it).apply {
                    //同步Request的extras
                    extras = page.request.extras
                    SpiderUtils.initRequest(
                        this,
                        UserAgentLibrary().USER_AGENT_EDGE,
                        page.url.get(), null, null
                    )
                    request(this, 1, object : SpiderListener {
                        override fun onError(request: Request, e: Exception) {
                            e.printStackTrace()
                            notifyOnFailed(
                                ErrorFlag.EXCEPTION_WHEN_PARSING,
                                if (null == e.message) "视频解析失败" else e.message
                            )
                        }
                    })
                }
            }
        }
    }

    override fun checkVideoUrl(
        page: Page,
        extras: String,
        whenIsNotVideoUrlContinue: Boolean
    ): Boolean {
        val url = selAsString(extras, "$.url")
        if (!isNullStr(url) && url!!.contains("JT")) {
            url.runCatching {
                decodeUrlWithJtType(this)
            }.getOrNull()
                .takeUnless {
                    isNullStr(it)
                }?.runCatching {
                    if (startsWith("%"))
                        URLDecoder.decode(this, "UTF-8")
                    else
                        this
                }?.getOrNull()
                ?.takeIf {
                    //校验是否是视频链接
                    SpiderUtils.isVideoFileBySuffix(it)
                }?.let {
                    notifyOnCompleted(it)
                    return true
                }
        }
        return super.checkVideoUrl(page, extras, whenIsNotVideoUrlContinue)
    }

    override fun onWatchPageVideoLinkParse(page: Page, js: String) {
        if (whenNullNotifyFail(js, ErrorFlag.NO_PARSED_DATA, "未解析到视频信息")) {
            //需要解析的视频链接
            val urlParam = selAsString(js, "$.url")
            //解析接口名
            val apiName = selAsString(js, "$.from")
            if (whenNullNotifyFail(urlParam, ErrorFlag.NO_PARSED_DATA, "未解析到url参数") &&
                whenNullNotifyFail(
                    apiName,
                    ErrorFlag.NO_PARSED_DATA,
                    "未解析到from参数"
                ) && null != urlParam
            ) {
                val isVideoUrl = checkVideoUrl(page, js, false)
                if (!isVideoUrl) {
                    //尝试解码JT链接并对链接进行URLDecoder解码
                    urlParam.runCatching {
                        decodeUrlByType(urlParam, EngineConstant.UrlDecodeType.JT).let {
                            if(it.startsWith("%")) {
                                URLDecoder.decode(it, "UTF-8")
                            } else {
                                it
                            }
                        }
                    }.onFailure {
                        notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, it.message ?: "链接解码失败")
                    }.onSuccess {
                        if(whenNullNotifyFail(it, ErrorFlag.EMPTY_VIDEO_URL, "视频解析失败")) {
                            val request = Request(buildPlayerConfigApi(page))
                            SpiderUtils.initRequest(
                                request,
                                UserAgentLibrary().USER_AGENT_EDGE,
                                page.url.get(), null, null
                            )
                            request.putExtra(keyParams, Pair(it, apiName)
                            )
                            request(request, 2, object : SpiderListener {
                                override fun onError(request: Request, e: Exception) {
                                    e.printStackTrace()
                                    notifyOnFailed(
                                        ErrorFlag.EXCEPTION_WHEN_PARSING,
                                        if (null == e.message) "请求接口信息失败!" else e.message
                                    )
                                }
                            })
                        }
                    }
                }
            }
        }
    }


}