package com.sho.ss.asuna.engine.processor.ext

import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.core.Request
import com.sho.ss.asuna.engine.core.SpiderListener
import com.sho.ss.asuna.engine.core.model.HttpRequestBody
import com.sho.ss.asuna.engine.core.selector.Html
import com.sho.ss.asuna.engine.core.utils.HttpConstant
import com.sho.ss.asuna.engine.constant.ErrorFlag
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.common.BaseThirdLevelPageProcessor
import com.sho.ss.asuna.engine.utils.JsonPathUtils
import com.sho.ss.asuna.engine.utils.UserAgentLibrary
import java.nio.charset.StandardCharsets

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2024/2/4 22:11
 * @description 动漫巴士-网址发布页：http://dm84.site
 * @deprecated 使用SaoHuoExtNew
 **/
@Deprecated(message = "Use the SaoHuoExtNew to instead.")
class AnimBusExt(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : BaseThirdLevelPageProcessor(video, videoSource, episode, listener) {

    private val parseApi = videoSource.extras?.get("parseApi")
    private val videoUrlPath = videoSource.extras?.get("videoUrlPath")
    override fun handleVideoUrl(page: Page, videoUrl: String) {
        videoUrl.runCatching {
            val paramsRaw: String = videoUrl.substring(0, videoUrl.indexOf(";//") + 1)
            paramsRaw.split(";".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray().map { params ->
                    val param = params.split("=".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    val key = param[0].substring(param[0].indexOf("var") + 3)
                    val value =
                        param[1].substring(param[1].indexOf("\"") + 1, param[1].lastIndexOf("\""))
                    key to value
                }
        }.onSuccess {
            if (whenNullNotifyFail(parseApi, ErrorFlag.API_MISSING, "extras@parseApi缺失")) {
                request(Request(toAbsoluteUrl(page.url.get(), parseApi!!)).apply {
                    addHeader(HttpConstant.Header.USER_AGENT, UserAgentLibrary().USER_AGENT_CHROME1)
                    addHeader("Origin", getHostByUrl(page.url.get()))
                    addHeader(HttpConstant.Header.REFERER, page.url.get())
                    requestBody = HttpRequestBody.form(it.toMap().apply {
                        println("api params：$this")
                    }, StandardCharsets.UTF_8.name())
                    method = HttpConstant.Method.POST
                }, 1, object : SpiderListener {
                    override fun onError(request: Request?, e: Exception?) {
                        println("onError -> request：$request")
                        notifyOnFailed(
                            ErrorFlag.EXCEPTION_WHEN_PARSING,
                            e?.message ?: "视频解析请求出错啦"
                        )
                    }
                })
            }
        }.onFailure {
            notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, it.message)
        }
    }

    override fun doParseThirdPage(page: Page, html: Html, curPageUrl: String) {
        //{"code":200,"msg":"success","ext":"mp4","referer":"never","url":"https:\/\/sf1-ttcdn-tos.pstatp.com\/obj\/tos-cn-o-0000c0030\/3268bc61432d4408942e193fc462e91c"}
        page.rawText.takeIf {
            whenNullNotifyFail(it, ErrorFlag.DATA_INVALIDATE, "api无响应信息") &&
            whenNullNotifyFail(videoUrlPath, ErrorFlag.EXTRAS_MISSING, "extras@videoUrlPath缺失")
        }?.runCatching {
            JsonPathUtils.selAsString(this, videoUrlPath!!)
        }?.onSuccess { videoUrl ->
            takeIf {
                whenNullNotifyFail(videoUrl, ErrorFlag.EMPTY_VIDEO_URL, "视频解析失败")
            }?.notifyOnCompleted(toAbsoluteUrl(videoUrl!!))
        }?.onFailure {
            notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, it.message)
        }
    }
}