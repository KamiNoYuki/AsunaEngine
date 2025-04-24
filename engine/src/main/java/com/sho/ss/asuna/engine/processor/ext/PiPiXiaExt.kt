package com.sho.ss.asuna.engine.processor.ext

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONException
import com.alibaba.fastjson2.JSONPath
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
import com.sho.ss.asuna.engine.utils.AESUtils
import com.sho.ss.asuna.engine.utils.JsonPathUtils
import com.sho.ss.asuna.engine.utils.SpiderUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2024/8/2 21:41
 * @description 皮皮虾影视扩展处理器-http://www.ppxys.vip
 **/
open class PiPiXiaExt(
    entity: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : CommonSecondaryPageProcessor(entity, videoSource, episode, listener) {

    private val key by lazy { videoSource.extras?.get("key") }

    private val iv by lazy { videoSource.extras?.get("iv") }

    private val uid by lazy { videoSource.extras?.get("uid") }

    private val urlPath by lazy { videoSource.extras?.get("urlPath") }

    init {
        addParseTarget(1, getParsePlayerUrlConfigJsInstance())
    }

    /**
     * 解析存放播放器接口的js文件
     *
     * @return iParseTarget
     */
    private fun getParsePlayerUrlConfigJsInstance(): IParser {
        return IParser { page: Page, html: Html, _: String ->
            val videoUrl = page.request.getExtra<String?>("videoUrl")
            val from = page.request.getExtra<String?>("from")
            if (whenNullNotifyFail(
                    videoUrl,
                    ErrorFlag.EMPTY_VIDEO_URL,
                    "视频url参数缺失"
                ) && whenNullNotifyFail(from, ErrorFlag.API_MISSING, "播放器接口未知")
            ) {
                val xml = html.get()
                if (whenNullNotifyFail(
                        xml,
                        ErrorFlag.EXCEPTION_WHEN_PARSING,
                        "空的播放器接口配置"
                    )
                ) {
                    val config =
                        extractUrlWithSubstring(xml, "player_list=", ",MacPlayerConfig")
                    if (whenNullNotifyFail(
                            config,
                            ErrorFlag.EXCEPTION_WHEN_PARSING,
                            "未解析到播放器配置"
                        )
                    ) {
                        //播放器接口信息
                        val api = JSONPath.eval(
                            JSONPath.eval(
                                config,
                                "$.$from"
                            ), "$.parse"
                        ) as? String?
                        if (whenNullNotifyFail(
                                api,
                                ErrorFlag.EXCEPTION_WHEN_PARSING,
                                "播放器接口无效!"
                            ) && null != api
                        ) {
                            //拼接为完整的链接
                            val playerUrl = api + videoUrl
                            if (whenNullNotifyFail(
                                    playerUrl,
                                    ErrorFlag.EXCEPTION_WHEN_PARSING,
                                    "播放器链接无效"
                                )
                            ) {
                                handlePlayerUrl(playerUrl)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handlePlayerUrl(api: String) {
        var mApi = api.replace("&amp;","&")
        mApi = toAbsoluteUrl(mApi)
        println("播放器链接：$mApi")
        val request = Request(mApi)
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
        val spider = Spider.create(this)
            .thread(1)
            .addRequest(request)
        SpiderUtils.addListenerForSpider(spider, object : SpiderListener {
            override fun onError(request: Request, e: Exception) {
                System.err.println("播放器解析请求失败: " + e.message)
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "播放器解析请求失败")
            }
        })
        spider.runAsync()
    }

    override fun getNormalVideoProcessor(
        page: Page,
        listener: ParseListener<Episode>
    ) = object : VideoCopyrightChecker(entity, videoSource, episode, listener) {
        override fun getCopyrightTipTagRule() = "//div[contains(text(),'应版权方')]"
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
                        //解码url
                        try {
                            val decodedUrl = decodeUrlWithJtType(videoUrl)
                            println("尝试解密Jt格式的加密链接后：$decodedUrl")
                            videoUrl = transcoding(decodedUrl)
                            println("解码后的视频链接：$videoUrl")
                        } catch (e: java.lang.Exception) {
                            System.err.println("failed to try decode: " + e.message)
                        }
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

    override fun getUrlOfPlayerPage(page: Page, fakeVideoUrl: String): String? {
        return super.getUrlOfPlayerPage(page, fakeVideoUrl)?.replace(
            "{time}",
            SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(
                Date(
                    System.currentTimeMillis()
                )
            )
        )
    }

    override fun handleVideoUrl(page: Page, url: String) {
        if (SpiderUtils.isVideoFileBySuffix(url)) {
            println("是视频直链：$url")
            notifyOnCompleted(toAbsoluteUrl(page.url.get(), url))
        } else {
            //尝试解密
            if (parameterValidator(
                    ErrorFlag.EXTRAS_MISSING,
                    key to "extras@key缺失",
                    iv to "extras@iv缺失",
                    uid to "extras@uid缺失",
                    urlPath to "extras@urlPath缺失"
                )
            ) {
                println("Try to decrypt the url: $url")
                val uid = JsonPathUtils.selAsString(url, uid!!)
                val rawVideoUrl = JsonPathUtils.selAsString(url, urlPath!!)
                println("解析uid结果：$uid")
                println("解析原始播放链接：$rawVideoUrl")
                if (parameterValidator(
                        ErrorFlag.NO_PARSED_DATA, uid to "未解析到参数uid",
                        rawVideoUrl to "视频解析失败"
                    )
                ) {
                    println("尝试解密链接")
                    val cipherKey = key!!.replace("{uid}", uid!!)
                    println("key: $cipherKey, iv: $iv")
                    val decryptedUrl =
                        AESUtils.decrypt(rawVideoUrl!!, cipherKey, iv!!)
                    println("Decrypted url: $decryptedUrl")
                    if (whenNullNotifyFail(decryptedUrl, ErrorFlag.DECRYPT_ERROR, "视频解密失败")) {
                        notifyOnCompleted(toAbsoluteUrl(page.url.get(), decryptedUrl!!))
//                    {"code":200,"msg":"获取成功","url":"https://wxkdhls.mcloud.139.com/v2/hls/1468908188622393472/playlist.m3u8?ci=FqEC3xdd4IJrBH1xPgd5L8LmdKjCj-Coa&fileSize=2132772720&isNew=1","type":"m3u8","time":0,"system":"Cloudhai 计费 5.1"}
//                        val videoUrl = JsonPathUtils.selAsString(decryptedUrl!!, "$.url");
//                        if (whenNullNotifyFail(
//                                videoUrl,
//                                ErrorFlag.EMPTY_VIDEO_URL,
//                                "视频解析失败"
//                            )
//                        ) {
//                            println("解析到视频链接：$videoUrl")
//                            notifyOnCompleted(toAbsoluteUrl(page.url.get(), videoUrl!!))
//                        }
                    }
                }
            }
        }
    }
}