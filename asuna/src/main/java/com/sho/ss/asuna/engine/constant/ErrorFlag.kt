package com.sho.ss.asuna.engine.constant

/**
 * @author Sho Tan
 */
object ErrorFlag {
    /**
     * 没有可用的配置源
     * cause: enable = false
     */
    const val CONFIG_SOURCE_NO_ENABLE = 0

    /**
     * 配置源为空
     */
    const val CONFIG_SOURCE_INVALIDATE = 1

    /**
     * 解析搜索列表时对应的源为空
     */
    const val NO_SOURCE_WHEN_SEARCHING = 2

    /**
     * 解析视频详情页时，节点列表为空
     */
    const val NO_NODE_LIST = 3

    /**
     * 视频对应的相关详情页面url为空
     */
    const val NO_VIDEO_DETAIL_URL = 4

    /**
     * 超出预期的待解析链接
     */
    const val PARSE_URL_UNKNOWN = 5

    /**
     * 视频URL为空
     */
    const val EMPTY_VIDEO_URL = 6

    /**
     * 在解析过程中发生异常，通常用于不确定异常发生的原因等情况
     */
    const val EXCEPTION_WHEN_PARSING = 7

    /**
     * 在动态创建扩展爬虫处理器的时候未找到对应处理器
     */
    const val EXT_PROCESSOR_NOT_FOUND = 8

    /**
     * 缺少规则
     */
    const val RULE_MISSING = 9

    /**
     * 初始化引擎失败
     */
    const val INIT_ENGINE_EXCEPTION = 10

    /**
     * 播放链接提取方式超出预期
     */
    const val EXTRACTOR_UNKNOWN = 11

    /**
     * sub方式的提取标识符无效
     */
    const val EXTRACT_SYMBOL_INVALIDATE = 12

    /**
     * 请求API缺失
     */
    const val API_MISSING = 13

    /**
     * 解析源主域名无效
     */
    const val HOST_INVALIDATE = 14

    /**
     * PageShowURL链接无效
     */
    const val PAGE_URL_INVALIDATE = 15

    /**
     * PageSource中的reference引用的源无效
     */
    const val PAGE_SOURCE_REFERENCE_INVALIDATE = 16

    /**
     * TabSource无效
     */
    const val PAGE_SOURCE_INVALIDATE = 17

    /**
     * Page无效
     */
    const val PAGE_INVALIDATE = 18

    /**
     * 解析到超出预期的链接
     */
    const val UN_EXPECTED_URL = 19

    /**
     * 搜索链接无效
     */
    const val SEARCH_URL_INVALIDATE = 20

    /**
     * 前置标识符缺失
     */
    const val PREFIX_MISSING = 21

    /**
     * 抽取方式为regex时，使用正则表达式抽取过程中出错
     */
    const val REGEX_EXTRACT_ERROR = 22

    /**
     * 非法的视频链接，如：没有http协议头
     */
    const val URL_ILLEGAL = 23

    /**
     * 剧集链接无效
     */
    const val EPISODE_URL_INVALIDATE = 24

    /**
     * 播放器页面的链接无效
     */
    const val PLAYER_URL_INVALIDATE = 25

    /**
     * 未知的视频源类型
     */
    const val SOURCE_TYPE_UN_EXPECTED = 26

    /**
     * 待解析的目标Pag下标越界
     */
    const val TARGET_PAGE_INDEX_OUT_OF_BOUNDS = 27

    /**
     * 没有可用的Page,即所有请求的Page中isEnabled为false
     */
    const val NONE_ENABLED_PAGE = 28

    /**
     * 分类源配置为空
     */
    const val CATEGORY_SOURCE_IS_EMPTY = 29

    /**
     * 解析到的分类数据为空
     */
    const val CATEGORY_DATA_IS_EMPTY = 30

    /**
     * Video无效，如：""
     */
    const val VIDEO_INVALIDATE = 31

    /**
     * 未解析到数据
     */
    const val NO_PARSED_DATA = 32

    /**
     * BannerSource为空或无效
     */
    const val NO_BANNER_SOURCE = 33

    /**
     * Banner源网页链接无效
     */
    const val BANNER_URL_INVALIDATE = 34

    /**
     * 需要的数据为空或无效
     */
    const val DATA_INVALIDATE = 35

    /**
     * 在发送请求时出错
     */
    const val ERROR_ON_REQUEST = 36

    /**
     * 序列化数据时出错
     */
    const val SERIALIZED_ERROR = 37

    /**
     * 在配置了源的Extras数据后，处理器解析过程中该Extras数据缺失。
     * 可能原因是源配置被人为改动了。
     */
    const val EXTRAS_MISSING = 38

    /**
     * 解密时出错
     */
    const val DECRYPT_ERROR = 39

    /**
     * 正则表达式缺失
     */
    const val REGEX_MISSING = 40

    /**
     * 在解析剧集视频时源为空
     */
    const val NO_SOURCE_WHEN_VIDEO_PARSE = 41

    /**
     * 获取指定下标的节点失败，该下标处的节点不存在
     */
    const val NO_TARGET_NODE = 42

