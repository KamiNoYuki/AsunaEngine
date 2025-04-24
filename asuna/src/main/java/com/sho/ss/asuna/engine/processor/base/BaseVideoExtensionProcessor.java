package com.sho.ss.asuna.engine.processor.base;

import android.text.TextUtils;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.Spider;
import com.sho.ss.asuna.engine.core.SpiderListener;
import com.sho.ss.asuna.engine.core.model.HttpRequestBody;
import com.sho.ss.asuna.engine.core.processor.PageProcessor;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.core.utils.HttpConstant;
import com.sho.ss.asuna.engine.constant.EngineConstant;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.utils.DecryptUtils;
import com.sho.ss.asuna.engine.utils.RegexHelper;
import com.sho.ss.asuna.engine.utils.SpiderUtils;
import com.sho.ss.asuna.engine.utils.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import kotlin.jvm.functions.Function1;


/**
 * @author Sho
 * 该类处理无法通过常规方式进行解析的视频源
 */
@WorkerThread
public abstract class BaseVideoExtensionProcessor<S extends VideoSource, V extends Episode> extends BaseProcessor<Video, ParseListener<V>> implements PageProcessor {
    protected final V episode;
    protected final S videoSource;
    /**
     * 是否启用默认的字符过滤器
     */
    private boolean useDefaultFilter = true;

    public BaseVideoExtensionProcessor(@NonNull Video entity, S videoSource, @NonNull V episode, @Nullable ParseListener<V> listener) {
        super(entity, listener);
        this.episode = episode;
        this.videoSource = videoSource;
    }

    @Override
    public void process(Page page) {
        if (isRunning()) {
            extensionParse(page, page.getHtml());
        }
    }

    /**
     * 发送GET请求
     *
     * @param url url
     */
    protected void request(@NonNull String url) {
        request(url, HttpConstant.Method.GET);
    }

    protected void request(@NonNull String url, @NonNull String method) {
        request(url, method, null);
    }

    protected void request(@NonNull String url, @NonNull String method, @Nullable String ua) {
        request(url, method, ua, null);
    }

    protected void request(@NonNull String url, @NonNull String method, @Nullable String ua, @Nullable String referer) {
        request(1, url, method, null, ua, referer, null, null, (SpiderListener) null);
    }

    protected void request(@IntRange(from = 0) int threadCount, @NonNull String url, @Nullable String method,
                           @Nullable Map<String, Object> param, @Nullable String ua, @Nullable String referer,
                           @Nullable Map<String, String> cookie, @Nullable Map<String, String> header,
                           @Nullable SpiderListener... listeners) {
        Request request = new Request(url);
        String userAgent = SpiderUtils.checkUserAgent(ua, videoSource);
        SpiderUtils.initRequest(request, userAgent, null, cookie, header);
        SpiderUtils.applyMethod(request, method);
        if (null != param) {
            request.setRequestBody(HttpRequestBody.form(param, StandardCharsets.UTF_8.name()));
        }
        SpiderUtils.addReferer(videoSource, request, referer, true);
        request(request, threadCount, listeners);
    }

    protected void request(@NonNull Request request, @IntRange(from = 0) int threadCount, @Nullable SpiderListener... listeners) {
        Spider spider = Spider.create(this)
                .thread(threadCount <= 0 ? 1 : threadCount)
                .addRequest(request);
        if (null != listeners && listeners.length > 0) {
            List<SpiderListener> spiderListeners = Arrays.asList(listeners);
            spider.setSpiderListeners(spiderListeners);
        }
        spider.runAsync();
    }


