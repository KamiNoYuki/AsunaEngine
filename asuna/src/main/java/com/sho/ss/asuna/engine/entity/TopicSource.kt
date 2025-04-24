package com.sho.ss.asuna.engine.entity

import com.sho.ss.asuna.engine.core.utils.HttpConstant
import java.io.Serializable

/**
 * @project  启源视频
 * @author   Sho Tan.
 * @e-mail   2943343823@qq.com
 * @created 2023/5/13 15:59:27
 * @description  专题源实体类
 **/
@kotlinx.serialization.Serializable
data class TopicSource(
    /**
     * 是否启用此专题源
     */
    var enable: Boolean = true,
    /**
     * 该专题对应的页面链接
     */
    var topicUrl: String,
    /**
     * 请求方式，默认GET
     */
    var topicMd: String? = HttpConstant.Method.GET,
    /**
     * 请求时携带的参数，GET不会使用此params
     */
    var topicPm: Map<String,String>? = null,
    /**
     * cookies
     */
    var topicCk: Map<String,String>? = null,
    /**
     * headers
     */
    var topicHd: Map<String,String>? = null,
    /**
     * 代理userAgent
     */
    var topicUa: String? = null,
    /**
     * 请求专题页面时，topicUa为空时是否自动随机生成代理Ua
     */
    var isTopicUaGen: Boolean = true,
    /**
     * referer,默认空
     */
    var topicRf: String? = null,
    /**
     * 专题数据列表规则
     */
    var list: String,
    /**
     * 专题数据列表item的标题规则
     */
    var itemTitle: String,
    var itemTitleFilter: Map<String,String>? = null,
    /**
     * 专题数据列表item的子列表规则
     */
    var itemSubtitle: String? = null,
    var itemSubtitleFilter: Map<String,String>? = null,
    /**
     * 专题数据列表item的banner图片规则
     */
    var banner: String? = null,
    /**
     * 该专题进入对应的专题视频列表页面链接规则
     */
    var detailUrl: String,
    /**
     * 请求方式，默认GET
     */
    var detailMd: String? = HttpConstant.Method.GET,
    /**
     * 请求时携带的参数，GET不会使用此params
     */
    var detailPm: Map<String, String>? = null,
    /**
     * cookies
     */
    var detailCk: Map<String,String>? = null,
    /**
     * headers
     */
    var detailHd: Map<String,String>? = null,
    /**
     * 代理userAgent，默认随机
     */
    var detailUa: String? = null,
    /**
     * 请求专题页面时，detailUa为空时是否自动随机生成代理Ua
     */
    var isDetailUaGen: Boolean = true,
    /**
     * referer,默认空
     */
    var detailRf: String? = null,
    /**
     * 专题页面下一页链接规则，非专题详情列表的下一页链接规则
     */
    var topicNext: String? = null,
    /**
     * 专题详情页面标题规则（可空）
     */
    var detailTitle: String? = null,
    /**
     * 专题详情页面标题过滤器
     */
    var detailTitleFilter: Map<String,String>? = null,
    /**
     * 专题详情页描述文字规则（可空）
     */
    var detailDes: String? = null,
    var detailDescFilter: Map<String,String>? = null,
    var videoList: String,
    var videoName: String,
    var cover: String? = null,
    var coverTitle: String? = null,
    /**
     * 视频的子标题规则
     */
    var subtitle: String? = null,
    /**
     * 进入视频详情页面的链接规则
     */
    var videoDtUrl: String,
    /**
     * 专题详情页面的下一页链接规则
     */
    var nextUrl: String? = null,
): Serializable
