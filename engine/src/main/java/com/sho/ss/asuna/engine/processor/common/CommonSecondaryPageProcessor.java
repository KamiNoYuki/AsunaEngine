package com.sho.ss.asuna.engine.processor.common;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.Spider;
import com.sho.ss.asuna.engine.core.SpiderListener;
import com.sho.ss.asuna.engine.core.utils.HttpConstant;
import com.sho.ss.asuna.engine.constant.EngineConstant;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.VideoProcessor;
import com.sho.ss.asuna.engine.processor.base.BaseMultiPageProcessor;
import com.sho.ss.asuna.engine.utils.MapUtils;
import com.sho.ss.asuna.engine.utils.RegexHelper;
import com.sho.ss.asuna.engine.utils.SpiderUtils;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/7/29 14:17:26
 * @description 通用二级页面视频扩展处理器
 **/
public class CommonSecondaryPageProcessor extends BaseMultiPageProcessor {

    protected boolean autoDecodeUrlOnWatchPage = true;

    /**
     * true 开启(默认)/false 关闭 自动检测观看页面解析到的视频链接是否为一个可播放的链接
     * 如果是可播放的视频链接，则会停止向后解析，回调给播放器进行播放
     */
    private boolean autoCheckVideoUrl;

    public CommonSecondaryPageProcessor(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener) {
        super(entity, videoSource, episode, listener);
        this.autoCheckVideoUrl = videoSource.isAutoCheckVideoUrlInPlay();
        registryParseTargetQueue(
                getWatchPageTargetInstance(),
                getPlayerPageTargetInstance());
    }

    @NonNull
    protected VideoProcessor getNormalVideoProcessor(@NonNull Page page, @NonNull ParseListener<Episode> listener) {
        return new VideoProcessor(entity, Objects.requireNonNull(entity.getVideoSource()), episode, listener);
    }

    /**
     * 解析视频观看页的视频Url参数
     *
     * @return 解析任务
     */
    protected IParser getWatchPageTargetInstance() {
        return (page, html, url) ->
        {
            final VideoProcessor processor = getNormalVideoProcessor(page, new ParseListener<>() {
                @Override
                public void onStarted() {

                }

                @Override
                public void onCompleted(@NonNull Episode ep) {
                    final String videoUrl = ep.getVideoUrl();
                    if (whenNullNotifyFail(videoUrl, ErrorFlag.EMPTY_VIDEO_URL, "视频解析失败") && null != videoUrl) {
                        checkVideoUrl(page, videoUrl);
                    }
                }

                @Override
                public void onFail(int flag, String errMsg) {
                    notifyOnFailed(flag, errMsg);
                }
            });
            processor.setUseDefaultFilter(isUseDefaultFilter());
            processor.setAutoDecodePlayUrlByType(autoDecodeUrlOnWatchPage);
            processor.process(page);
        };
    }

    /**
     * 对链接进行检测，是否为视频链接，如果是，则直接回调播放，不再向后解析。如果未检测到视频链接，则向下执行解析。
     *
     * @param page page
     * @param url  可能包含视频链接的链接
     * @return 如果检测到视频链接，则返回true，否则false
     */
    protected boolean checkVideoUrl(@NonNull Page page, @NonNull String url) {
        return checkVideoUrl(page, url, true);
    }

    /**
     * 对链接进行检测，是否为视频链接，如果是，则直接回调播放，不再向后解析
     *
     * @param url                       视频链接
     * @param whenIsNotVideoUrlContinue 如果为false，则在未检测到视频链接时不会向下执行解析
     * @return 如果有可播放的视频链接，则返回true，并回调完成，如果不是视频链接，则返回false，并向下执行解析
     */
    protected boolean checkVideoUrl(@NonNull Page page, @NonNull String url, boolean whenIsNotVideoUrlContinue) {
        //自动检测视频链接
        if (autoCheckVideoUrl && SpiderUtils.isVideoFileBySuffix(url)) {
            notifyOnCompleted(
                    decodeUrlByType(
                            toAbsoluteUrl(url)
                    )
            );
            return true;
        }
        //可能链接混杂其他东西，尝试处理掉干扰信息再识别
        //如果链接包含http和两种常用的视频格式.m3u8|.mp4
        else if (autoCheckVideoUrl && url.contains("http") && (url.contains(".m3u8") || url.contains(".mp4"))) {
            //如果同时存在多个格式后缀，则不进行剥离
            if (url.contains(".m3u8") && !url.contains(".mp4") || !url.contains(".m3u8") && url.contains(".mp4")) {
                int httpIndexed = url.indexOf("http");
                String format = null;
                if (url.contains(".m3u8"))
                    format = ".m3u8";
                else if (url.contains(".mp4"))
                    format = ".mp4";
                if (null != format) {
                    int formatIndexed = url.indexOf(format);
                    //http必须在格式后缀索引的前面
                    if (httpIndexed < formatIndexed) {
                        System.out.println("发现疑似视频链接，尝试剥离：" + url);
                        //尝试剥离链接
                        String tryStr = url.substring(url.indexOf("http"), url.indexOf(format) + format.length());
                        System.out.println("剥离后链接：" + tryStr);
                        //剥离后如果是视频文件链接
                        if (SpiderUtils.isVideoFileBySuffix(tryStr)) {
                            System.out.println("校验通过，已剥离出视频链接：" + tryStr);
                            tryStr = toAbsoluteUrl(tryStr);
                            notifyOnCompleted(decodeUrlByType(tryStr));
                            return true;
                        }
                    }
                }
            }
        }
        if (whenIsNotVideoUrlContinue) {
            watchPageParseDone(episode, page, url);
        }
        return false;
    }

