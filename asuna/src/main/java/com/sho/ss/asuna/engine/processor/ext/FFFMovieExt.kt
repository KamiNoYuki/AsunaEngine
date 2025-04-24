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
import com.sho.ss.asuna.engine.extension.hexStringToByteArray
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.common.BaseThirdLevelPageProcessor
import com.sho.ss.asuna.engine.utils.AESUtils
import com.sho.ss.asuna.engine.utils.JsonPathUtils
import com.sho.ss.asuna.engine.utils.SpiderUtils
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2024/2/1 16:45
 * @description 555电影网-网址发布页：https://www.555app.vip/
 * 网站链接：https://5flix.net/
 **/
class FFFMovieExt(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : BaseThirdLevelPageProcessor(video, videoSource, episode, listener) {

    private val key = videoSource.extras?.get("cipherKey")
    private val iv = videoSource.extras?.get("cipherIv")
    private var epUrl: String? = null //播放页面的视频链接

    override fun onWatchPageVideoLinkParse(page: Page, url: String) {
        epUrl = url//保存一下解析到的url，请求解析链接时需要该参数
        super.onWatchPageVideoLinkParse(page, url)
    }

    override fun handleVideoUrl(page: Page, api: String) {
        takeIf {
            extrasValidator(
                api to "未解析到api信息",
                key to "extras@cipherKey缺失",
                iv to "extras@cipherIv缺失",
                epUrl to "url参数缺失"
            )
        }?.apply {
            //注释掉的此方式携带的参数会返回假的播放链接：https://www.baidu.com/index.m3u8
//            val data = AESUtils.encrypt("{\"url\":\"$epUrl\"}", key!!, iv!!.hexStringToByteArray())
            val data = AESUtils.encrypt(epUrl!!, key!!, iv!!.hexStringToByteArray())
            val url = "$api${if (!api.contains("?")) "?" else "&"}data=${
                URLEncoder.encode(
                    data,
                    StandardCharsets.UTF_8.name()
                )
            }"
            request(
                Request(url).apply {
                    addHeader(
                        HttpConstant.Header.USER_AGENT,
                        SpiderUtils.checkUserAgent(videoSource.videoApiUa, videoSource)
                    )
                    SpiderUtils.addReferer(videoSource, this, videoSource.videoApiReferer, true)
                }, 1,
                object : SpiderListener {
                    override fun onError(request: Request?, e: Exception?) {
                        notifyOnFailed(
                            ErrorFlag.EXCEPTION_WHEN_PARSING,
                            e?.message ?: "向api发起请求时出错"
                        )
                    }
                }
            )
        }
    }

    override fun doParseThirdPage(page: Page, html: Html, curPageUrl: String) {
        val rawText = page.rawText
        println("解析结果：$rawText")
        val videoUrlPath = videoSource.extras?.get("videoUrlPath")
        rawText.takeIf {
            whenNullNotifyFail(it, ErrorFlag.NO_PARSED_DATA, "api无解析结果") &&
                    whenNullNotifyFail(
                        videoUrlPath,
                        ErrorFlag.RULE_MISSING,
                        "extras@videoUrlPath缺失"
                    )
        }?.runCatching {
            AESUtils.decrypt(rawText!!, key!!, iv!!.hexStringToByteArray())
        }?.onSuccess { json ->
            println("响应信息：$json")
            json.takeIf {
                whenNullNotifyFail(it, ErrorFlag.EMPTY_VIDEO_URL, "api无数据返回")
            }?.runCatching {
                JsonPathUtils.selAsString(this, videoUrlPath!!)
            }?.onSuccess {
                if (whenNullNotifyFail(it, ErrorFlag.EMPTY_VIDEO_URL, "视频解析失败")) {
                    notifyOnCompleted(toAbsoluteUrl(page.url.get(), it!!))
                }
            }?.onFailure {
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, it.message)
            }
        }?.onFailure {
            notifyOnFailed(ErrorFlag.DECRYPT_ERROR, it.message)
        }
    }

}