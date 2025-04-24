package com.sho.ss.asuna.engine.processor.ext

import com.alibaba.fastjson.JSON
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
import com.sho.ss.asuna.engine.processor.base.BaseMultiPageProcessor.IParser
import com.sho.ss.asuna.engine.processor.common.BaseThirdLevelPageProcessor
import com.sho.ss.asuna.engine.utils.DecryptUtils
import com.sho.ss.asuna.engine.utils.SpiderUtils
import java.util.Date

/**
 * @project  启源视频
 * @author   Sho Tan.
 * @e-mail   2943343823@qq.com
 * @created 2023/5/12 19:21:59
 * @description  HDMoli-https://hdmoli.com/
 * @deprecated 该源因Android底层证书协议问题无法访问，暂时废弃
 **/
@Deprecated("just for now")
class HDMoLiExt(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : BaseThirdLevelPageProcessor(video, videoSource, episode, listener) {

    private fun handleParams(param: String) =
        param.runCatching {
            val vid = param.substring(param.indexOf("vid=\"") + 5, param.indexOf("\";var vfrom"))
            val vFrom =
                param.substring(param.indexOf("vfrom=\"") + 7, param.indexOf("\";var vpart"))
            val vPart = param.substring(param.indexOf("vpart=\"") + 7, param.indexOf("\";var now"))
            val now = param.substring(param.indexOf("now=\"") + 5, param.indexOf("\";var pn"))
            val pn = param.substring(param.indexOf("pn=\"") + 4, param.indexOf("\"; var next"))
            val next =
                param.substring(param.indexOf("next=\"") + 6, param.indexOf("\";var prePage"))
            val nextPage =
                param.substring(param.indexOf("nextPage=\"") + 10, param.lastIndexOf("\";"))
            mutableMapOf(
                "vid" to vid,
                "vfrom" to vFrom,
                "vpart" to vPart,
                "now" to now,
                "pn" to pn,
                "next" to next,
                "nextPage" to nextPage
            )
        }.onFailure {
            println("error to handle params：${it.message}")
        }.getOrNull()

    private fun buildUrl(params: MutableMap<String, String>): String? {
        return when (params["pn"]) {
            "dp" ->
                "${videoSource.host}/api/webvideo.php?url=${params["now"]}&type=json&t=${Date().time}"

            "duoduozy" -> "https://play.qwertwe.top/xplay/?url=${params["now"]}"
            else -> null
        }
    }

    override fun getUrlOfPlayerPage(page: Page, params: String) =
        handleParams(params)?.let { buildUrl(it) }

    override fun getPlayerPageTargetInstance(): IParser {
        return IParser { page: Page, html: Html, _: String? ->
            println("正在解析播放器页面视频信息：" + html.get())
            if (whenNullNotifyFail(
                    playerPageJsXpath,
                    ErrorFlag.RULE_MISSING,
                    "视频信息规则缺失"
                )
            ) {
                val videoJs = `$`(playerPageJsXpath, html)
                if (page.url.get()?.contains("webvideo.php") != true) {
                    println("播放器Js配置信息：$videoJs")
                    if (whenNullNotifyFail(
                            videoJs,
                            ErrorFlag.EXCEPTION_WHEN_PARSING,
                            "播放器参数解析失败"
                        ) && null != videoJs
                    ) {
                        handleVideoConfigJs(page, videoJs)
                    }
                } else {
                    handleVideoUrl(page, videoJs ?: "")
                }
            }
        }
    }

    //节点类型（共2个节点）：
    //var vid="1889";var vfrom="1";var vpart="0";var now="1d72cfcce6eafd85c9d803b101422b63";var pn="dp"; var next="4ed3081b889233fe2a7c149c2447cb83";var prePage="/play/1889-1-0.html";var nextPage="/play/1889-1-1.html";
    //      dp：http://dplayer/dplayer.html?v=1.61&videourl={nextPage},https://www.hdmoli.com/api/webvideo.php?url={now},{next},{vid},{vfrom},{vpart}
    //          视频链接解析：https://www.hdmoli.com/api/webvideo.php?url=bfa326030c4c901bf4027eb3da8c64b3&type=json&t=1683892430707
    //var vid="1889";var vfrom="0";var vpart="0";var now="pRoo00oE5lRo000oo000oijkVkkVIx56G4lvcm6r3SRSHdjeFIJDvkmU1koGUkOCEx0ay7m7WapBgAVoo00ocVEBpp6le0528pLhSTf6z8qRQvyhbbhDEjJCo000osQ3cPKeENo000oUZqoo00ooo00oxY79Mphd";var pn="duoduozy"; var next="pRoo00oE5lRo000oo000oijkVkkVIx56G4lvcm6r3SRSHdjeFIJDvklb9wkfHHXLHPLfeASnr1cmEy67kuko000o5yeVkJb4t8GgwPtAMNUG4F5J4Ypx40Izyyeo000oSnLHvbp9ElHlqPYZ9ibx";var prePage="/play/1889-0-0.html";var nextPage="/play/1889-0-1.html";
    //      duoduozy：https://play.qwertwe.top/xplay/?url={now}
    override fun handleVideoUrl(page: Page, videoUrl: String) {
        //dp
        if (page.url.get()?.contains("webvideo.php") == true) {
            //解析json拿播放链接
            val url = page.json.runCatching { jsonPath("$.url").get() }
                .onFailure { println("error to get url in json: ${it.message}") }
                .getOrNull()
            if (whenNullNotifyFail(url, ErrorFlag.EMPTY_VIDEO_URL, "未解析到视频链接")) {
                //解密视频链接
                println("解析到的原始视频链接：$url")
                url.runCatching { decryptHDMoLi(this!!) }
                    .onFailure {
                        println("failed to decrypted the video url: ${it.message}")
                        notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "链接解密失败")
                    }.onSuccess {
                        notifyOnCompleted(
                            if (videoSource.isToAbsoluteUrlInPlay) toAbsoluteUrl(
                                page.url.get(),
                                it
                            ) else it
                        )
                    }
            }
        } else { //duoduozy
            sendVideoParseRequest(videoUrl)
        }
    }

    private fun sendVideoParseRequest(videoUrl: String) {
        //转为Json
        val json = JSON.parseObject(videoUrl)
        if (whenNullNotifyFail(json, ErrorFlag.EMPTY_VIDEO_URL, "视频配置信息无效")) {
            val videoApi = videoSource.videoApi
            if (whenNullNotifyFail(videoApi, ErrorFlag.API_MISSING, "视频前置Api缺失")) {
                val baseUrl = getHostByUrl(videoApi) + "/xplay/555tZ4pvzHE3BpiO838.php?"
                val builder = baseUrl + "tm=" + Date().time + "&" +
                        "url=" + json.getString("url") + "&" +
                        "vkey=" + json.getString("vkey") + "&" +
                        "token=" + json.getString("token") + "&" +
                        "sign=F4penExTGogdt6U8"
                val request = Request(builder)
                    .addHeader(HttpConstant.Header.REFERER, videoSource.videoApiReferer)
                    .addHeader(
                        HttpConstant.Header.USER_AGENT,
                        SpiderUtils.checkUserAgent(videoSource.videoApiUa, videoSource)
                    )
                val spider = SpiderUtils.buildSpider(this, request, 1)
                if (whenNullNotifyFail(
                        spider,
                        ErrorFlag.INIT_ENGINE_EXCEPTION,
                        "初始化引擎时出错!"
                    ) && null != spider
                ) {
                    SpiderUtils.addListenerForSpider(spider, object : SpiderListener {
                        override fun onError(request: Request, e: Exception) {
                            val msg = e.message
                            notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, msg)
                        }
                    })
                    spider.runAsync()
                }
            }
        }
    }

    override fun doParseThirdPage(page: Page, html: Html, curPageUrl: String) {
        page.json
            .runCatching { jsonPath("$.url").get() }
            .onFailure { notifyOnFailed(ErrorFlag.EMPTY_VIDEO_URL, "抽取视频链接失败") }
            .onSuccess {
                val newUrl =
                    if (videoSource.isToAbsoluteUrlInPlay) toAbsoluteUrl(page.url.get(), it) else it
                if (whenNullNotifyFail(newUrl, ErrorFlag.EMPTY_VIDEO_URL, "未解析到视频链接")) {
                    //解密链接
                    println("解密前链接：$it")
                    DecryptUtils.removePretendChars(it)!!.apply {
                        println("解密后链接：$this")
                        notifyOnCompleted(this)
                    }
                }
            }
    }

    private fun decryptHDMoLi(url: String): String {
        val cipher = "ItLdg666"
        val newUrl = DecryptUtils.JsBase64Helper.atob(url.replace("\\", ""))
        val len = cipher.length
        val appender = StringBuilder()
        for (i in newUrl.indices) {
            val mod = i % len
            val a = newUrl[i].code xor cipher[mod].code
            appender.append(a.toChar())
        }
        return DecryptUtils.JsBase64Helper.atob(appender.toString())
    }
}