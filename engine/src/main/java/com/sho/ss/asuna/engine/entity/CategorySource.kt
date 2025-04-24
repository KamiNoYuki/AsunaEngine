package com.sho.ss.asuna.engine.entity

import java.io.Serializable

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/9/15 15:02:23
 * @description
 */
@kotlinx.serialization.Serializable
data class CategorySource(
    /**
     * 分类名称
     */
    val categoryName: String? = null,

    /**
     * 是否启用分类界面
     */
    val enabled: Boolean = true,

    /**
     * 分类界面Url
     * 可空
     */
    val categoryUrl: String? = null,

    /**
     * 访问分类界面链接时会采用该请求方法
     * 可空，为空时默认为GET
     */
    val categoryUrlMd: String? = null,
    val categoryUrlReferer: String? = null,

    /**
     * 访问分类界面链接时携带的请求参数
     * 可空
     */
    val categoryUrlPm: Map<String, String>? = null,

    /**
     * 访问分类界面链接时携带的cookie
     * 可空
     */
    val categoryUrlCk: Map<String, String>? = null,

    /**
     * 访问分类界面链接时携带的header
     * 可空
     */
    val categoryUrlHd: Map<String, String>? = null,

    /**
     * 访问分类界面链接时采用的用户代理
     * 可为空
     * 为空时每次请求都会随机生成用户代理
     */
    val categoryUrlUa: String? = null,

    /**
     * 分类中存放单行全部tab的容器
     * 如：[tab1 tab2 tab3 tab4 tab5]
     * ↑该容器规则
     */
    val categoryTabList: String? = null,

    /**
     * 分类单行的分类名称
     * 如：分类 [tab1 tab2 tab3 tab4 tab5]
     * ↑该名称的规则
     */
    val categoryListName: String? = null,

    /**
     * 分类单行列表中的单个tab规则
     * 如：分类 [tab1 tab2 tab3 tab4 tab5]
     * ↑单个tab的规则
     */
    val categoryTabItem: String? = null,

    /**
     * 分类单行列表中的单个tab的名称规则
     * 如：分类 [tab1 tab2 tab3 tab4 tab5]
     * ↑单个tab名称的规则
     */
    val categoryTabName: String? = null,

    /**
     * 单个tab的对应页面链接规则
     */
    val categoryTabUrl: String? = null,

    /**
     * 以下为分类界面解析VideoList的相关规则
     */
    val videoList: String? = null,
    val videoName: String? = null,
    val cover: String? = null,
    val coverTitle: String? = null,
    val subtitle: String? = null,

    /**
     * 详情页链接
     */
    val dtUrl: String? = null,

    /**
     * 下一页链接
     */
    val nextUrl: String? = null,
    /**
     * 是否忽略标签列表的子分类Tab只有一项的分类数据，默认true
     */
    val isDropSingleTabItem: Boolean = true,
) : Serializable {
}
