package com.sho.ss.asuna.engine.processor.ext;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.Spider;
import com.sho.ss.asuna.engine.core.SpiderListener;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.constant.EngineConstant;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor;
import com.sho.ss.asuna.engine.utils.JsonPathUtils;
import com.sho.ss.asuna.engine.utils.SpiderUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/10/4 16:00:56
 * @description TVB云播-http://www.tvyb03.com/
 **/
public class TVBYunBoExt extends CommonSecondaryPageProcessor {
    public TVBYunBoExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener) {
        super(entity, videoSource, episode, listener);
        clearAllTarget();
        registryParseTargetQueue(
                getWatchPageTargetInstance(),
                getParsePlayerUrlConfigJsInstance(),
                getPlayerPageTargetInstance()
        );
    }

    @Override
    protected void extensionParse(Page page, Html html) {
        System.out.println("extensionParse -> parseIndex：" + getParseQueueIndex());
        super.extensionParse(page, html);
    }

    /**
     * 解析存放播放器接口的js文件
     *
     * @return iParseTarget
     */
    protected IParser getParsePlayerUrlConfigJsInstance() {
        return (page, html, url) ->
        {
//            System.out.println("getParsePlayerUrlConfigJsInstance");
            String videoUrl = page.getRequest().getExtra("videoUrl");
            String from = page.getRequest().getExtra("from");
            if (whenNullNotifyFail(videoUrl, ErrorFlag.EMPTY_VIDEO_URL, "视频url参数缺失") && whenNullNotifyFail(from, ErrorFlag.API_MISSING, "播放器接口未知")) {
                String xml = page.getHtml().get();
                if (whenNullNotifyFail(xml, ErrorFlag.EXCEPTION_WHEN_PARSING, "空的播放器接口配置")) {
                    String config = extractUrlWithSubstring(xml, "player_list=", ",MacPlayerConfig");
                    if (whenNullNotifyFail(config, ErrorFlag.EXCEPTION_WHEN_PARSING, "未解析到播放器配置")) {
                        String apiInfo = JsonPathUtils.selAsString(config, "$." + from);
                        if (whenNullNotifyFail(apiInfo, ErrorFlag.API_MISSING, "未获取到接口信息") && null != apiInfo) {
                            //播放器接口信息
                            String api = JsonPathUtils.selAsString(apiInfo, "$.parse");
                            if (whenNullNotifyFail(api, ErrorFlag.EXCEPTION_WHEN_PARSING, "播放器接口无效!") && null != api) {
                                //拼接为完整的链接
                                String playerUrl = api + videoUrl;
                                if (whenNullNotifyFail(playerUrl, ErrorFlag.EXCEPTION_WHEN_PARSING, "播放器链接无效")) {
                                    handlePlayerUrl(playerUrl);
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    private void handlePlayerUrl(@NonNull String api) {
        api = toAbsoluteUrl(api);
//        System.out.println("播放器链接：" + api);
        Request request = new Request(api);
        String userAgent = SpiderUtils.checkUserAgent(videoSource.getVideoApiUa(), videoSource);
        SpiderUtils.initRequest(request, userAgent, null, videoSource.getVideoApiCk(), videoSource.getVideoApiHd());
        SpiderUtils.applyMethod(request, videoSource.getVideoApiMd());
        SpiderUtils.addReferer(videoSource, request, videoSource.getVideoApiReferer(), true);
//        System.out.println("播放器Request：" + request);
        Spider spider = Spider.create(this)
                .thread(1)
                .addRequest(request);
        SpiderUtils.addListenerForSpider(spider, new SpiderListener() {
            @Override
            public void onError(Request request, Exception e) {
                System.err.println("播放器解析请求失败: " + e.getMessage());
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "播放器解析请求失败");
            }
        });
        spider.runAsync();
    }

    /**
     * 观看页面数据解析完毕
     *
     * @param page
     * @param url  通常来说(可播放的直链不应使用该处理器，而是{@link com.sho.ss.asuna.engine.processor.VideoProcessor})，该链接不是可供播放的链接，而是播放器页面的链接参数
     */
    @Override
    protected void onWatchPageVideoLinkParse(@NonNull Page page, @NonNull String url) {
        if (whenNullNotifyFail(url, ErrorFlag.EPISODE_URL_INVALIDATE, "播放器参数无效")) {
            System.out.println("观看页面解析完毕:  " + url);
            String videoUrl = JsonPathUtils.selAsString(url, "$.url");
            String from = JsonPathUtils.selAsString(url, "$.from");
            System.out.println("from: " + from + ", videoUrl: " + videoUrl);
            if (whenNullNotifyFail(from, ErrorFlag.EXCEPTION_WHEN_PARSING, "from参数为空!") &&
                    whenNullNotifyFail(videoUrl, ErrorFlag.EMPTY_VIDEO_URL, "url参数为空!") &&
                    null != videoUrl && null != from
            ) {
                //解码url
                try {
                    final String decodedUrl = decodeUrlByType(videoUrl, EngineConstant.UrlDecodeType.JT);
                    System.out.println("尝试解密Jt格式的加密链接后：" + decodedUrl);
                    videoUrl = transcoding(decodedUrl);
                    System.out.println("解码后的视频链接：" + videoUrl);
                } catch (Exception e) {
                    System.err.println("failed to try decode: " + e.getMessage());
                }
                //如果是可播放的直链，则不再向后解析
                if (isAutoCheckVideoUrl() && SpiderUtils.isVideoFileBySuffix(videoUrl)) {
                    videoUrl = SpiderUtils.fixHostIfMissing(videoUrl, getHostByUrl(page.getUrl().get()));
                    notifyOnCompleted(videoUrl);
                } else {
                    //存放播放器地址的js文件链接
                    String playerUrl = getPlayerUrlConfigJsUrl(page, videoUrl);
                    if (whenNullNotifyFail(playerUrl, ErrorFlag.API_MISSING, "接口配置链接无效")) {
                        Request request = new Request(toAbsoluteUrl(page.getUrl().get(), playerUrl))
                                .putExtra("from", from)
                                .putExtra("videoUrl", videoUrl);
                        String userAgent = SpiderUtils.checkUserAgent(videoSource.getVideoApiUa(), videoSource);
                        SpiderUtils.initRequest(request, userAgent, null, videoSource.getVideoApiCk(), videoSource.getVideoApiHd());
                        SpiderUtils.applyMethod(request, videoSource.getVideoApiMd());
                        SpiderUtils.addRequestParamsForKeyword(playerUrl, true, request, videoSource.getVideoApiPm());
                        SpiderUtils.addReferer(videoSource, request, videoSource.getVideoApiReferer(), true);
                        System.out.println("请求存放播放器Api文件Request = " + request);
                        Spider spider = Spider.create(this)
                                .thread(1)
                                .addRequest(request);
                        SpiderUtils.addListenerForSpider(spider, new SpiderListener() {
                            @Override
                            public void onError(Request request, Exception e) {
                                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "播放器信息请求失败!");
                            }
                        });
                        spider.runAsync();
                    }
                }
            }
        }
    }

    protected String getPlayerUrlConfigJsUrl(@NonNull Page page, String url) {
        return super.getUrlOfPlayerPage(page, url)
                .replace("{time}", new SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(new Date(System.currentTimeMillis())));
    }
}