    private void watchPageParseDone(@NonNull Episode episode, @NonNull Page page, @NonNull String url) {
        //清除掉url，否则如果在后续解析中卡住，重试时会用该链接播放，会失败
        episode.clearVideoUrl();
        onWatchPageVideoLinkParse(page, url);
    }

    @Override
    protected void notifyOnFailed(int errCode, String errMsg) {
        if (!TextUtils.isEmpty(episode.getVideoUrl()))
            episode.clearVideoUrl();
        super.notifyOnFailed(errCode, errMsg);
    }

    /**
     * 该方法在解析到观看页中位于js中的videoUrl时回调
     *
     * @param url 通常来说(可播放的直链不应使用该处理器，而是{@link VideoProcessor})，该链接不是可供播放的链接，而是播放器页面的链接参数
     */
    protected void onWatchPageVideoLinkParse(@NonNull Page page, @NonNull String url) {
//        System.out.println("观看页视频参数解析");
        if (whenNullNotifyFail(url, ErrorFlag.EMPTY_VIDEO_URL, "未解析到视频信息!")) {
            if (whenNullNotifyFail(videoSource.getVideoApi(), ErrorFlag.API_MISSING, "videoApi缺失,请检查源是否配置VideoApi!")) {
                //播放器页面的链接
                String playerUrl = getUrlOfPlayerPage(page, url);
                if (whenNullNotifyFail(playerUrl, ErrorFlag.EPISODE_URL_INVALIDATE, "播放器链接无效")) {
                    Request request = new Request(playerUrl);
                    final String md = videoSource.getVideoApiMd();
                    request.setExtras(page.getRequest().getExtras());
                    SpiderUtils.initRequest(request, null, null, videoSource.getVideoApiCk(), videoSource.getVideoApiHd());
                    SpiderUtils.addUserAgent(request, videoSource.getVideoApiUa());
                    SpiderUtils.applyMethod(request, md);
                    if (!TextUtils.isEmpty(md) && md.equalsIgnoreCase(HttpConstant.Method.POST)) {
                        SpiderUtils.buildRequestParams(request, videoApiPmTransformer(url, playerUrl));
                    }
                    SpiderUtils.addReferer(videoSource, request, videoSource.getVideoApiReferer(), true);
                    Spider spider = SpiderUtils.buildSpider(this, request, 1);
                    if (whenNullNotifyFail(spider, ErrorFlag.EXCEPTION_WHEN_PARSING, "解析视频时出错") && null != spider) {
                        SpiderUtils.addListenerForSpider(spider, new SpiderListener() {
                            @Override
                            public void onError(Request request, Exception e) {
                                String msg = null != e ? e.getMessage() : "解析视频时出错";
                                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, msg);
                            }
                        });
                        spider.runAsync();
                    }
                }
            }
        }
    }

    /**
     * 如果需要对请求参数进行转换，则重写此方法即可。
     *
     * @param strings obj1为观看页解析后的数据，obj2为播放器链接
     * @return The transformed request params of videoApi.
     */
    protected Map<String, String> videoApiPmTransformer(String... strings) {
        Map<String, String> transformed = new HashMap<>();
        MapUtils.proxy(videoSource.getVideoApiPm(), (k, v) ->
        {
            try {
                String newVal = v.replace("{videoUrl}", strings[0])
                        .replace("{playerUrl}", strings[1]);
                String newKey = k.replace("{videoUrl}", strings[0])
                        .replace("{playerUrl}", strings[1]);
                transformed.put(newKey, newVal);
            } catch (Exception e) {
                e.printStackTrace();
                transformed.put(k, v);
            }
        });
        return transformed;
    }

    /**
     * 播放器页面的链接
     *
     * @param fakeVideoUrl 进入播放页面所需的
     * @param page         page
     * @return url
     */
    @Nullable
    protected String getUrlOfPlayerPage(@NonNull Page page, @NonNull String fakeVideoUrl) {
        String videoApi = videoSource.getVideoApi();
        return null != videoApi ? videoApi.replace("{videoUrl}", fakeVideoUrl)
                .replace("{host}", videoSource.getHost()) : null;
    }

    /**
     * 获取播放器页面解析实例
     *
     * @return 解析器实例
     */
    protected IParser getPlayerPageTargetInstance() {
        return (page, html, url) ->
        {
            System.out.println("正在解析播放器页面视频信息：" + page.getRawText());
            if (whenNullNotifyFail(getPlayerPageJsXpath(), ErrorFlag.RULE_MISSING, "视频信息规则缺失")) {
//                System.out.println("播放器Js抽取规则：" + getPlayerPageJsXpath());
//                System.out.println("播放器页面：" + html.get());
                String videoJs = $(getPlayerPageJsXpath(), html);
                System.out.println("播放器Js配置信息：" + videoJs);
                if (whenNullNotifyFail(videoJs, ErrorFlag.EXCEPTION_WHEN_PARSING, "播放器参数解析失败") && null != videoJs) {
                    handleVideoConfigJs(page, videoJs);
                }
            }
        };
    }


    /**
     * 处理存放视频配置信息的Javascript
     *
     * @param page          当前页面
     * @param videoConfigJs 视频配置信息
     */
    protected void handleVideoConfigJs(@NonNull Page page, @NonNull String videoConfigJs) {
//        System.out.println("处理视频配置Js: " + videoConfigJs);
        videoConfigJs = handleWhitespace(videoConfigJs);
        System.out.println("handleVideoConfigJs -> " + videoConfigJs);
        String extractor = videoSource.getPlayerExtractor();
        //没有指定抽取器，则认为链接可直接使用
        if (TextUtils.isEmpty(extractor))
            handleVideoUrl(page, videoConfigJs);
        else {
            switch (extractor) {
                case EngineConstant.EXTRACTOR_SUB:
                    String playerPrefix = videoSource.getPlayerPrefix();
                    if (whenNullNotifyFail(playerPrefix, ErrorFlag.PREFIX_MISSING, "前置抽取标志无效")) {
                        String videoUrl = extractUrlWithSubstring(videoConfigJs, playerPrefix, videoSource.getPlayerSuffix());
                        System.out.println("视频链接：" + videoUrl);
                        if (null != videoUrl)
                            handleVideoUrl(page, videoUrl);
                    }
                    break;
                case EngineConstant.EXTRACTOR_REGEX:
                    String playerRegex = videoSource.getPlayerRegex();
                    if (whenNullNotifyFail(playerRegex, ErrorFlag.REGEX_EXTRACT_ERROR, "正则表达式缺失") && null != playerRegex) {
                        final Map<String, String> params = RegexHelper.INSTANCE
                                .extractParamsWithRegex(
                                        videoConfigJs,
                                        playerRegex
                                        , msg -> {
                                            notifyOnFailed(ErrorFlag.REGEX_EXTRACT_ERROR, msg);
                                            return null;
                                        });
                        if (null != params) {
                            handleVideoUrl(page, params);
                        }
                    }
                    break;
                default://指定了一个未知的抽取器
                    notifyOnFailed(ErrorFlag.EXTRACTOR_UNKNOWN, "未知的抽取器");
                    break;
            }
        }
    }

    @NonNull
    protected String handleWhitespace(@NotNull String url) {
        return url.replace(" ", "");
    }

    /**
     * 采用VideoSource中的host转为绝对链接
     *
     * @param relativeUrl 相对链接
     * @return 绝对链接
     */

    protected String toAbsoluteUrl(@NonNull String relativeUrl) {
        return SpiderUtils.fixHostIfMissing(relativeUrl, videoSource.getHost());
    }

    protected void handleVideoUrl(@NonNull Page page, @NonNull String videoUrl) {
        if (whenNullNotifyFail(videoUrl, ErrorFlag.VIDEO_INVALIDATE, "无效视频链接")) {
            videoUrl = applyFilter(videoUrl, videoSource.getPlayerJsFilter(), isUseDefaultFilter());
            //相对链接转绝对链接
            if (videoSource.isToAbsoluteUrlInPlay())
                videoUrl = toAbsoluteUrl(page.getUrl().get(), videoUrl);
            System.out.println("视频链接：" + videoUrl);
            notifyOnCompleted(videoUrl);
        }
    }

    /**
     * 仅播放器链接抽取器为Regex时才会回调此方法
     * 处理播放器页面解析的参数
     * 此方法通常用于处理播放器页面使用Regex抽取的数据，并构造出下一个页面的请求链接
     *
     * @param params 播放器页面的解析数据
     */
    protected void handleVideoUrl(@NonNull Page page, @NonNull Map<String, String> params) {
        String videoUrl = params.get("value");
        if (whenNullNotifyFail(videoUrl, ErrorFlag.VIDEO_INVALIDATE, "无效的视频链接") && null != videoUrl) {
            handleVideoUrl(page, videoUrl);
        }
    }

    protected String getPlayerPageJsXpath() {
        return videoSource.getPlayerJs();
    }


    /**
     * 自动检测观看页面解析到的视频链接是否为一个可播放的链接
     *
     * @return true 开启(默认)/false 关闭
     */
    protected boolean isAutoCheckVideoUrl() {
        return autoCheckVideoUrl;
    }

    protected void setAutoCheckVideoUrl(boolean autoCheckVideoUrl) {
        this.autoCheckVideoUrl = autoCheckVideoUrl;
    }
}
