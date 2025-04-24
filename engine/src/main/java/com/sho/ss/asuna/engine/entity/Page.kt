package com.sho.ss.asuna.engine.entity

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import java.io.Serializable

/**
 * @project: 启源视频
 * @author: Sho Tan.
 * @E-mail: 2943343823@qq.com
 * @created: 2022/5/4 16:03:53
 * @description: page实体类
 */
@kotlinx.serialization.Serializable
data class Page(
    /**
     * item标题
     */
    @SerializedName("name")
    @SerialName("name")
    val pageName: String? = null,

    /**
     * 已废弃
     */
    @SerializedName("pt")
    @Deprecated("")
    val pageTitle: String? = null,

    /**
     * 该页对应的数据解析页面链接
     */
    @SerializedName("url")
    @SerialName("url")
    val pageUrl: String? = null,

    /**
     * 下一页链接规则
     * 主要避免在每个页面的下一页规则有所差异时使用
     * 如果每个页面的下一页相同，无需在每个page内重复填写
     * 只需要填写nextPage即可
     */
    val nextUrl: String? = null,

    /**
     * 该Page是否启用
     */
    val isEnabled: Boolean = true,

    /**
     * 是否显示banner 默认不显示
     * 当该值为true时，pageTile也会显示
     *  * true 显示
     *  * false 隐藏(默认)
     */
    val showBanner: Boolean = false
) : Serializable
