package com.sho.ss.asuna.engine.processor.ext

import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.constant.ErrorFlag
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor
import com.sho.ss.asuna.engine.utils.AESUtils
import com.sho.ss.asuna.engine.utils.JsonPathUtils
import com.sho.ss.asuna.engine.utils.SpiderUtils

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/9/26 2:32:48
 * @description 电影狗-https://www.dydog.cc
 */
open class DianYingGouExt(
    entity: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : CommonSecondaryPageProcessor(entity, videoSource, episode, listener) {

    private val uidPath by lazy {
        videoSource.extras?.get("uid")
    }

    private val urlPath by lazy {
        videoSource.extras?.get("url")
    }

    private val key by lazy {
        videoSource.extras?.get("key")
    }

    private val iv by lazy {
        videoSource.extras?.get("iv")
    }

    override fun handleVideoUrl(page: Page, json: String) {
        json.takeIf {
            parameterValidator(
                ErrorFlag.EXTRAS_MISSING,
                uidPath to "extras@uidPath缺失",
                urlPath to "extras@urlPath缺失"
            )
        }?.let {
            val uid = JsonPathUtils.selAsString(it, uidPath!!)
            val url = JsonPathUtils.selAsString(it, urlPath!!)
            if (parameterValidator(
                    ErrorFlag.EMPTY_VIDEO_URL, url to "参数url获取失败"
                )
            ) {
                if (SpiderUtils.isVideoFileBySuffix(url)) {
//                    println("是视频链接，直接回调播放")
                    notifyOnCompleted(toAbsoluteUrl(page.url.get(), url!!))
                } else if (parameterValidator(
                        ErrorFlag.NO_PARSED_DATA,
                        uid to "参数uid获取失败"
                    ) && parameterValidator(
                        ErrorFlag.EXTRAS_MISSING,
                        key to "extras@key缺失",
                        iv to "extras@iv缺失"
                    )
                ) {
//                    println("uid: $uid, 原始url: $url")
                    //进行AES解密
                    val decryptedUrl = AESUtils.decrypt(url!!, key!!.replace("{uid}", uid!!), iv!!)
                    if (whenNullNotifyFail(
                            decryptedUrl,
                            ErrorFlag.DECRYPT_ERROR,
                            "视频解密失败"
                        )
                    ) {
//                        println("解密后视频链接：$decryptedUrl")
                        notifyOnCompleted(toAbsoluteUrl(page.url.get()!!, decryptedUrl!!))
                    }
                }
            }
        }
    }
}
