package com.sho.ss.asuna.engine.entity

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import java.io.Serializable

/**
 * @project: 启源视频
 * @author: Sho Tan.
 * @E-mail: 2943343823@qq.com
 * @created: 2022/5/3 12:38:42
 * @description: 首页的ViewPagerTab展示源
 */
@kotlinx.serialization.Serializable
data class PageSource(
    /**
     * 根据reference加载对应的VideoSource到该变量
     * 系统自动加载
     * 如果reference填写的解析源名称错误，则会加载失败!
     */
    var videoSource: VideoSource? = null,

    /**
     * 引用自哪个源
     * 必填!!
     * 根据该值，引擎才能知道基于哪个源进行数据解析
     * 该值填写对应源的名字
     */
    val reference: String? = null,

    /**
     * 显示在首页的Tab页面对应URL的请求方式
     */
    val method: String? = null,

    /**
     * <h3 style="color:red">显示在首页的Tab页面对应URL的请求参数
    </h3> */
    val param: Map<String, String>? = null,
    val cookie: Map<String, String>? = null,
    val header: Map<String, String>? = null,
    val userAgent: String? = null,

    /**
     * 显示在主页的Page
     * key为主页顶部的tab名称规则
     * value为page所展示的数据对应的界面url
     */
    @SerializedName("page")
    @SerialName("page")
    val pages: List<Page>? = null,

    /**
     * 视频列表规则
     * 必填
     */
    val list: String? = null,

    /**
     * 单个视频的视频名称
     * 尽量不要为空
     */
    val name: String? = null,

    /**
     * 视频封面链接
     * 尽量不要为空
     */
    val cover: String? = null,

    /**
     * 显示在APP视频封面右下角小标题
     * 可为空，但为了美观尽量不要为空
     */
    val coverTitle: String? = null,

    /**
     * APP视频名称下方的子标题
     * 可空，但尽量不要为空
     */
    val subtitle: String? = null,

    /**
     * 进入详情页面的URL规则
     * 必填！!
     */
    val dtUrl: String? = null,

    /**
     * 下一页的链接规则
     * 注意：不是详情页链接，是下一页的链接规则
     */
    val nextPage: String? = null,

    /**
     * 以下规则为热门数据规则，主要用于显示在App搜索界面的热门推荐
     * 规则如果和page的规则一样，则可以不填，不填默认采用page的通用规则
     */
    val trendingUrl: String? = null,
    val trendingList: String? = null,
    val trendingName: String? = null,
    val trendingCover: String? = null,
    val trendingCoverTitle: String? = null,
    val trendingSubtitle: String? = null,
    val trendingDtUrl: String? = null,
    /**
     * 是否优先使用热门规则进行解析
     * 默认false，false时默认使用page规则进行解析
     */
    @SerializedName("trendingFirst")
    @SerialName("trendingFirst")
    val isTrendingFirst: Boolean = false
) : Serializable
