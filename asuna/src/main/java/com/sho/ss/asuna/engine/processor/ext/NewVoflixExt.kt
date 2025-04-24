package com.sho.ss.asuna.engine.processor.ext

import com.google.gson.JsonParser
import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.core.Request
import com.sho.ss.asuna.engine.core.Spider
import com.sho.ss.asuna.engine.core.SpiderListener
import com.sho.ss.asuna.engine.core.selector.Html
import com.sho.ss.asuna.engine.constant.ErrorFlag
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.base.BaseMultiPageProcessor.IParser
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor
import com.sho.ss.asuna.engine.utils.SpiderUtils

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2024/10/2 20:44
 * @description Voflix的新版扩展处理器-https://www.voflix.fun
 * 观看界面 -> 接口JS配置文件获取解析接口 -> 视频播放界面解析播放链接
 **/
open class NewVoflixExt(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : CommonSecondaryPageProcessor(video, videoSource, episode, listener) {
    init {
        addParseTarget(1, getParsePlayerUrlConfigJsInstance())
    }

    override fun onWatchPageVideoLinkParse(page: Page, url: String) {
        if (whenNullNotifyFail(url, ErrorFlag.EPISODE_URL_INVALIDATE, "播放器参数无效")) {
            try {
                println("观看页面解析完毕:  $url")
                val json = JsonParser.parseString(url).asJsonObject
                //视频信息序列化为json
//                JSONObject json = JSON.parseObject(url);
                if (whenNullNotifyFail(json, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频参数无效")) {
                    val from = if (null == json["from"]) null else json["from"].asString
                    val videoUrl = if (null == json["url"]) null else json["url"].asString
                    if (whenNullNotifyFail(
                            from,
                            ErrorFlag.EXCEPTION_WHEN_PARSING,
                            "接口名为空"
                        ) && whenNullNotifyFail(
                            videoUrl,
                            ErrorFlag.EMPTY_VIDEO_URL,
                            "视频链接参数为空"
                        )
                    ) {
                        //存放播放器地址的js文件链接
                        val playerUrl: String? = getPlayerUrlConfigJsUrl(page, videoUrl!!, from!!)
                        if (whenNullNotifyFail(
                                playerUrl,
                                ErrorFlag.EPISODE_URL_INVALIDATE,
                                "接口配置链接无效"
                            )
                        ) {
                            val request = Request(playerUrl)
                                .putExtra("videoUrl", videoUrl) //访问播放器页面需要该参数
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
                                playerUrl!!,
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
                            Spider.create(this)
                                .thread(1)
                                .addRequest(request)
                                .apply {
                                    SpiderUtils.addListenerForSpider(this, object : SpiderListener{
                                        override fun onError(
                                            request: Request?,
                                            e: java.lang.Exception?
                                        ) {
                                            notifyOnFailed(ErrorFlag.ERROR_ON_REQUEST, e?.message ?: "视频数据解析失败")
                                        }
                                    })
                                }
                                .runAsync()
                        }
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, e.message)
            }
        }
    }

    private fun getPlayerUrlConfigJsUrl(page: Page, videoUrl: String, from: String) =
        super.getUrlOfPlayerPage(page, videoUrl)
            ?.replace("{from}", from)

    /**
     * 解析存放播放器链接的js文件
     * @return iParseTarget
     */
    private fun getParsePlayerUrlConfigJsInstance(): IParser {
        return IParser { page: Page, _: Html?, _: String? ->
            page.request.getExtra<String>("videoUrl").takeIf {
                parameterValidator(
                    ErrorFlag.EXTRAS_MISSING,
                    it to "page.extras@videoUrl参数缺失"
                )
            }?.let { videoUrl ->
                `$`("//iframe/@src", page.rawText).takeIf { url ->
                    whenNullNotifyFail(url, ErrorFlag.EXCEPTION_WHEN_PARSING, "未解析到接口信息")
                }?.runCatching {
                    substring(0, indexOf("'"))
                }?.onSuccess { parseApi ->
                    if(whenNullNotifyFail(parseApi, ErrorFlag.API_MISSING, "解析到无效的接口数据")) {
                        val relativeUrl = parseApi + videoUrl
                        println("解析出api：$relativeUrl")
                        handlePlayerUrl(page, relativeUrl)
                    }
                }?.onFailure {
                    notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, it.message)
                }
            }
        }
    }

    private fun handlePlayerUrl(page: Page, parseApi: String) {
        val api = toAbsoluteUrl(parseApi)
        println("播放器链接：$api")
        val request = Request(api)
        val userAgent = SpiderUtils.checkUserAgent(videoSource.videoApiUa, videoSource)
        SpiderUtils.initRequest(
            request,
            userAgent,
            null,
            videoSource.videoApiCk,
            videoSource.videoApiHd
        )
        SpiderUtils.applyMethod(request, videoSource.videoApiMd)
        SpiderUtils.addReferer(request, getHostByUrl(page.url.get()))
        println("播放器Request: $request")
        val spider = Spider.create(this)
            .thread(1)
            .addRequest(request)
        SpiderUtils.addListenerForSpider(spider, object : SpiderListener {
            override fun onError(request: Request, e: Exception) {
                notifyOnFailed(
                    ErrorFlag.EXCEPTION_WHEN_PARSING,
                    if (null == e.message) "视频解析请求失败" else e.message
                )
            }
        })
        spider.runAsync()
    }
}