    /**
     * 目标剧集列表为空
     */
    const val TARGET_EPISODE_LIST_IS_EMPTY = 43

    /**
     * 给定的剧集范围索引越界
     */
    const val EPISODE_INDEX_RANGE_OUT_OF_BOUNDS = 44

    /**
     * 给定的剧集下标越界
     */
    const val EPISODE_INDEX_OUT_OF_BOUNDS = 45

    /**
     * 解析时获取对应的批量解析任务不存在
     */
    const val TARGET_PARSE_BATCH_NOT_EXISTS = 46
    
    @JvmStatic
    fun getFlagMessage(flag: Int): String =
        when(flag) {
            CONFIG_SOURCE_NO_ENABLE -> "CONFIG_SOURCE_NO_ENABLE"
            CONFIG_SOURCE_INVALIDATE -> "CONFIG_SOURCE_INVALIDATE"
            NO_SOURCE_WHEN_SEARCHING -> "NO_SOURCE_WHEN_SEARCHING"
            NO_NODE_LIST -> "NO_NODE_LIST"
            NO_VIDEO_DETAIL_URL -> "NO_VIDEO_DETAIL_URL"
            PARSE_URL_UNKNOWN -> "PARSE_URL_UNKNOWN"
            EMPTY_VIDEO_URL -> "EMPTY_VIDEO_URL"
            EXCEPTION_WHEN_PARSING -> "EXCEPTION_WHEN_PARSING"
            EXT_PROCESSOR_NOT_FOUND -> "EXT_PROCESSOR_NOT_FOUND"
            RULE_MISSING -> "RULE_MISSING"
            INIT_ENGINE_EXCEPTION -> "INIT_ENGINE_EXCEPTION"
            EXTRACTOR_UNKNOWN -> "EXTRACTOR_UNKNOWN"
            EXTRACT_SYMBOL_INVALIDATE -> "EXTRACT_SYMBOL_INVALIDATE"
            API_MISSING -> "API_MISSING"
            HOST_INVALIDATE -> "HOST_INVALIDATE"
            PAGE_URL_INVALIDATE -> "PAGE_URL_INVALIDATE"
            PAGE_SOURCE_REFERENCE_INVALIDATE -> "PAGE_SOURCE_REFERENCE_INVALIDATE"
            PAGE_SOURCE_INVALIDATE -> "PAGE_SOURCE_INVALIDATE"
            PAGE_INVALIDATE -> "PAGE_INVALIDATE"
            UN_EXPECTED_URL -> "UN_EXPECTED_URL"
            SEARCH_URL_INVALIDATE -> "SEARCH_URL_INVALIDATE"
            PREFIX_MISSING -> "PREFIX_MISSING"
            REGEX_EXTRACT_ERROR -> "REGEX_EXTRACT_ERROR"
            URL_ILLEGAL -> "URL_ILLEGAL"
            EPISODE_URL_INVALIDATE -> "EPISODE_URL_INVALIDATE"
            PLAYER_URL_INVALIDATE -> "PLAYER_URL_INVALIDATE"
            SOURCE_TYPE_UN_EXPECTED -> "SOURCE_TYPE_UN_EXPECTED"
            TARGET_PAGE_INDEX_OUT_OF_BOUNDS -> "TARGET_PAGE_INDEX_OUT_OF_BOUNDS"
            NONE_ENABLED_PAGE -> "NONE_ENABLED_PAGE"
            CATEGORY_SOURCE_IS_EMPTY -> "CATEGORY_SOURCE_IS_EMPTY"
            CATEGORY_DATA_IS_EMPTY -> "CATEGORY_DATA_IS_EMPTY"
            VIDEO_INVALIDATE -> "VIDEO_INVALIDATE"
            NO_PARSED_DATA -> "NO_PARSED_DATA"
            NO_BANNER_SOURCE -> "NO_BANNER_SOURCE"
            BANNER_URL_INVALIDATE -> "BANNER_URL_INVALIDATE"
            DATA_INVALIDATE -> "DATA_INVALIDATE"
            ERROR_ON_REQUEST -> "ERROR_ON_REQUEST"
            SERIALIZED_ERROR -> "SERIALIZED_ERROR"
            EXTRAS_MISSING -> "EXTRAS_MISSING"
            DECRYPT_ERROR -> "DECRYPT_ERROR"
            REGEX_MISSING -> "REGEX_MISSING"
            NO_SOURCE_WHEN_VIDEO_PARSE -> "NO_SOURCE_WHEN_VIDEO_PARSE"
            NO_TARGET_NODE -> "NO_TARGET_NODE"
            TARGET_EPISODE_LIST_IS_EMPTY -> "TARGET_EPISODE_LIST_IS_EMPTY"
            EPISODE_INDEX_RANGE_OUT_OF_BOUNDS -> "EPISODE_INDEX_RANGE_OUT_OF_BOUNDS"
            TARGET_PARSE_BATCH_NOT_EXISTS -> "TARGET_PARSE_BATCH_NOT_EXISTS"
            else -> "UNKNOWN_FLAG"
        }
}
