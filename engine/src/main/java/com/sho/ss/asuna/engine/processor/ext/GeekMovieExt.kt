package com.sho.ss.asuna.engine.processor.ext

import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.core.utils.HttpConstant
import com.sho.ss.asuna.engine.constant.ErrorFlag
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor
import java.net.HttpURLConnection
import java.net.URL

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2024/2/2 13:15
 * @description 极客电影-https://www.jkdy.cc
 **/
class GeekMovieExt(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : CommonSecondaryPageProcessor(video, videoSource, episode, listener) {

    override fun handleVideoUrl(page: Page, videoUrl: String) {
        //非自建节点，在播放器界面可直接解析到视频链接
        if (!checkVideoUrl(page, videoUrl, false)) {
            videoSource.extras?.get("parseApi")
                .takeIf {
                    extrasValidator(
                        it to "extras@parseApi缺失"
                    )
                }?.replace("{videoUrl}", videoUrl)
                ?.let { api ->
                    parseVideoUrl(page, api)
                        .takeIf {
                            whenNullNotifyFail(it, ErrorFlag.EMPTY_VIDEO_URL, "视频解析失败")
                        }?.let {
                            notifyOnCompleted(toAbsoluteUrl(it))
                        }
                }
        }
    }

    private fun parseVideoUrl(page: Page, api: String) =
        runCatching {
            (URL(api).openConnection() as HttpURLConnection).let {
                it.requestMethod = HttpConstant.Method.GET // 设置请求方法为GET
                it.setRequestProperty(
                    HttpConstant.Header.REFERER,
                    page.url.get() ?: videoSource.host
                )
                it.setRequestProperty(
                    HttpConstant.Header.USER_AGENT,
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36 Edg/121.0.0.0"
                )
                it.instanceFollowRedirects = false
                //重定向链接内包含视频url，因此直接处理一下即可回调播放
                val location = it.getHeaderField("Location")
                it.disconnect() // 断开连接
                location?.split("url=")?.get(1)
            }
        }.onFailure {
            println(it.message)
        }.getOrNull()
}