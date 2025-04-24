package com.sho.ss.asuna.engine.entity

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.sho.ss.asuna.engine.constant.SourceType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Transient
import java.io.Serializable

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2024/2/6 1:44
 * @description
 **/
@kotlinx.serialization.Serializable
data class VideoSource(
    /**
     * 网站主域名
     */
    val host: String,

    /**
     * true 开启(默认)/false 关闭 自动检测<h2>在观看页面解析到的视频链接</h2>是否为一个可播放的链接
     * 如果是可播放的视频链接，则会停止向后解析，回调给播放器进行播放
     */
    val isAutoCheckVideoUrlInPlay: Boolean = true,

    /**
     * 观看界面的视频链接如果是相对链接，是否自动转绝对链接
     * 默认否
     */
    val isToAbsoluteUrlInPlay: Boolean = false,

    /**
     * 存放额外数据
     */
    val extras: HashMap<String, String>? = null,

    /**
     * 源名称
     */
    val name: String,

    /**
     * 是否为系统内置的视频源
     * 不可通过编辑源更改
     * 不参与序列化
     */
    @Expose(deserialize = false)
    @Transient //添加该注解以忽略该变量参与序列化，但需要在Json添加ignoreUnknownKeys = true，否则解析到未知Key会抛异常
    val isSystem: Boolean = true,

    /**
     * 是否为不稳定的源：
     * 时而能加载时而加载失败的，此类干扰比较频繁的可视为不稳定的源
     * 默认true：稳定
     */
    @SerializedName("stable")
    @SerialName("stable")
    val isStable: Boolean = true,

    /**
     * 是否可见，即源管理中是否显示
     */
    @SerializedName("visible")
    @SerialName("visible")
    val isVisible: Boolean = true,

    /**
     * 是否启用源
     * 默认启用
     */
    var isEnable: Boolean = true,

    /**
     * 搜索的启用状态是否可被用户手动修改,默认允许
     */
    val isSearchStateModifiable: Boolean = true,
    /**
     * 源类型：
     * {@link com.sho.ss.asuna.engine.engine.constant.SourceType.TYPE_EXTENSION}：该源的视频资源链接需要特殊处理才能解析到，如多页面解析、解密、破解反爬虫等因素
     * {@link com.sho.ss.asuna.engine.engine.constant.SourceType.TYPE_NORMAL}：该源通过常规方式解析即可获取到视频资源链接
     * {@link com.sho.ss.asuna.engine.engine.constant.SourceType.TYPE_SECONDARY_PAGE}：二级常规源
     */
    val type: Int = 0,

    /**
     * 源品质级别，32767以内：
     * 数值越高，代表该源品质越优质
     * 该数值应根据视频源是否包含广告、水印、加载速度、视频清晰度...等一系列因素作综合评估
     * 高数值将优先被引擎加载、解析，源列表也根据该数值进行排序。
     */
    val quality: Short = 0,

    /**
     * spiderPath + ext = 扩展爬虫处理器路径
     */
    val ext: String? = null,

    /**
     * 该源的视频中是否包含广告
     */
    val isHasAd: Boolean = false,

    /**
     * 通用分类页规则配置，可以将多个规则相同的页面规则配置到此处
     * 默认优先采用每个页面自带的规则配置，如果为空则采用该配置
     */
    val commonCategory: CategorySource? = null,

    /**
     * 分类页配置
     */
    val categoryConfig: List<CategorySource>? = null,

    /**
     * 搜索界面API前置
     */
    val searchApi: String? = null,

    /**
     * 是否自动检测剧集列表顺序为升序，如检测为降序则自动逆序为升序
     * 使剧集顺序始终保持第1集、第2集…，默认true
     */
    @SerializedName("listReverse")
    @SerialName("listReverse")
    val isListReverse: Boolean = true,

    /**
     * 视频是否支持预加载，有些源的视频链接访问有时间限制，超过一定时间就无法访问
     * false时不会预加载、缓存链接，而是实时解析
     */
    val isEpCacheable: Boolean = true,

    /**
     * 播放剧集视频时携带的Header
     */
    val epHeader: Map<String, String>? = null,

    /**
     * 视频详情页面API前置
     */
    val dtApi: String? = null,

    /**
     * 播放页面API前置
     */
    val playApi: String? = null,

    /**
     * 全局重定向，未指定url的重定向则会携带该重定向
     * 可为空
     */
    val referer: String? = null,

    /**
     * 是否启用referer
     * 该值为false，未开发完整
     * 目前仅作用于请求播放链接时是否携带referer
     * 默认true
     */
    val refererEnable: Boolean = true,

    /**
     * 视频资源请求API前置
     */
    val videoApi: String? = null,

    /**
     * 重定向
     */
    val videoApiReferer: String? = null,

    /**
     * UserAgent
     */
    val videoApiUa: String? = null,

    /**
     * [.videoApi]的请求方法
     */
    val videoApiMd: String? = null,

    /**
     * [.videoApi]请求时携带的Header
     */
    val videoApiHd: Map<String, String>? = null,

    /**
     * [.videoApi]请求时携带的参数
     */
    val videoApiPm: Map<String, String>? = null,

    /**
     * [.videoApi]请求时携带的Cookies
     */
    val videoApiCk: Map<String, String>? = null,

    /**
     * 专题源
     */
    val topic: TopicSource? = null,

    /**
     * 搜索请求方式（get、post、delete、option...）
     * searchMD: search method
     */
    val searchMD: String? = null,

    /**
     * 搜索url的 cookie
     * searchCK: search cookie
     */
    val searchCK: Map<String, String>? = null,

    /**
     * 搜索请求参数
     * searchPM: search param
     */
    val searchPM: Map<String, String>? = null,

    /**
     * 搜索时携带的userAgent
     * 可空，为空时默认全局userAgent
     */
    val searchUA: String? = null,

    /**
     * 搜索下一页链接请求方式,为空时默认GET
     */
    val searchNextMD: String? = null,

    /**
     * 搜索下一页header
     */
    val searchNextHD: Map<String, String>? = null,

    /**
     * 搜索下一页cookies
     */
    val searchNextCK: Map<String, String>? = null,
    val searchNextUA: String? = null,

    /**
     * 搜索下一页请求参数
     */
    val searchNextPM: Map<String, String>? = null,

    /**
     * 搜索下一页的重定向，可为空，为空时默认使用全局Referer
     */
    val searchNextReferer: String? = null,

    /**
     * 关键字是否转码为GBK2312
     * 如：%3D%80%31%0A%4B
     * 可空，默认false
     */
    val isTranscoding: Boolean = false,

    /**
     * 全局userAgent，该源的每个页面都会携带该userAgent
     */
    val userAgent: String? = null,

    /**
     * 搜索Url时携带该header
     * searchHD: search header
     */
    val searchHD: Map<String, String>? = null,

    /**
     * 搜索列表容器规则
     */
    val searchList: String? = null,

    /**
     * 搜索界面下一页的链接规则
     * 可为空
     */
    val searchNext: String? = null,

    /**
     * 视频名称规则
     */
    val videoName: String? = null,

    /**
     * 封面规则
     */
    val videoCover: String? = null,

    /**
     * 该规则显示在App搜索列表Item，封面右下角的文本信息
     */
    val coverTitle: String? = null,

    /**
     * 该规则显示在App搜索列表Item电影名称下面的子标题
     */
    val subtitle: String? = null,

    val subtitleFilter: Map<String, String>? = null,

    /**
     * 视频分类
     */
    val category: String? = null,

    /**
     * 搜索列表中，视频进入详情页面的url规则
     */
    val dtUrl: String? = null,

    /**
     * 进入详情页面url的请求cookies，可为空
     * dtUrlCK：dtUrl cookie
     */
    val dtUrlCK: Map<String, String>? = null,

    /**
     * 进入详情页面的url的请求参数，可为空
     * dtUrlPM： dtUrl param
     */
    val dtUrlPM: Map<String, String>? = null,

    /**
     * 详情页面的url的请求方式，可为空，默认GET
     * dtUrlMD: dtUrl method
     */
    val dtUrlMD: String? = null,

    /**
     * 详情页面的url的header，可为空
     * dtUrlHD: dtUrl header
     */
    val dtUrlHD: Map<String, String>? = null,

    /**
     * 详情页面的url的用户代理，可为空
     * 如果空，则会采用全局userAgent
     * dtUrlUA: dtUrl userAgent
     */
    val dtUrlUA: String? = null,
    val dtUrlReferer: String? = null,

    /**
     * 详情页解析时的视频封面链接规则，仅在搜索界面未解析到封面链接时才会使用该规则，可为空
     * 通常用于通过点击Banner进入的播放界面时
     */
    val dtCover: String? = null,

    /**
     * 视频详情页剧情简介规则
     */
    val dtPlot: String? = null,

    /**
     * 剧情简介过滤器
     * 支持正则表达式
     */
    val plotFilter: Map<String, String>? = null,

    /**
     * 详情页评分规则
     */
    val dtRating: String? = null,

    /**
     * 详情页其他信息，如演员表、地区、电影类型、时常等
     */
    val dtOther: LinkedHashMap<String, String>? = null,

    /**
     * 其他信息过滤器
     */
    val dtOtherFilter: Map<String, String>? = null,

    /**
     * 节点黑名单，该集合中的节点将在解析时屏蔽
     * 需要保证nodeNameList能正确解析到节点名称数据才能生效
     */
    val nodeBlackList: HashSet<String>? = null,

    /**
     * 节点列表容器规则：container{节点1[第一集,第二集...],节点2[第一集,第二集...]},
     */
    val nodeList: String? = null,

    /**
     * 节点名称列表
     */
    val nodeNameList: String? = null,

    /**
     * 节点Item名称
     */
    val nodeNameItem: String? = null,
    val nodeNameFilter: Map<String, String>? = null,

    /**
     * 节点列表规则，如：节点1[第一集,第二集...]
     */
    val nodeItem: String? = null,

    /**
     * 剧集名称规则
     */
    val itemName: String? = null,
    val itemNameFilter: Map<String, String>? = null,

    /**
     * 进入播放界面的Url，即每一集的Url
     */
    val itemUrl: String? = null,
    val itemUrlFilter: Map<String, String>? = null,

    /**
     * itemUrl的链接可否直接用于播放
     */
    val isItemUrlCanPlay: Boolean = false,

    /**
     * 每一集Url的请求方法，可为空，默认GET
     * itemUrlMD： itemUrl method.
     */
    val itemUrlMD: String? = null,

    /**
     * 每一集Url的请求参数，可为空
     * itemUrlPM: itemUrl param
     */
    val itemUrlPM: Map<String, String>? = null,

    /**
     * 每一集Url的请求Cookies,可为空
     * itemUrlCK: itemUrl cookie
     */
    val itemUrlCK: Map<String, String>? = null,

    /**
     * 每一集Url的请求Header，可为空
     * itemUrlHD: itemUrl header
     */
    val itemUrlHD: Map<String, String>? = null,

    /**
     * 观看界面的相关推荐列表规则
     * 可空
     */
    val relatedList: String? = null,

    /**
     * 相关推荐列表视频名称规则
     */
    val relatedVideoName: String? = null,

    /**
     * 相关推荐列表封面规则
     */
    val relatedVideoCover: String ?= null,

    /**
     * 相关推荐列表封面右下角的文本信息
     */
    val relatedCoverTitle: String? = null,

    /**
     * 相关推荐列表Item电影名称下面的子标题
     */
    val relatedSubtitle: String? = null,

    /**
     * 相关推荐列表Item详情页面的url规则
     */
    val relatedDtUrl: String? = null,

    /**
     * 视频url规则
     * 源类型：2（常规|NORMAL）时，必须通过该基于该规则解析到播放Url
     * 原类型：1（扩展|EXTENSION)时，表明无法通过该值直接解析到视频Url，需要通过扩展处理器进行代码编写以获取到视频Url
     */
    val playUrl: String? = null,

    val playUrlDecode: Int = 0,

    /**
     * 视频Url提取方式
     * 考虑到有些guys不会正则（比如我），特意增加了这个功能
     * 目前提取方式有三种：1、regex-顾名思义，正则
     * 2、sub-截取，该方式非常简单，截取两段字符或两个字符之间的值。
     * 只需要知道开始字符和结束字符即可
     * 如：《这是示例示例，"this is a simple string.aaa"》
     * 截取引号“与引号”之间的英语，填入开始字符"和结束字符“即可
     * 也可以只要前置字符，后置字符缺省
     * 3、空：不需要特殊处理，通过规则可直接解析到播放链接
     */
    val playExtractor: String? = null,

    /**
     * 前置截取标识字符串
     */
    val playPrefix: String? = null,

    /**
     * 后置截取标识字符串
     */
    val playSuffix: String? = null,

    /**
     * playUrl的请求方法，可为空，默认GET
     * playUrlMD： playUrl method.
     */
    val playUrlMD: String? = null,

    /**
     * playUrl的请求参数，可为空
     * playUrlPM: playUrl param
     */
    val playUrlPM: Map<String, String>? = null,

    /**
     * playUrlUrl的请求Cookies,可为空
     * playUrlCK: playUrl cookie
     */
    val playUrlCK: Map<String, String>? = null,

    /**
     * playUrlUrl的请求Header，可为空
     * playUrlHD: playUrl header
     */
    val playUrlHD: Map<String, String>? = null,

    /**
     * 视频播放url的用户代理头，可为空
     * 该值为空时采用全局UserAgent
     */
    val playUrlUA: String? = null,

    /**
     * type = 2(源类型为2时，即常规方式可解析到播放链接)
     * 如果playUrl规则解析得到的是一串字符串(如script中)，播放链接在该字符串内
     * 则通过以下正则表达式提取出播放链接
     * 当该值为空时，则代表playUrl内的规则可直接提取出播放直链
     */
    val playUrlRegex: String? = null,

    /**
     * 播放链接过滤器，可为空
     * 将视频播放链接中的一些特殊字符进行替换
     * 将Key替换为Value
     */
    val playUrlFilter: Map<String, String>? = null,

    /**
     * 播放器界面存放视频链接等信息的javascript
     */
    val playerJs: String? = null,

    /**
     * 目前仅源类型为2（二级页面）和3类型时生效
     */
    val playerJsFilter: Map<String, String>? = null,

    /**
     * 提取[.playerJs]的方式
     * 有正则提取与字符截取等两种抽取器
     */
    val playerExtractor: String? = null,

    /**
     * 如果[.playerExtractor]为截取(sub)的方式，则需要指定截取的前置标志符
     */
    val playerPrefix: String? = null,

    /**
     * 如果[.playerExtractor]为截取(sub)的方式，则需要指定截取的后置标志符，如果想截取某个特定字符后的全部内容，则可留空
     */
    val playerSuffix: String? = null,

    /**
     * 如果[.playerExtractor]为正则(regex)匹配的方式，则需要指定正则表达式
     * 缺乏测试，效果不能保证!
     */
    val playerRegex: String? = null,

    /**
     * 观看页，访问player链接transcoding
     */
    val isWatchTranscoding: Boolean = true,

    /**
     * 视频播放链接是否转码，即转换为%e2%c2%e7%46%a8这种形式
     */
    val isPlayUrlTranscoding: Boolean = true,
    /**
     * 是否强制转码视频播放链接，非强制的情况下仅会在视频链接中包含中文的情况下进行转义。
     */
    val isForcePlayUrlTranscoding: Boolean = false,

    /**
     * 视频链接的转码（编码）拦截器，此list中包含的字符会被保留，不会被编码。
     */
    val videoUrlTranscodingInterceptor: List<String>? = null,

    /**
     * 是否需要将m3u8文件保存到本地进行播放，仅对m3u8视频格式的链接生效
     * 主要用于防止某些视频链接只能访问一次，二次访问则失效的情况
     * 默认false
     */
    @SerializedName("isLocalPlay")
    @SerialName("isLocalPlay")
    val localPlay: Boolean = false,
) : Serializable {
    fun getTypeString() =
        when (type) {
            SourceType.TYPE_NORMAL -> "常规源"
            SourceType.TYPE_EXTENSION -> "扩展源"
            SourceType.TYPE_SECONDARY_PAGE -> "二级常规源"
            else -> "未知型源"
        }

    /**
     * 当前源是否是优质源
     * 品质大于等于8的源为优质源
     */
    fun isQualitySource() = quality >= 8
}