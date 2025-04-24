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
import com.sho.ss.asuna.engine.utils.DecryptUtils
import com.sho.ss.asuna.engine.utils.JsonPathUtils
import com.sho.ss.asuna.engine.utils.UserAgentLibrary
import java.nio.charset.StandardCharsets

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2024/2/3 15:44
 * @description 克拉TV-https://www.kelatv.com
 **/
class KeLaTvExt(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : BaseThirdLevelPageProcessor(video, videoSource, episode, listener) {

    private val token = videoSource.extras?.get("token")
    private val parseApi = videoSource.extras?.get("parseApi")
    private val videoUrlPath = videoSource.extras?.get("videoUrlPath")

    override fun onWatchPageVideoLinkParse(page: Page, url: String) {
        if (whenNullNotifyFail(token, ErrorFlag.EXTRAS_MISSING, "extras@token缺失")) {
            buildPlayerUrl(url, token!!).takeUnless {
                it.isNullOrBlank()
            }?.let {
                println("build param: $it")
                super.onWatchPageVideoLinkParse(page, it)
            }
        }
    }

    private fun buildPlayerUrl(json: String, token: String): String? {
        val url = JsonPathUtils.selAsString(json, "$.url")
        val name = JsonPathUtils.selAsString(json, "$.name")
        val time = JsonPathUtils.selAsString(json, "$.time")
        if (whenNullNotifyFail(url, ErrorFlag.EXCEPTION_WHEN_PARSING, "参数url获取失败") &&
            whenNullNotifyFail(name, ErrorFlag.EXCEPTION_WHEN_PARSING, "参数name获取失败") &&
            whenNullNotifyFail(time, ErrorFlag.EXCEPTION_WHEN_PARSING, "参数time获取失败")
        ) {
            val sign = buildSign(url!!, time!!, token)
            if (whenNullNotifyFail(sign, ErrorFlag.EXCEPTION_WHEN_PARSING, "构造sign失败")) {
                return "?v=$url&name=$name&sign=$sign&time=$time"
            }
        }
        return null
    }

    override fun handleVideoUrl(page: Page, params: MutableMap<String, String>) {
        //RegexHelper.extractParamsWithRegex(jsCode, "'(\\w+)':\\s*'([^']+)'")?.let {
        //                println(it.toString())
        //            }
        if (whenNullNotifyFail(params, ErrorFlag.DATA_INVALIDATE, "params接收失败") &&
            whenNullNotifyFail(parseApi, ErrorFlag.EXTRAS_MISSING, "parseApi缺失")
        ) {
            request(Request(toAbsoluteUrl(parseApi!!)).apply {
                method = HttpConstant.Method.POST
                requestBody = HttpRequestBody.form(
                    params as Map<String, Any>,
                    StandardCharsets.UTF_8.name()
                )
                addHeader(HttpConstant.Header.USER_AGENT, UserAgentLibrary().proxyUserAgent)
                addHeader("Origin", getHostByUrl(page.url.get()) ?: videoSource.host)
            }, 1, object : SpiderListener {
                override fun onError(request: Request?, e: Exception?) {
                    notifyOnFailed(
                        ErrorFlag.EXCEPTION_WHEN_PARSING,
                        e?.message ?: "发送解析请求失败"
                    )
                }
            })
        }
    }

    @Deprecated("deprecated")
    override fun handleVideoUrl(page: Page, videoUrl: String) {

    }

    override fun doParseThirdPage(page: Page, html: Html, curPageUrl: String) {
        //{
        //    "code": "200",
        //    "success": "1",
        //    "Player": "dplayer",
        //    "playsUrl": "videoUrl"
        //}
        page.rawText.takeIf {
            whenNullNotifyFail(it, ErrorFlag.NO_PARSED_DATA, "接口无数据响应") &&
                    whenNullNotifyFail(
                        videoUrlPath,
                        ErrorFlag.EXTRAS_MISSING,
                        "extras@videoUrlPath缺失"
                    )
        }?.runCatching {
            JsonPathUtils.selAsString(this, videoUrlPath!!)
        }?.onSuccess { url ->
            url.takeIf {
                whenNullNotifyFail(it, ErrorFlag.VIDEO_INVALIDATE, "视频解析失败")
            }?.let {
                notifyOnCompleted(toAbsoluteUrl(it))
            }
        }?.onFailure {
            notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, it.message)
        }
    }

    //token: pSj5EWI5
    private fun buildSign(url: String, time: String, token: String) =
        DecryptUtils.toMd5(url + time + token)
}