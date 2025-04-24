package com.sho.ss.asuna.engine.processor.ext

import android.text.TextUtils
import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.constant.ErrorFlag
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.VideoProcessor

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2024/8/2 15:08
 * @description 速影TV扩展处理器-https://xn--tv-ks3d939o.com
 **/
class SuYingTVExt(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : VideoProcessor(video, videoSource, episode, listener) {
    override fun postPlayUrl(page: Page, videoUrl: String) {
        var mVideoUrl = videoUrl
        println("postPlayUrl: $mVideoUrl")
        if (!TextUtils.isEmpty(mVideoUrl)) {
            //有些链接编码为gbk2312，因此转码为utf-8
            if (videoSource.isWatchTranscoding) mVideoUrl = transcoding(mVideoUrl)
            if (isAutoDecodePlayUrlByType) {
                mVideoUrl = decodeUrlByType(mVideoUrl)
            }
            //防止加密的是转码后的链接，因此解码后再尝试一次url解码操作
            if (videoSource.isWatchTranscoding) mVideoUrl = transcoding(mVideoUrl)
            val playUrlFilter = videoSource.playUrlFilter
            mVideoUrl = applyFilter(mVideoUrl, playUrlFilter, isUseDefaultFilter)
            //相对转绝对
            if (videoSource.isToAbsoluteUrlInPlay) mVideoUrl =
                toAbsoluteUrl(page.url.get(), mVideoUrl)
            parseAllVideoUrl(mVideoUrl)
            println("已过滤URL:[$mVideoUrl],解析完毕")
        } else notifyOnFailed(ErrorFlag.EMPTY_VIDEO_URL, "视频链接为空")
    }

    private fun parseAllVideoUrl(videoUrls: String) {
        //第01集$https://v11.tlkqc.com/wjv11/202405/16/Y3DWqiJpKv83/video/index.m3u8#第02集$https://v11.tlkqc.com/wjv11/202405/16/3Qc5M9pvm483/video/index.m3u8#第03集$https://v11.tlkqc.com/wjv11/202405/16/PHC2duFB7k83/video/index.m3u8
        videoUrls.runCatching {
            entity.getNodes()?.find { node ->
                null != node.episodes?.find {
                    it == episode
                }
            }?.let { node ->
                this.split("#").forEachIndexed { index, item ->
                    val url = item.split("\$")[1]
                    node.episodes?.takeIf { index in it.indices }?.get(index)?.videoUrl = url
                    println("index: $index, url: $url")
                }
            }
        }.onSuccess {
            notifyOnCompleted()
        }.onFailure {
            notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "解析剧集时出错")
        }
    }
}