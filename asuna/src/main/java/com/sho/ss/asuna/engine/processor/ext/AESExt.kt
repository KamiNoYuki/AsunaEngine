package com.sho.ss.asuna.engine.processor.ext

import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.constant.ErrorFlag
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.VideoProcessor
import com.sho.ss.asuna.engine.utils.AESUtils

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2024/10/2 11:32
 * @description AES扩展处理器，常规源适用，用于解密被AES加密的源
 **/
open class AESExt(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : VideoProcessor(video, videoSource, episode, listener) {
    private val key by lazy {
        videoSource.extras?.get("key" as? String?)
    }
    private val iv by lazy {
        videoSource.extras?.get("iv" as? String?)
    }
    private val urlRegex by lazy {
        videoSource.extras?.get("urlRegex" as? String?)
    }

    override fun postPlayUrl(page: Page, str: String) {
        decrypt(page, str)
    }

    fun decrypt(page: Page, str: String,) {
        if (extrasValidator(
                key to "extras@key缺失",
                iv to "extras@iv缺失",
                urlRegex to "extras@urlRegex缺失"
            )
        ) {
//            println("解密key: $key, iv: $iv")
            AESUtils.decrypt(str, key!!, iv!!).takeIf {
                whenNullNotifyFail(it, ErrorFlag.DECRYPT_ERROR, "视频解密失败")
            }?.let { js ->
                extractUrlWithRegex(js, urlRegex!!)?.let {
//                    println("已提取到链接：$it")
                    super.postPlayUrl(page, it)
                }
            }
        }
    }
}