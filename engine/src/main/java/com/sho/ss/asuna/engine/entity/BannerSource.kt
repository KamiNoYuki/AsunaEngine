package com.sho.ss.asuna.engine.entity

import java.io.Serializable

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/9/29 15:49:49
 * @description
 */
@kotlinx.serialization.Serializable
data class BannerSource(
    /**
     * 包含banner数据的网页链接
     */
    var url: String? = null,

    /**
     * 该Banner引用的视频源名称,可为空
     */
    var reference: String? = null,

    /**
     * 该Banner的视频源，可为空
     */
    var source: VideoSource? = null,
    var header: Map<String, String>? = null,
    var cookie: Map<String, String>? = null,
    var userAgent: String? = null,
    var referer: String? = null,
    var method: String? = null,
    var params: Map<String, String>? = null,

    /**
     * SYSTEM(评分+上映时间) . 当subType指明为该类型，会对subTitle进行一些特殊处理
     */
    var isSystem: Boolean = false,

    /**
     * banner列表规则
     */
    var list: String? = null,

    /**
     * banner标题（电影名称）规则
     */
    var title: String? = null,
    var subtitle: String? = null,

    /**
     * subtitle过滤器
     */
    var subtitleFilter: Map<String, String>? = null,

    /**
     * banner图片链接规则
     */
    var bannerUrl: String? = null,

    /**
     * 点击banner进入详情页的链接
     */
    var dtUrl: String? = null,
) : Serializable
