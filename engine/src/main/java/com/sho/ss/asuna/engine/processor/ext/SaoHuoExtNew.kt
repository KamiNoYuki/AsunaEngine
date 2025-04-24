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
import com.sho.ss.asuna.engine.utils.RegexHelper
import com.sho.ss.asuna.engine.utils.UserAgentLibrary
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2025/1/25 19:49
 * @description 骚火电影(http://shapp.us)、动漫巴士(http://dm84.site)、韩剧巴士(https://hj84.cc)等三个源公用的视频扩展处理器
 **/
class SaoHuoExtNew(
    entity: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : BaseThirdLevelPageProcessor(entity, videoSource, episode, listener) {
    private val videoUrlPath = videoSource.extras?.get("videoUrlPath")
    private val parseApi by lazy { videoSource.extras?.get("parseApi") }
    private val urlRegex by lazy { videoSource.extras?.get("urlRegex") }
    private val tRegex by lazy { videoSource.extras?.get("tRegex") }
    private val keyRegex by lazy { videoSource.extras?.get("keyRegex") }
    private val actRegex by lazy { videoSource.extras?.get("actRegex") }
    private val playRegex by lazy { videoSource.extras?.get("playRegex") }
    private val keyMapJson by lazy { videoSource.extras?.get("keyMap") }

    override fun handleVideoUrl(page: Page, videoUrl: String) {
        if (parameterValidator(
                ErrorFlag.EXTRAS_MISSING,
                urlRegex to "extras@urlRegex缺失",
                tRegex to "extras@tRegex缺失",
                keyRegex to "extras@keyRegex缺失",
                actRegex to "extras@actRegex缺失",
                playRegex to "extras@playRegex缺失"
            )
        ) {
            //以下参数均为向api.php发送解析请求时需要使用的参数。
            val urlParam =
                RegexHelper.extractParamsWithRegex(videoUrl, urlRegex!!)
                    ?.get("value")
            val tParam =
                RegexHelper.extractParamsWithRegex(videoUrl, tRegex!!)
                    ?.get("value")
            val keyParam =
                RegexHelper.extractParamsWithRegex(videoUrl, keyRegex!!)
                    ?.get("value")
            val actParam =
                RegexHelper.extractParamsWithRegex(videoUrl, actRegex!!)
                    ?.get("value")
            val playParam =
                RegexHelper.extractParamsWithRegex(videoUrl, playRegex!!)
                    ?.get("value")
            if (parameterValidator(
                    ErrorFlag.REGEX_EXTRACT_ERROR,
                    urlParam to "正则抽取参数url失败",
                    tParam to "正则抽取参数t失败",
                    keyParam to "正则抽取参数key失败",
                    actParam to "正则抽取参数act失败",
                    playParam to "正则抽取参数play失败",
                    keyMapJson to "extras@keyMap缺失"
                )
            ) {
                val key = decodeKeyParam(keyParam!!)
//                println("接口参数：url[$urlParam], t:[$tParam], key[$keyParam], act[$actParam], play[$playParam]")
//                println("解码key结果：$key")
                if (whenNullNotifyFail(key, ErrorFlag.EXCEPTION_WHEN_PARSING, "参数key解码失败") &&
                    whenNullNotifyFail(parseApi, ErrorFlag.EXTRAS_MISSING, "extras@parseApi缺失")) {
                    val params = mapOf(
                        "url" to urlParam!!,
                        "t" to tParam!!,
                        "key" to key,
                        "act" to actParam!!,
                        "play" to playParam!!
                    )
                    request(Request(toAbsoluteUrl(page.url.get(), parseApi!!)).apply {
                        addHeader(HttpConstant.Header.USER_AGENT, UserAgentLibrary().USER_AGENT_CHROME1)
                        addHeader("Origin", getHostByUrl(page.url.get()))
                        addHeader(HttpConstant.Header.REFERER, page.url.get())
                        requestBody = HttpRequestBody.form(params.apply {
//                            println("api params：$this")
                        }, StandardCharsets.UTF_8.name())
                        method = HttpConstant.Method.POST
                    }, 1, object : SpiderListener {
                        override fun onError(request: Request?, e: Exception?) {
//                            println("onError -> request：$request")
                            notifyOnFailed(
                                ErrorFlag.EXCEPTION_WHEN_PARSING,
                                e?.message ?: "视频解析失败啦"
                            )
                        }
                    })
                }
            }
        }
    }

    override fun doParseThirdPage(page: Page, html: Html, curPageUrl: String) {
        //{"code":200,"msg":"success","ext":"mp4","referer":"never","url":"https:\/\/sf1-ttcdn-tos.pstatp.com\/obj\/tos-cn-o-0000c0030\/3268bc61432d4408942e193fc462e91c"}
//        println("接口响应数据：${page.rawText}")
        page.rawText.takeIf {
            whenNullNotifyFail(it, ErrorFlag.DATA_INVALIDATE, "api无响应信息") &&
                    whenNullNotifyFail(videoUrlPath, ErrorFlag.EXTRAS_MISSING, "extras@videoUrlPath缺失")
        }?.runCatching {
            JsonPathUtils.selAsString(this, videoUrlPath!!) to JsonPathUtils.selAsString(this,"$.msg")
        }?.onSuccess { (videoUrl, msg) ->
            takeIf {
                whenNullNotifyFail(videoUrl, ErrorFlag.EMPTY_VIDEO_URL, msg ?: "解析到无效视频链接")
            }?.notifyOnCompleted(toAbsoluteUrl(page.url.get(), videoUrl!!))
        }?.onFailure {
            notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, it.message)
        }
    }

    /**
     * 该源的解析参数中的key需要解码后才能使用，该方法负责解码key
     * @param input 需要解码的key
     */
    private fun decodeKeyParam(input: String): String? {
        return keyMapJson.runCatching {
            val mapping: Map<String, String> = Json.decodeFromString(keyMapJson!!)
            // 解码 base64 字符串
            val decodedString = DecryptUtils.JsBase64Helper.atob(input)
            val result = StringBuilder()
            var index = 0
            while (index < decodedString.length) {
                var found = false
                for ((key, value) in mapping) {
                    if (decodedString.startsWith(key, index)) {
                        result.append(value)
                        index += key.length
                        found = true
                        break
                    }
                }
                // 如果没有找到匹配的键，添加原始字符
                if (!found) {
                    result.append(decodedString[index])
                    index++
                }
            }
            result.toString()
        }.getOrNull()
    }
}