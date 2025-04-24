package com.sho.ss.asuna.engine.processor.ext

import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import java.net.URL

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2024/4/29 14:25
 * @description 热播之家视频扩展处理器-https://rebozj.pro
 **/
class ReBoZhiJiaExt(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : OldDianYingGouExt(video, videoSource, episode, listener) {
    private val videoParseApi by lazy { videoSource.extras?.get("parseApi") }
    override fun getVideoParseApi(page: Page) =
        videoParseApi.takeUnless {
            it.isNullOrBlank()
        }?.let { parseApi ->
            page.url
                .get()
                .runCatching {
                    URL(this).let {
                        "${it.protocol}://${it.host}${it.path}$parseApi"
                    }
                }.onFailure {
                    println("Failed to build api of parse: ${it.message}")
                }.getOrNull()
        }
}