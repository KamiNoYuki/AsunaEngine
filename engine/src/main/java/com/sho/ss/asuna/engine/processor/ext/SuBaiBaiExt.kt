package com.sho.ss.asuna.engine.processor.ext

import android.text.TextUtils
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
 * @created 2024/10/2 11:56
 * @description
 **/
class SuBaiBaiExt(
    video: Video,
    videoSource: VideoSource,
    episode: Episode,
    listener: ParseListener<Episode>
) : VideoProcessor(video, videoSource, episode, listener) {

    private val vipFlag by lazy {
        videoSource.extras?.get("vipFlag")
    }

    private val urlRegex by lazy {
        videoSource.extras?.get("urlRegex" as? String?)
    }

    private val keyRegex by lazy { videoSource.extras?.get("keyReg") }
    private val ivRegex by lazy { videoSource.extras?.get("ivReg") }

    override fun postPlayUrl(page: Page, str: String) {
        if (!TextUtils.isEmpty(vipFlag)) {
            `$`(vipFlag!!, page.rawText).takeIf {
                !TextUtils.isEmpty(it)
            }?.let {
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "该源限制此视频为仅会员观看")
                return
            }
        }
        if (parameterValidator(
                ErrorFlag.EXTRAS_MISSING,
                keyRegex to "extras@keyReg缺失",
                ivRegex to "extras@ivReg缺失"
            )
        ) {
            str.split(";").let { params ->
                if (params.size >= 3) {
                    params[2].let { param ->
                        extractUrlWithSubstring(param, "\"", "\"")?.let { videoParam ->
                            println("已抽取参数：$videoParam")
                            val key = extractUrlWithRegex(str, keyRegex)
                            val iv = extractUrlWithRegex(str, ivRegex)
                            println("解析到解密key: $key, iv: $iv")
                            if (parameterValidator(
                                    ErrorFlag.REGEX_EXTRACT_ERROR,
                                    key to "抽取解密key失败",
                                    iv to "抽取解密iv失败"
                                )
                            ) {
                                AESUtils.decrypt(videoParam, key!!, iv!!).takeIf {
                                    whenNullNotifyFail(it, ErrorFlag.DECRYPT_ERROR, "视频解密失败")
                                }?.let { js ->
                                    extractUrlWithRegex(js, urlRegex!!)?.let {
                                        println("已提取到链接：$it")
                                        super.postPlayUrl(page, it)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}