    /**
     * playUrlDecode类型为1时采用该方法解密
     * TODO: 被加密的链接展示：JTY4JTc0JTc0JTcwJTczJTNBJTJGJTJGJTc3JTc3JTc3JTJFJTc0JTYxJTZGJTcwJTY5JTYxJTZFJTcwJTZDJ
     *
     * @param str 密文
     * @return 明文
     */
    public static String decodeUrlWithJtType(String str) {
        byte[] decodeChars = new byte[]{
                -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, 62, -1,
                -1, -1, 63, 52, 53, 54, 55, 56, 57,
                58, 59, 60, 61, -1, -1, -1, -1, -1,
                -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8,
                9, 10, 11, 12, 13, 14, 15, 16, 17,
                18, 19, 20, 21, 22, 23, 24, 25, -1,
                -1, -1, -1, -1, -1, 26, 27, 28, 29,
                30, 31, 32, 33, 34, 35, 36, 37, 38,
                39, 40, 41, 42, 43, 44, 45, 46, 47,
                48, 49, 50, 51, -1, -1, -1, -1, -1
        };
        int c1, c2, c3, c4;
        int i = 0;
        int len = str.length();
        StringBuilder out = new StringBuilder();
        while (i < len) {
            do {
                c1 = decodeChars[str.charAt(i++) & 0xff];
            } while (i < len && c1 == -1);
            if (c1 == -1)
                break;
            do {
                c2 = decodeChars[str.charAt(i++) & 0xff];
            } while (i < len && c2 == -1);
            if (c2 == -1)
                break;
            out.append((char) ((c1 << 2) | ((c2 & 0x30) >> 4)));
            do {
                c3 = str.charAt(i++) & 0xff;
                if (c3 == 61)
                    return out.toString();
                c3 = decodeChars[c3];
            } while (i < len && c3 == -1);
            if (c3 == -1)
                break;
            out.append((char) (((c2 & 0XF) << 4) | ((c3 & 0x3C) >> 2)));
            do {
                c4 = str.charAt(i++) & 0xff;
                if (c4 == 61)
                    return out.toString();
                c4 = decodeChars[c4];
            } while (i < len && c4 == -1);
            if (c4 == -1)
                break;
            out.append((char) (((c3 & 0x03) << 6) | c4));
        }
        return out.toString();
    }

    /**
     * 使用播放链接正则表达式提取播放链接
     *
     * @param html 待提取的目标网页
     * @return
     */
    protected String extractUrlWithRegex(String html) {
        return extractUrlWithRegex(html, videoSource.getPlayUrlRegex());
    }

