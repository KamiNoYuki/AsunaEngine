package com.sho.ss.asuna.engine.constant;

/**
 * @project: SourcesEngine
 * @author: Sho Tan.
 * @E-mail: 2943343823@qq.com
 * @created: 2022/4/9 7:35:35
 * @description: 视频源类型常量类
 **/
public class SourceType
{
    /**
     * 视频源类型为扩展类型(需要特殊处理才能获取到视频Url，如多级解析、解密、破解伪装Url等)
     */
    public static final int TYPE_EXTENSION = 1;
    /**
     * 视频源类型为常规类型（无需通过特殊处理就能获取到视频Url）
     */
    public static final int TYPE_NORMAL = 2;
    /**
     * 二级页面类型 从观看页解析视频参数，访问播放器页面解析到视频链接
     */
    public static final int TYPE_SECONDARY_PAGE = 3;
}
