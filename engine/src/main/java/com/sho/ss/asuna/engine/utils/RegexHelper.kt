package com.sho.ss.asuna.engine.utils

import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2024/2/3 14:40
 * @description 正则表达式辅助类，对正则进行了缓存、复用处理。
 **/
object RegexHelper {
    //采用ConcurrentHashMap确保线程安全
    private val patternCache = ConcurrentHashMap<String, Pattern>()
    val photoRegex: Pattern by lazy {
        Pattern.compile(".+\\.(jpeg|jpg|png|bmp|tif|gif|pcx|tga|exif|fpx|svg|psd|cdr|pcd|dxf|ufo|eps|ai|raw|wmf|webp|avif|apng)")
    }
    val videoRegex: Pattern by lazy {
        Pattern.compile(".+\\.(m3u8|mp4|avg|flv|f4v|webm|m4v|mov|3gp|3g2|mpg|mpeg|mpe|ts|div|dv|divx|vob|dat|mkv|lavf|flc|mod|ram|qt|fli|cpk|wmv|avi|asf|rm|rmvb|dirac)")
    }
    val audioRegex: Pattern by lazy {
        Pattern.compile(".+\\.(mp3|wma|wav|ape|flac|ogg|aac|aiff|vqf|midi|cd|mpeg|realaudio|au|wv|asf|dsp|pac|rmi|mod|cmf|oggvorbis)")
    }
    val RATING_REGEX: Pattern by lazy { Pattern.compile("^\\d{1,2}\\.+\\d分?$") }

    fun getPattern(regex: String): Pattern = patternCache.getOrPut(regex) {
        Pattern.compile(regex)
    }

    /**
     * 根据给定的正则表达式从目标字符串中抽取值
     * 抽取的值若有多个，会以Map键值对形式返回，若抽取的值为单一的字符串，则可通过map.get("value")获取
     */
    @JvmOverloads
    fun extractParamsWithRegex(
        str: String,
        regex: String,
        onError: ((msg: String) -> Unit)? = null
    ): Map<String, String>? {
        if (str.isBlank()) {
            onError?.invoke("抽取目标无效")
            return null
        }
        if(regex.isBlank()) {
            onError?.invoke("正则表达式无效")
            return null
        }
        return runCatching {
            val pattern = getPattern(regex) // 从缓存中获取或创建 Pattern
            val matcher = pattern.matcher(str)
            val params = mutableMapOf<String, String>()
            while (matcher.find()) {
                when (matcher.groupCount()) {
                    1 -> matcher.group(1)
                        ?.takeIf { it.isNotBlank() }
                        ?.let {
                            params["value"] = it
                        }

                    2 -> {
                        val key = matcher.group(1)
                        val value = matcher.group(2)
                        if (!key.isNullOrBlank() && !value.isNullOrBlank()) {
                            params[key] = value
                        }
                    }
                }
            }
            if(params.isEmpty()) throw IllegalStateException("正则匹配失败") else params
        }.onFailure {
            onError?.invoke(it.message ?: "正则编译失败")
        }.getOrNull()
    }
}