package com.sho.ss.asuna.engine.processor.ext

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONException
import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.core.Request
import com.sho.ss.asuna.engine.core.Spider
import com.sho.ss.asuna.engine.core.SpiderListener
import com.sho.ss.asuna.engine.core.model.HttpRequestBody
import com.sho.ss.asuna.engine.core.utils.HttpConstant
import com.sho.ss.asuna.engine.constant.ErrorFlag
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.base.BaseMultiPageProcessor.IParser
import com.sho.ss.asuna.engine.utils.JsonPathUtils
import com.sho.ss.asuna.engine.utils.SpiderUtils
import com.sho.ss.asuna.engine.utils.UserAgentLibrary
import java.nio.charset.StandardCharsets

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2024/8/3 12:31
 * @description
 **/
class HaiWaiHuiJuExt(
    entity: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : PiPiXiaExt(entity, videoSource, episode, listener) {
    init {
        addParseTarget(getApiParseInstance())
    }

    override fun onWatchPageVideoLinkParse(page: Page, url: String) {

        if (whenNullNotifyFail(url, ErrorFlag.EPISODE_URL_INVALIDATE, "播放器参数无效")) {
            try {
                println("观看页面解析完毕:  $url")
                //视频信息序列化为json
                val json = JSON.parseObject(url)
                if (whenNullNotifyFail(json, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频参数无效!")) {
                    val from = json.getString("from")
                    var videoUrl = json.getString("url")
                    if (whenNullNotifyFail(
                            from,
                            ErrorFlag.EXCEPTION_WHEN_PARSING,
                            "from参数为空!"
                        ) && whenNullNotifyFail(videoUrl, ErrorFlag.EMPTY_VIDEO_URL, "url参数为空!")
                    ) {
                        //如果是可播放的直链，则不再向后解析
                        if (isAutoCheckVideoUrl && SpiderUtils.isVideoFileBySuffix(videoUrl)) {
                            videoUrl =
                                SpiderUtils.fixHostIfMissing(videoUrl, getHostByUrl(page.url.get()))
                            notifyOnCompleted(videoUrl)
                        } else {
                            //存放播放器地址的js文件链接
                            val playerUrl: String? = getUrlOfPlayerPage(page, videoUrl)
                            if (whenNullNotifyFail(
                                    playerUrl,
                                    ErrorFlag.API_MISSING,
                                    "接口配置链接无效"
                                )
                            ) {
                                val request = Request(toAbsoluteUrl(page.url.get(), playerUrl!!))
                                    .putExtra("from", from)
                                    .putExtra("videoUrl", videoUrl)
                                println("存放extras后的Request: $request")
                                val userAgent =
                                    SpiderUtils.checkUserAgent(videoSource.videoApiUa, videoSource)
                                SpiderUtils.initRequest(
                                    request,
                                    userAgent,
                                    null,
                                    videoSource.videoApiCk,
                                    videoSource.videoApiHd
                                )
                                SpiderUtils.applyMethod(request, videoSource.videoApiMd)
                                SpiderUtils.addRequestParamsForKeyword(
                                    playerUrl,
                                    true,
                                    request,
                                    videoSource.videoApiPm
                                )
                                SpiderUtils.addReferer(
                                    videoSource,
                                    request,
                                    videoSource.videoApiReferer,
                                    true
                                )
                                println("请求存放播放器Api文件Request = $request")
                                val spider = Spider.create(this)
                                    .thread(1)
                                    .addRequest(request)
                                SpiderUtils.addListenerForSpider(spider, object : SpiderListener {
                                    override fun onError(request: Request, e: java.lang.Exception) {
                                        notifyOnFailed(
                                            ErrorFlag.EXCEPTION_WHEN_PARSING,
                                            "播放器信息请求失败!"
                                        )
                                    }
                                })
                                spider.runAsync()
                            }
                        }
                    }
                }
            } catch (e: JSONException) {
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, e.message)
            }
        }
    }

    override fun handleVideoUrl(page: Page, url: String) {
        println("url参数：$url")
        if (SpiderUtils.isVideoFileBySuffix(url)) {
            notifyOnCompleted(toAbsoluteUrl(page.url.get(), url))
        } else {
            val apiUrl = getApiUrl()
            if (whenNullNotifyFail(apiUrl, ErrorFlag.EXTRAS_MISSING, "extras@api缺失")) {
                //补全url
                page.url.get().let {
                    it.substring(0, it.indexOf("?"))
                }.takeIf {
                    whenNullNotifyFail(it, ErrorFlag.EXCEPTION_WHEN_PARSING, "接口无效")
                }?.let {
                    val fullApiUrl = "$it$apiUrl"
                    println("补全api链接：$fullApiUrl")
                    val request = Request(fullApiUrl).apply {
                        method = HttpConstant.Method.POST
                        addHeader(
                            HttpConstant.Header.USER_AGENT,
                            videoSource.videoApiUa ?: UserAgentLibrary().proxyUserAgent
                        )
                        requestBody = HttpRequestBody.form(
                            mutableMapOf<String, Any>("url" to url),
                            StandardCharsets.UTF_8.name()
                        )
                    }
                    request(request, 1, object : SpiderListener {
                        override fun onError(request: Request?, e: Exception?) {
                            notifyOnFailed(ErrorFlag.ERROR_ON_REQUEST, e?.message ?: "请求接口解析时出错")
                        }
                    })
                }
            }
        }
    }

    private fun getApiUrl() = videoSource.extras?.get("api")

    private fun getApiParseInstance() =
        IParser { page, html, url ->
            parseApiData(page, page.rawText)
        }

    /**
     * 解析接口响应结果中的视频链接
     */
    private fun parseApiData(page: Page, data: String?) {
        //{
        // "code":200,
        // "msg":"解析成功!",
        // "type":"hls",
        // "url":"https://json.shtpin.com/v1/api/file/hls/proxy/aHR0cHM6Ly9zdXBlci5mZnp5LW9ubGluZTYuY29tLzIwMjQwNjA3LzMyNjEwXzlhYWI0YjZmL2luZGV4Lm0zdTg=.m3u8"
        //}
        println("接口响应信息：$data")
        if (whenNullNotifyFail(data, ErrorFlag.NO_PARSED_DATA, "接口无响应数据")) {
            val url = JsonPathUtils.selAsString(data!!, "$.url")
            println("解析url：$url")
            if (whenNullNotifyFail(url, ErrorFlag.EMPTY_VIDEO_URL, "视频解析失败")) {
                notifyOnCompleted(toAbsoluteUrl(page.url.get(), url!!))
            }
        }
    }
}