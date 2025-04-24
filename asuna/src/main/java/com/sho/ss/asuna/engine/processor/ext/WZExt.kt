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
 * @project  启源视频
 * @author   Sho Tan.
 * @e-mail   2943343823@qq.com
 * @created 2023/6/15 17:18:55
 * @description  WZ影视-https://wzyshi.com/
 **/
class WZExt(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : CommonSecondaryPageProcessor(video, videoSource, episode, listener)  {

    init {
        addParseTarget(1, getParsePlayerUrlConfigJsInstance())
        autoDecodeUrlOnWatchPage = false
    }

    /**
     * 解析存放播放器接口的js文件
     * @return iParseTarget
     */
    private fun getParsePlayerUrlConfigJsInstance(): IParser {
        return IParser { page: Page, html: Html, _: String ->
            val videoUrl = page.request.getExtra<String>("videoUrl")
            val from = page.request.getExtra<String>("from")
            val id = page.request.getExtra<String>("id")
            if (whenNullNotifyFail(videoUrl, ErrorFlag.EMPTY_VIDEO_URL, "视频url参数缺失")) {
                //Js文件内容如下:
                //MacPlayer.Html = '<iframe border="0" src="https://vip.mhyyy.com/?url=' + MacPlayer.PlayUrl + '&next=' + (!MacPlayer.PlayLinkNext ? '' : window.location.protocol + '//' + window.location.host + MacPlayer.PlayLinkNext) + '&title=' + document.title.split("-")[0] + '" width="100%" height="100%" marginWidth="0" frameSpacing="0" allowfullscreen="true" marginHeight="0" frameBorder="0" scrolling="no" vspale="0" noResize></iframe>';
                //MacPlayer.Show();
                var src = `$`("//iframe/@src", html)
                println("播放器配置src：$src")
                if (whenNullNotifyFail(
                        src,
                        ErrorFlag.EXCEPTION_WHEN_PARSING,
                        "接口信息无效!"
                    ) && null != src
                ) {
                    //截取src中的播放器接口
                    try {
                        if(src.startsWith("/addons/dp/player/index.php")) {
                            src = "/addons/dp/player/dp.php?key=&from=&id=&api=&url="
                        }
                        if(src.contains("?")) {
                            src = src.substring(0, src.indexOf("?")+1)
                        }
                        //播放器链接
                        val api = toAbsoluteUrl(page.url.get(),"${src}key=0&from=$from&id=$id&api=&url=$videoUrl")
                        println("播放器页面链接：$api")
                        if (whenNullNotifyFail(api, ErrorFlag.API_MISSING, "播放器Api无效!")) {
                            handlePlayerUrl(api)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, e.message)
                    }
                }
            }
        }
    }

    private fun handlePlayerUrl(api: String) {
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
        SpiderUtils.addReferer(videoSource, request, videoSource.videoApiReferer, true)
        println("播放器Request：$request")
        Spider.create(this).apply {
            SpiderUtils.addListenerForSpider(this,object : SpiderListener{
                override fun onError(request: Request, e: java.lang.Exception) {
                    notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING,e.message)
                }
            })
        }
            .thread(1)
            .addRequest(request)
            .runAsync()
    }

    /**
     * 观看页面数据解析完毕
     * @param page
     * @param url 通常来说(可播放的直链不应使用该处理器，而是[com.sho.ss.asuna.engine.processor.VideoProcessor])，该链接不是可供播放的链接，而是播放器页面的链接参数
     */
    override fun onWatchPageVideoLinkParse(page: Page, url: String) {
        if (whenNullNotifyFail(url, ErrorFlag.EPISODE_URL_INVALIDATE, "播放器参数无效")) {
            try {
                println("观看页面解析完毕:  $url")
                //视频信息序列化为json
                val json = JsonParser.parseString(url).asJsonObject
                if (whenNullNotifyFail(json, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频参数无效!")) {
                    val from = json.get("from").asString
                    var videoUrl = json.get("url").asString
                    val id = json.get("id").asString
                    if (whenNullNotifyFail(
                            from,
                            ErrorFlag.EXCEPTION_WHEN_PARSING,
                            "from参数为空!"
                        ) && whenNullNotifyFail(videoUrl, ErrorFlag.EMPTY_VIDEO_URL, "url参数为空!")
                    ) {
                        //解码url
                        videoUrl = transcoding(decodeUrlByType(videoUrl))
                        println("解码后的视频链接: $videoUrl")
                        //如果是可播放的直链，则不再向后解析
                        if (isAutoCheckVideoUrl && SpiderUtils.isVideoFileBySuffix(videoUrl)) {
                            videoUrl =
                                SpiderUtils.fixHostIfMissing(videoUrl, getHostByUrl(page.url.get()))
                            notifyOnCompleted(videoUrl)
                        } else {
                            //存放播放器地址的js文件链接
                            val playerUrl = getPlayerUrlConfigJsUrl(page, videoUrl, from)
                            if (whenNullNotifyFail(
                                    playerUrl,
                                    ErrorFlag.EPISODE_URL_INVALIDATE,
                                    "接口配置链接无效"
                                )
                            ) {
                                val request = Request(playerUrl)
                                    .putExtra("id",id)
                                    .putExtra("from",from)
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
//                                println("请求存放播放器Api文件Request = $request")
                                Spider.create(this)
                                    .thread(1)
                                    .addRequest(request)
                                    .runAsync()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, e.message)
            }
        }
    }

    private fun getPlayerUrlConfigJsUrl(page: Page, url: String, from: String): String? {
        return super.getUrlOfPlayerPage(page, url)
            ?.replace("{from}", from)
    }

}