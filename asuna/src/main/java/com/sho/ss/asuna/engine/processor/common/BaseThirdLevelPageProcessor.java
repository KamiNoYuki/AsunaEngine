package com.sho.ss.asuna.engine.processor.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;

import java.util.Map;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/9/9 21:37:08
 * @description Base三级页面视频扩展处理器
 * TODO: 2022/9/10 尚未实现通用三级页面扩展处理器
 **/
public abstract class BaseThirdLevelPageProcessor extends CommonSecondaryPageProcessor {
    public BaseThirdLevelPageProcessor(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener) {
        super(entity, videoSource, episode, listener);
        addParseTarget(getThirdParseTargetInstance());
    }

    /**
     * 第三页面的解析器实例
     *
     * @return IParser
     */
    @NonNull
    protected IParser getThirdParseTargetInstance() {
        return this::doParseThirdPage;
    }

    /**
     * 解析第三个页面的数据
     *
     * @param page       page
     * @param html       html
     * @param curPageUrl 当前page的url
     */
    protected abstract void doParseThirdPage(@NonNull Page page, @NonNull Html html, @NonNull String curPageUrl);

    /**
     * 二级页面到此方法就结束，重写该方法实现请求第三个页面的逻辑
     *
     * @param videoUrl 视频链接
     */
    @Override
    protected abstract void handleVideoUrl(@NonNull Page page, @NonNull String videoUrl);

    /**
     * 请求视频链接时采用的线程数
     *
     * @return 默认1
     */
    protected int getRequestOfVideoUrlThreadCount() {
        return 1;
    }

    /**
     * 为空时默认为GET
     *
     * @return 请求方法
     */
    @Nullable
    protected String getVideoUrlMethod() {
        return videoSource.getVideoApiMd();
    }

    /**
     * 返回请求时携带的参数
     *
     * @return 参数
     */
    @Nullable
    protected Map<String, String> getVideoUrlRequestParam() {
        return videoSource.getVideoApiPm();
    }

    /**
     * 用户代理头
     *
     * @return 代理头
     */
    @Nullable
    protected String getVideoUrlUserAgent() {
        return videoSource.getVideoApiUa();
    }

    /**
     * 访问时携带的referer
     *
     * @return referer 可为空，为空默认用videoSource的全局referer
     */
    @Nullable
    protected String getVideoUrlReferer() {
        return videoSource.getVideoApiReferer();
    }

    /**
     * 访问时携带的cookie
     *
     * @return cookie, 可为空
     */
    @Nullable
    protected Map<String, String> getVideoUrlCookie() {
        return videoSource.getVideoApiCk();
    }

    /**
     * 指定访问时携带的header
     *
     * @return header, 可为空
     */
    @Nullable
    protected Map<String, String> getVideoUrlHeader() {
        return videoSource.getVideoApiHd();
    }
}
