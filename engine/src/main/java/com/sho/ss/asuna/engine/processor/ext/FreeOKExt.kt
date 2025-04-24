package com.sho.ss.asuna.engine.processor.ext

import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.core.selector.Html
import com.sho.ss.asuna.engine.constant.ErrorFlag
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.VideoProcessor
import com.sho.ss.asuna.engine.processor.base.BaseLeLeVideoExtProcessor
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor
import com.sho.ss.asuna.engine.utils.DecryptUtils
import com.sho.ss.asuna.engine.utils.SpiderUtils
import com.sho.ss.asuna.engine.utils.Xpath

/**
 * @project  启源视频
 * @author   Sho Tan.
 * @e-mail   2943343823@qq.com
 * @created 2023/7/7 9:23:54
 * @description  FreeOK-https://www.freeok.vip
 **/
class FreeOKExt(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : CommonSecondaryPageProcessor(video, videoSource, episode, listener) {

    /**
     * 版权下架检测器
     */
    class FreeOkCopyrightChecker(
        entity: Video,
        videoSource: VideoSource,
        episode: Episode,
        listener: ParseListener<Episode>
    ) : VideoProcessor(
        entity,
        videoSource, episode, listener
    ) {
        override fun extensionParse(page: Page, html: Html) {
            videoSource.extras.takeIf {
                whenNullNotifyFail(it, ErrorFlag.EXTRAS_MISSING, "未找到源的Extras数据")
            }?.let { extras ->
                val copyrightRule = extras["copyrightRule"]
                if (!copyrightRule.isNullOrBlank() && `$`(copyrightRule, html)?.isNotBlank() == true)
                    notifyOnFailed(
                        ErrorFlag.EXCEPTION_WHEN_PARSING,
                        "该源因版权限制已下架该视频\n请尝试切换到其他源观看吧~"
                    )
                else
                    super.extensionParse(page, html)
            }
        }
    }

    override fun getNormalVideoProcessor(
        page: Page,
        listener: ParseListener<Episode>
    ) = FreeOkCopyrightChecker(entity, videoSource, episode, listener)

    override fun handleVideoUrl(page: Page, videoUrl: String) {
        //解密链接
        if (!SpiderUtils.isVideoFileBySuffix(videoUrl)) {
            videoSource.extras.takeIf {
                whenNullNotifyFail(it, ErrorFlag.EXTRAS_MISSING, "源Extras缺失") &&
                        whenNullNotifyFail(
                            it!!["suffix"] as String,
                            ErrorFlag.EXTRAS_MISSING,
                            "Extras@suffix缺失"
                        ) &&
                        whenNullNotifyFail(
                            it["idRule"] as String,
                            ErrorFlag.EXTRAS_MISSING,
                            "Extras@idRule缺失"
                        ) &&
                        whenNullNotifyFail(
                            it["textRule"] as String,
                            ErrorFlag.EXTRAS_MISSING,
                            "Extras@textRule缺失"
                        ) &&
                        whenNullNotifyFail(
                            videoSource.extras!!["suffix"],
                            ErrorFlag.EXTRAS_MISSING,
                            "Extras@suffix缺失"
                        )
            }?.let { extras ->
                println("尝试解密链接：$videoUrl")
                //获取html页面中解密所需的两个字符串
                var ids = Xpath.select(extras["idRule"] as String, page.rawText)
                var texts = Xpath.select(extras["textRule"] as String, page.rawText)
                if (whenNullNotifyFail(ids, ErrorFlag.NO_PARSED_DATA, "解密所需id获取失败!") &&
                    whenNullNotifyFail(
                        texts,
                        ErrorFlag.NO_PARSED_DATA,
                        "解密所需的text获取失败!"
                    ) && null != ids && texts != null
                ) {
                    ids = ids.replace("now_", "")
                    texts = texts.replace("now_", "")
                    NewVisionExt.sortById(ids, texts).takeIf {
                        whenNullNotifyFail(it, ErrorFlag.DATA_INVALIDATE, "新的Text序列无效")
                    }?.run {
                        //需要拼接上该字符串
                        val fullNewText = this + videoSource.extras!!["suffix"]
                        //转md5加密字符串
                        val md5 = DecryptUtils.toMd5(fullNewText)
                        //解密所需的key
                        val key = md5.substring(16)
                        //解密所需的iv偏移量
                        val iv = md5.substring(0, 16)
                        println("key = $key iv = $iv")
                        BaseLeLeVideoExtProcessor.decryptLeLeVideoUrl(videoUrl, key, iv).takeIf {
                            whenNullNotifyFail(
                                it,
                                ErrorFlag.EMPTY_VIDEO_URL,
                                "视频链接解密失败!"
                            )
                        }?.let {
                            println("解密后的视频链接：$it")
                            super.handleVideoUrl(page, it)
                        }
                    }
                }
            }
        } else {
            super.handleVideoUrl(page, videoUrl)
        }
    }
}