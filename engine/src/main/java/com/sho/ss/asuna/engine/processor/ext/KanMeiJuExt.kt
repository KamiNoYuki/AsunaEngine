package com.sho.ss.asuna.engine.processor.ext

import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.constant.EngineConstant
import com.sho.ss.asuna.engine.constant.ErrorFlag
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor
import com.sho.ss.asuna.engine.utils.AESUtils
import com.sho.ss.asuna.engine.utils.JsonPathUtils
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * @project QiYuanVideo
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2025/3/8 15:13
 * @description 看美剧视频扩展处理器-https://www.kanmeiju.org
 **/
class KanMeiJuExt(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : CommonSecondaryPageProcessor(video, videoSource, episode, listener) {
    private val urlParamPath by lazy { videoSource.extras?.get("urlParamPath") }
    private val fromParamPath by lazy { videoSource.extras?.get("fromParamPath") }
    private val urlPath by lazy { videoSource.extras?.get("urlPath") }
    private val uidPath by lazy { videoSource.extras?.get("uidPath") }
    private val key by lazy { videoSource.extras?.get("key") }
    private val iv by lazy { videoSource.extras?.get("iv") }
    //该源极少数节点的from与解析时的接口名不一致，因此使用mapping映射，若mapping中不存在则直接使用即可
    private val fromMapping by lazy {
        videoSource.extras?.get("fromMapping")?.runCatching {
            Json.decodeFromString(this) as? Map<String, String>?
        }?.getOrNull()
    }

    override fun onWatchPageVideoLinkParse(page: Page, url: String) {
        if (extrasValidator(
                urlParamPath to "源extras@urlParamPath缺失",
                fromParamPath to "源extras@fromParamPath缺失"
            )
        ) {
            super.onWatchPageVideoLinkParse(page, url)
        }
    }

    override fun getUrlOfPlayerPage(page: Page, json: String): String? {
        val url = JsonPathUtils.selAsString(json, urlParamPath!!)
        val from = JsonPathUtils.selAsString(json, fromParamPath!!)
//        println("参数url: $url, from: $from")
        return if (!url.isNullOrEmpty() && !from.isNullOrEmpty()) {
            url.runCatching {
                val decoded = decodeUrlByType(url, EngineConstant.UrlDecodeType.JT)
                if(decoded.startsWith("%")) {
                    URLDecoder.decode(decoded, StandardCharsets.UTF_8.name())
                } else this
            }.onFailure {
                println("解码链接失败：${it.message}")
            }.getOrNull().let { decodedUrl ->
//                println("url解码结果: $decodedUrl")
                videoSource.videoApi?.replace("{videoUrl}", decodedUrl ?: url)
                    ?.replace("{from}", fromMapping?.get(from) ?: from)
                    ?.replace("{host}", videoSource.host)
            }
        } else null
    }

    override fun handleVideoUrl(page: Page, configJson: String) {
        if (extrasValidator(
                urlPath to "源extras@urlPath缺失",
                uidPath to "源extras@uidPath缺失",
                key to "源extras@key缺失",
                iv to "源extras@iv缺失"
            )
        ) {
            val url = JsonPathUtils.selAsString(configJson, urlPath!!)
            val uid = JsonPathUtils.selAsString(configJson, uidPath!!)
//            println("解析数据结果：url[$url], uid[$uid]")
            if (parameterValidator(
                    ErrorFlag.NO_PARSED_DATA,
                    url to "视频解析失败啦",
                    uid to "未找到uid数据"
                )
            ) {
                val fullKey = fixKey(uid!!)
//                println("AES Key[$fullKey], iv[$iv]")
                val videoUrl = AESUtils.decrypt(url!!, fullKey, iv!!)
//                println("视频链接解密后：$videoUrl")
                if (parameterValidator(
                        ErrorFlag.DECRYPT_ERROR,
                        videoUrl to "视频解密失败啦"
                    )
                ) {
                    notifyOnCompleted(videoUrl!!)
                }
            }
        }
    }

    private fun fixKey(uid: String) = key!!.replace("{uid}", uid)
}