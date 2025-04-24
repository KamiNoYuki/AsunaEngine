package com.sho.ss.asuna.engine.entity

import android.text.TextUtils
import androidx.annotation.IntRange
import com.sho.ss.asuna.engine.utils.RegexHelper.RATING_REGEX
import java.io.Serializable

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2024/2/6 11:34
 * @description
 **/
data class Video(
    /**
     * 横向banner海报
     */
    var bannerUrl: String? = null,

    /**
     * 视频封面链接
     */
    var coverUrl: String? = null,

    /**
     * 详情页链接
     */
    var detailUrl: String? = null,

    /**
     * 视频类型，如冒险、动作、科幻...等
     */
    var category: String? = null,

    /**
     * 位于封面右下角的文本内容
     */
    var coverTitle: String? = null,

    /**
     * 电影/电视名称
     */
    var videoName: String,

    /**
     * 电视名称下面的子标题
     */
    var subtitle: String? = null,

    /**
     * 评分
     */
    var rating: String? = null,

    /**
     * 剧情简介
     */
    var plot: String? = "暂无简介信息",

    /**
     * 详情页其他信息，如演员表、地区、电影类型、时长等
     */
    var otherInfo: Map<String, String>? = null,

    /**
     * 所有包含所有剧集的节点列表
     */
    private var nodes: MutableList<Node>? = null,

    /**
     * 该视频对应的视频源
     */
    var videoSource: VideoSource?
) : Serializable {

    /**
     * 移除掉所有空白符之后的视频名称
     */
    val trimmedVideoName: String
        get() = videoName.replace("\\s".toRegex(), "")

    companion object {
        @JvmStatic
        fun toList(node: Node?): List<Node>? {
            var nodes: MutableList<Node>? = null
            if (null != node) {
                nodes = ArrayList()
                nodes.add(node)
            }
            return nodes
        }

        /**
         * 根据所给的数据创建Video对象并返回
         */
        @JvmStatic
        fun insOf(
            videoName: String, subtitle: String? = null,
            coverTitle: String? = null, coverUrl: String? = null,
            bannerUrl: String? = null, category: String? = null,
            rating: String? = null, plot: String? = null,
            detailUrl: String? = null, otherInfo: Map<String, String>? = null,
            nodes: MutableList<Node>? = null, videoSource: VideoSource? = null
        ) = Video(
            bannerUrl,
            coverUrl,
            detailUrl,
            category,
            coverTitle,
            videoName,
            subtitle,
            rating,
            plot,
            otherInfo,
            nodes,
            videoSource
        )
    }

    fun getNodes() = this.nodes

    fun setNodes(nodes: List<Node>?) {
        this.nodes = nodes as MutableList<Node>?
    }

    fun setEpisodeNode(node: Node?) {
        if (null != node) {
            nodes = nodes ?: ArrayList()
            nodes?.takeUnless { it.contains(node) }?.add(node)
        }
    }

    fun matchRating(str: String?): Boolean {
        if (null != str && !TextUtils.isEmpty(str)) {
            //使用正则表达式匹配是否为评分
            val matcher = RATING_REGEX.matcher(str)
            return matcher.find()
        }
        return false
    }

    /**
     * 获取对应节点中的所有剧集
     * @param nodeWhich 节点下标
     * @return 剧集列表
     */
    fun episodesOf(@IntRange(from = 0) nodeWhich: Int) =
        nodeOf(nodeWhich)?.takeUnless {
            it.episodes.isNullOrEmpty()
        }?.episodes

    /**
     * 根据节点下标获取对应节点
     * @param nodeWhich 节点下标
     * @return 节点
     */
    fun nodeOf(@IntRange(from = 0) nodeWhich: Int) =
        nodes?.takeIf {
            it.isNotEmpty() && nodeWhich in it.indices
        }?.get(nodeWhich)

    /**
     * 获取对应节点中的对应剧集
     * @param nodeWhich 节点下标
     * @param epWhich 剧集下标
     * @return 剧集
     */
    fun epOf(
        @IntRange(from = 0) nodeWhich: Int,
        @IntRange(from = 0) epWhich: Int
    ) = epOf(nodeOf(nodeWhich), epWhich)

    fun epOf(node: Node?, @IntRange(from = 0) epWhich: Int) =
        node?.episodes?.takeIf { it.isNotEmpty() && epWhich in it.indices }?.get(epWhich)

    fun isSameSource(targetSource: VideoSource?) =
        null != this.videoSource && null != targetSource && TextUtils.equals(
            this.videoSource?.name,
            targetSource.name
        ) && TextUtils.equals(this.videoSource?.host, targetSource.host)

    fun isSameVideo(targetVideo: Video) =
        TextUtils.equals(
            targetVideo.videoName,
            this.videoName
        ) && isSameSource(targetVideo.videoSource)

    fun matchRating(): Boolean {
        return matchRating(rating)
    }
}