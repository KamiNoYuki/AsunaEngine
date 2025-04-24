package com.sho.ss.asuna.engine.processor.ext

import androidx.core.util.Pair
import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.constant.ErrorFlag
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.base.BaseLeLeVideoExtProcessor
import com.sho.ss.asuna.engine.utils.DecryptUtils
import com.sho.ss.asuna.engine.utils.JsonPathUtils
import com.sho.ss.asuna.engine.utils.SpiderUtils
import com.sho.ss.asuna.engine.utils.Xpath

/**
 * @project  启源视频
 * @author   Sho Tan.
 * @e-mail   2943343823@qq.com
 * @created 2023/6/16 10:24:30
 * @description  爱看影院-https://www.3wyy.com
 **/
class AiKanCinemaExt(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : NewVisionExt(video, videoSource, episode, listener) {
    override fun handleVideoUrl(page: Page, js: String) {
        val videoInfo = page.request.getExtra<Pair<String, String>>(keyParams)
        if (whenNullNotifyFail(videoInfo, ErrorFlag.DATA_INVALIDATE, "未接收到视频参数!") &&
            whenNullNotifyFail(videoInfo.second, ErrorFlag.NO_PARSED_DATA, "接口名称丢失!")
        ) {
//            val apiName = videoInfo.second
            var config = js.substring(0, js.lastIndexOf("}") + 1)
            println("视频信息：$config")
            if (whenNullNotifyFail(config, ErrorFlag.DATA_INVALIDATE, "未获取到视频数据!")) {
                config = config.replace(",}", "}")
                config = applyFilter(config, videoSource.playerJsFilter)
                println("视频信息标准化后：$config")
                //视频链接
                val url = JsonPathUtils.selAsString(config, "$.url")
                if (whenNullNotifyFail(url, ErrorFlag.DATA_INVALIDATE, "参数url为空!") && !url.isNullOrBlank()) {
                    //解密链接
                    if (!SpiderUtils.isVideoFileBySuffix(url)) {
                        println("尝试解密链接：$url")
                        //获取html页面中解密所需的两个字符串
                        var ids = Xpath.select("//meta[@charset='UTF-8']/@id", page.rawText)
                        var texts = Xpath.select("//meta[@name='viewport']/@id", page.rawText)
                        if (whenNullNotifyFail(ids, ErrorFlag.NO_PARSED_DATA, "解密参数id获取失败!") &&
                            whenNullNotifyFail(
                                texts,
                                ErrorFlag.NO_PARSED_DATA,
                                "解密参数text获取失败!"
                            ) && null != ids && texts != null
                        ) {
                            ids = ids.replace("now_", "")
                            texts = texts.replace("now_", "")
                            var newText = sortById(ids, texts)
                            if (whenNullNotifyFail(
                                    newText,
                                    ErrorFlag.DATA_INVALIDATE,
                                    "新的text序列无效!"
                                )
                            ) {
                                //需要拼接上该字符串
                                newText += "xsjyy6080yy"
                                //转md5加密字符串
                                val md5 = DecryptUtils.toMd5(newText)
                                //解密所需的key
                                val key = md5.substring(16)
                                //解密所需的iv偏移量
                                val iv = md5.substring(0, 16)
                                println("key = $key iv = $iv")
                                val videoUrl =
                                    BaseLeLeVideoExtProcessor.decryptLeLeVideoUrl(url, key, iv)
                                println("解密后的视频链接：$videoUrl")
                                if (whenNullNotifyFail(
                                        videoUrl,
                                        ErrorFlag.EMPTY_VIDEO_URL,
                                        "视频链接解密失败!"
                                    )
                                ) {
                                    notifyOnCompleted(toAbsoluteUrl(page.url.get(),videoUrl))
                                }
                            }
                        }
                    }
                    else {
                        notifyOnCompleted(url)
                    }
                }
            }
        }
    }
}