    /**
     * 根据正则表达式提取值，不再推荐使用此方法，推荐用{@link RegexHelper#extractParamsWithRegex(String, String, Function1)}替换
     *
     * @param str   待提取的目标字符串
     * @param regex 正则表达式
     * @return
     */
    @Nullable
    protected String extractUrlWithRegex(String str, String regex) {
        if (null != videoSource && !TextUtils.isEmpty(str)) {
            if (null != regex && !TextUtils.isEmpty(regex)) {
                Matcher matcher = RegexHelper.INSTANCE.getPattern(regex).matcher(str);
                if (matcher.find()) {
                    try {
                        //视频链接
                        return matcher.group(1);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                } else {
                    notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "正则匹配失败");
                    System.out.println("正则匹配失败");
                }
            }
            //playUrlRegex为空
            else {
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "正则不能为空");
            }
        } else
            notifyOnFailed(ErrorFlag.RULE_MISSING, "缺少源信息");
        return null;
    }

    protected String extractUrlWithSubstring(String str) {
        return extractUrlWithSubstring(str, videoSource.getPlayPrefix(), videoSource.getPlaySuffix());
    }

    protected String extractUrlWithSubstring(String str, String prefix, @Nullable String suffix) {
        if (!TextUtils.isEmpty(str)) {
            if (str.contains(prefix)) {
                if (!TextUtils.isEmpty(prefix)) {
                    int prefixIndex = str.indexOf(prefix);
                    int suffixIndex = -1;
                    if (null != suffix && !TextUtils.isEmpty(suffix)) {
                        suffixIndex = str.lastIndexOf(suffix);
                    }
                    if (prefixIndex >= 0 && prefixIndex <= str.length() - 1) {
                        if (suffixIndex != -1)
                            return str.substring(prefixIndex + prefix.length(), suffixIndex);
                            //有指定后缀但是没有匹配到
                        else if (!TextUtils.isEmpty(suffix)) {
                            notifyOnFailed(ErrorFlag.EXTRACT_SYMBOL_INVALIDATE, "后置标识符未找到");
                            return null;
                        } else
                            return str.substring(prefixIndex + prefix.length());
                    } else
                        notifyOnFailed(ErrorFlag.EXTRACT_SYMBOL_INVALIDATE, "前置标识符无效");
                } else
                    notifyOnFailed(ErrorFlag.PREFIX_MISSING, "缺少前置标识符");
            } else
                notifyOnFailed(ErrorFlag.PREFIX_MISSING, "前置标识符未找到");
        } else
            notifyOnFailed(ErrorFlag.RULE_MISSING, "缺少源信息");
        return null;
    }

    /**
     * 通知UI解析完成
     */
    protected void notifyOnCompleted() {
        if (isRunning()) {
            if (null != listener) {
                switchToUIThread(episode, listener::onCompleted);
                setIsRunning(false);
            }
        }
    }

    protected void notifyOnCompleted(@NonNull String videoUrl) {
        if (isRunning()) {
            //再过滤一遍链接，以防止链接存在\\/这类转义符
            videoUrl = applyFilter(videoUrl, getNormalFilter());
            // 包含中文且允许转码，则进行转换
            if (videoSource.isForcePlayUrlTranscoding() || (StringUtils.isContainChinese(videoUrl) && videoSource.isPlayUrlTranscoding())) {
                videoUrl = SpiderUtils.encodingUrlPath(videoUrl, videoSource.isForcePlayUrlTranscoding(), videoSource.getVideoUrlTranscodingInterceptor());
                System.out.println("已对视频链接进行转码，转码后链接：" + videoUrl);
            }
            episode.setVideoUrl(videoUrl);
            System.out.println("episode = " + episode);
            notifyOnCompleted();
        }
    }

    /**
     * 该方法负责解析无法通过常规方式进行解析视频直链的源
     *
     * @param page page
     * @param html html
     */
    protected void extensionParse(Page page, Html html) {
        System.out.println("正在解析源[" + videoSource.getName() + "]的url[" + page.getUrl().get() + "]");
    }

    public boolean isUseDefaultFilter() {
        return useDefaultFilter;
    }

    public void setUseDefaultFilter(boolean useDefaultFilter) {
        this.useDefaultFilter = useDefaultFilter;
    }

    /**
     * 根据PlayUrlDecode类型解密链接
     * 目前仅一种解密方式
     *
     * @param url url
     * @return 解密后的url，如果有指定解密方式
     */
    protected String decodeUrlByType(@NonNull String url) {
        return decodeUrlByType(url, videoSource.getPlayUrlDecode());
    }

    protected String decodeUrlByType(@NonNull String url, int type) {
        switch (type) {
            case EngineConstant.UrlDecodeType.JT:
                //首先检查链接是否包含有被转码后的JT前缀
                if (url.contains("JT")) {
                    url = decodeUrlWithJtType(url);
                }
                break;
            case EngineConstant.UrlDecodeType.UNICODE://将url进行unicode解码
                url = StringUtils.unicodeDecode(url);
                break;
            case EngineConstant.UrlDecodeType.BASE64://base64解码
                url = DecryptUtils.JsBase64Helper.atob(url);
                break;
        }
        return url;
    }

    protected String transcoding(String url) {
        //仅在确认有被转码后才执行解码。
        if (!isNullStr(url) && StringUtils.isContainUrlEncodedChar(url)) {
            try {
                //发现转码后的unicode编码，需先解码还原unicode编码后再解码链接
                if (url.contains("%u")) {
                    url = StringUtils.unicodeDecode(url.replace("%u", "\\u"));
                    System.out.println("found illegal chars(encoded unicode),fixed: " + url);
                }
                System.out.println("transcoding -> " + url);
                return TextUtils.isEmpty(URLDecoder.decode(url, StandardCharsets.UTF_8.name())) ?
                        url : URLDecoder.decode(url, StandardCharsets.UTF_8.name())
                        .replace("%3A", ":")
                        .replace("%2F", "/")
                        .replace("%26", "&")
                        .replace("%3F", "?")
                        .replace("%3D", "=");
            } catch (UnsupportedEncodingException e) {
                return url;
            }
        }
        return url;
    }
}
