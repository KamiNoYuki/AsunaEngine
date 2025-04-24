package com.sho.ss.asuna.engine.entity

import android.text.TextUtils
import com.google.gson.annotations.SerializedName
import com.sho.ss.asuna.engine.core.utils.HttpConstant
import java.io.Serializable

/**
 * @author Sho
 */
open class Episode : Serializable {
    /**
     * 剧集名称
     */
    var name: String? = null

    /**
     * 每一集对应的观看界面Url，并非视频url
     */
    var url: String? = null

    /**
     * 每集的视频url，通过该url获取播放视频文件
     */
    var videoUrl: String? = null

    /**
     * 播放链接重定向，可为空
     */
    var referer: String? = null
    @JvmField
    var method: String? = null
    var header: MutableMap<String, String>? = null

    /**
     * true：则将M3U8文件缓存到本地播放
     * false:在线播放
     */
    @SerializedName("cacheable")
    var isCacheable: Boolean = false

    constructor()
    constructor(name: String?, url: String) {
        this.name = name
        this.url = url
    }

    constructor(name: String?, url: String, referer: String?) {
        this.name = name
        this.url = url
        this.referer = referer
    }

    constructor(
        name: String?,
        url: String,
        videoUrl: String?,
        referer: String?,
        cacheable: Boolean
    ) {
        this.name = name
        this.url = url
        this.videoUrl = videoUrl
        this.referer = referer
        this.isCacheable = cacheable
    }

    constructor(
        name: String?,
        url: String?,
        videoUrl: String?,
        referer: String?,
        header: MutableMap<String, String>?,
        cacheable: Boolean
    ) {
        this.name = name
        this.url = url
        this.videoUrl = videoUrl
        this.referer = referer
        this.header = header
        this.isCacheable = cacheable
    }

    /**
     * 比对Episode在解析前其内部字段是否相等
     * @param episode 比对对象
     * @return 结果
     */
    fun canEq(episode: Episode?): Boolean {
        if (null != episode) {
            val mName = episode.name
            val mReferer = episode.referer
            val mUrl = episode.url
            return TextUtils.equals(mName, name) && TextUtils.equals(
                mReferer,
                referer
            ) && TextUtils.equals(mUrl, url)
        }
        return false
    }

    val userAgent: String?
        get() = if (null != header) header!![HttpConstant.Header.USER_AGENT] else null

    fun setUserAgent(userAgent: String) {
        header = header ?: LinkedHashMap()
        header!![HttpConstant.Header.USER_AGENT] = userAgent
    }

    fun clearVideoUrl() {
        videoUrl = null
    }

    fun isSameEpisode(ep: Episode) =
        !name.isNullOrEmpty() && !ep.name.isNullOrEmpty() &&
                !url.isNullOrEmpty() && !ep.url.isNullOrEmpty() &&
                TextUtils.equals(name, ep.name) && TextUtils.equals(url, ep.url)

    companion object {
        const val serialVersionUID = -2100158540407299617L
    }

    override fun toString(): String {
        return "{" +
                "\"name\":\"$name\",\n" +
                "\"url\":\"$url\",\n" +
                "\"videoUrl\":\"$videoUrl\",\n" +
                "\"userAgent\":\"$userAgent\",\n" +
                "\"referer:\":\"$referer\",\n" +
                "\"method\":\"$method\",\n" +
                "\"header\":${header.toString()},\n" +
                "\"isCacheable\":\"$isCacheable\"\n" +
                "}"
    }

    enum class State(val value: Int) {
        /**
         * 剧集未解析
         */
        UNPARSED(0),

        /**
         * 剧集正在解析
         */
        PARSING(1),

        /**
         * 剧集解析完成
         */
        PARSED(2),

        /**
         * 剧集解析失败
         */
        PARSE_FAILED(3),

        /**
         * 剧集解析暂停
         */
        PARSE_PAUSED(4),

        /**
         * 暂停解析剧集失败
         */
        PAUSE_FAILED(5)
    }
}
