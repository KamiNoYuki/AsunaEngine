package com.sho.ss.asuna.engine.processor.ext

import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor
import com.sho.ss.asuna.engine.utils.JsonPathUtils

/**
 * @project  启源视频
 * @author   Sho Tan.
 * @e-mail   2943343823@qq.com
 * @created 2023/6/17 16:45:43
 * @description 七色番动漫-https://www.7sefun.top
 **/
class QiSeFanExt(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : CommonSecondaryPageProcessor(video, videoSource, episode, listener) {

    override fun getUrlOfPlayerPage(page: Page, fakeVideoUrl: String): String? {
        val id = JsonPathUtils.selAsString(fakeVideoUrl, "$.id")
        //解析所需的接口名称
        val from = JsonPathUtils.selAsString(fakeVideoUrl, "$.from")
        val url = JsonPathUtils.selAsString(fakeVideoUrl, "$.url")
            ?.runCatching { transcoding(decodeUrlWithJtType(this)) }?.getOrNull()
        return super.getUrlOfPlayerPage(page, fakeVideoUrl)
            ?.replace("{id}", id ?: "")
            ?.replace("{url}", url ?: "")
            ?.replace("{from}", from ?: "")
    }
}