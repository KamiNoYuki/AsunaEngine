package com.sho.ss.asuna.engine.utils;

import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.Spider;
import com.sho.ss.asuna.engine.core.SpiderListener;
import com.sho.ss.asuna.engine.core.model.HttpRequestBody;
import com.sho.ss.asuna.engine.core.processor.PageProcessor;
import com.sho.ss.asuna.engine.core.utils.HttpConstant;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.utils.MapUtils;
import com.sho.ss.asuna.engine.utils.RegexHelper;
import com.sho.ss.asuna.engine.utils.StringUtils;
import com.sho.ss.asuna.engine.utils.UserAgentLibrary;

import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * @project: 启源视频
 * @author: Sho Tan.
 * @E-mail: 2943343823@qq.com
 * @created: 2022/5/13 2:33:01
 * @description:
 **/
public class SpiderUtils {
    /**
     * 校验给定的字符串是否是视频文件后缀
     *
     * @param target str
     * @return true 视频文件/false 非视频文件
     */
    public static boolean isVideoFileBySuffix(String target) {
        if (TextUtils.isEmpty(target)) return false;
        return RegexHelper.INSTANCE
                .getVideoRegex()
                .matcher(target)
                .matches();
    }

    /**
     * 校验给定的字符串是否是以图片文件格式后缀结尾的。
     *
     * @param target str
     * @return true 图片文件/false 非图片文件
     */
    public static boolean isImageFileBySuffix(String target) {
        if (TextUtils.isEmpty(target)) return false;
        return RegexHelper.INSTANCE
                .getPhotoRegex()
                .matcher(target)
                .matches();
    }

    /**
     * 校验给定的字符串是否是以音乐文件格式后缀结尾的。
     *
     * @param target str
     * @return true 音乐文件/false 非音乐文件
     */
    public static boolean isMusicFileBySuffix(String target) {
        if (TextUtils.isEmpty(target)) return false;
        return RegexHelper.INSTANCE
                .getAudioRegex()
                .matcher(target)
                .matches();
    }

    /**
     * 如果链接为相对链接，则在链接开头追加指定的host，将其转为绝对链接
     *
     * @param url  链接
     * @param host 要追加的host
     * @return 绝对url
     */
    public static String fixHostIfMissing(String url, String host) {
        if (!TextUtils.isEmpty(url) && !TextUtils.isEmpty(host)) {
            if (!url.startsWith("http")) {
                boolean needSeparator = !url.startsWith("/");
                url = host + (needSeparator ? "/" + url : url);
            }
        }
        return url;
    }

    /**
     * 获取给定url的全主机域名
     * 例如https://www.baidu.com/add/bbb/ccc 返回https://www.baidu.com
     *
     * @param target 目标url
     * @return 全host链接
     */
    @Nullable
    public static String getHostByUrl(String target) {
        if (TextUtils.isEmpty(target)) return null;
        URL url;
        try {
            url = new URL(target);
            return url.getProtocol() + "://" + url.getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将get请求参数追加到api
     *
     * @param params 请求参数
     * @return 追加后的完整url
     */
    public static String buildGetParams(@NonNull String keyword, boolean isEncoding, Map<String, String> params) {
        if (null == params || params.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        String joiner = "&";
        String getSymbol = "?";
        builder.append(getSymbol);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = applyKeywordByPlaceholder(entry.getKey(), keyword, isEncoding);
            String value = applyKeywordByPlaceholder((String) entry.getValue(), keyword, isEncoding);
            if (!builder.toString().endsWith(joiner) && !builder.toString().endsWith(getSymbol)) {
                builder.append(joiner);
            }
            builder.append(key)
                    .append("=")
                    .append(value);
        }
        return builder.toString();
    }

    /**
     * 将占位符替换为关键字
     *
     * @param beReplace  目标字符串
     * @param keyword    替换为该值
     * @param isEncoding 是否将关键字转码GBK2312再替换 true 转码/false 不转
     * @return 替换完毕的值
     */
    public static String applyKeywordByPlaceholder(String beReplace, String keyword, boolean isEncoding) {
        if (TextUtils.isEmpty(beReplace) || TextUtils.isEmpty(keyword)) return "";
        //TODO: 2022/4/18 郁闷，安卓的正则中这个}竟然必须转义,然后你转义还会警告你没必要转义!!Fuck!!66666
        String placeholder = "\\{kw\\}";
        if (isEncoding) {
            try {
                keyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException ignored) {
            }
        }
        return beReplace.replaceAll(placeholder, keyword);
    }

    /**
     * 返回一个完整的GET请求Search url，如：url?k=v&k2=v2...
     *
     * @param videoSource 源
     * @return url
     */
    public static String buildSearchGetUrl(@NonNull String keyword, @NonNull VideoSource videoSource) {
        Map<String, String> params = videoSource.getSearchPM();
        String searchUrl = videoSource.getHost() + applyKeywordByPlaceholder(videoSource.getSearchApi(), keyword, videoSource.isTranscoding());
        searchUrl += buildGetParams(keyword, videoSource.isTranscoding(), params);
        return searchUrl;
    }

    public static void initRequest(@NonNull Request request, String userAgent, String referer, Map<String, String> cookies, Map<String, String> headers) {
        addUserAgent(request, userAgent);
        addReferer(request, referer);
        addCookies(request, cookies);
        addHeaders(request, headers);
    }

    /**
     * 添加UserAgent到Request
     *
     * @param request     request
     * @param userAgent   userAgent
     * @param proxyIfNull 如果userAgent为空，是否代理添加一个随机UserAgent
     */
    public static void addUserAgent(Request request, String userAgent, boolean proxyIfNull) {
        //如果UserAgent为空且允许代理添加，则从UserAgent库随机生成一个UserAgent
        if (TextUtils.isEmpty(userAgent) && proxyIfNull) {
            userAgent = new UserAgentLibrary().getProxyUserAgent();
        }
        //添加UserAgent
        if (null != request && !TextUtils.isEmpty(userAgent)) {
            request.addHeader(HttpConstant.Header.USER_AGENT, userAgent);
        }
    }

    /**
     * 添加UserAgent，当传递的userAgent为空时，允许代理添加UserAgent
     *
     * @param request   request
     * @param userAgent userAgent
     */
    public static void addUserAgent(Request request, String userAgent) {
        addUserAgent(request, userAgent, true);
    }

    public static void addReferer(Request request, String referer) {
        //Referer
        if (!TextUtils.isEmpty(referer)) {
            request.addHeader(HttpConstant.Header.REFERER, referer);
        }
    }

    /**
     * 允许传递的referer为空时，自动代理添加主域名到重定向
     *
     * @param source      s
     * @param request     r
     * @param referer     r
     * @param proxyIfNull true 允许代理/false 不允许代理
     */
    public static void addReferer(VideoSource source, Request request, String referer, boolean proxyIfNull) {
        if (null != source && null != request && TextUtils.isEmpty(referer) && proxyIfNull)
            referer = source.getHost();//如果Referer为空，自动添加主域名为Referer
        addReferer(request, referer);
    }

    public static void addCookies(Request request, Map<String, String> cookies) {
        //Cookie
        if (null != request && null != cookies && !cookies.isEmpty()) {
            MapUtils.proxy(cookies, request::addCookie);
        }
    }

    public static void addHeaders(Request request, Map<String, String> headers) {
        //Headers
        if (null != request && null != headers && !headers.isEmpty()) {
            MapUtils.proxy(headers, request::addHeader);
        }
    }


    /**
     * 为请求添加数据参数，并将指定占位符替换为对应数据
     *
     * @param keyword  关键字
     * @param encoding 是否将关键字转GBK2312
     * @param request  请求
     * @param params   请求参数
     */
    public static void addRequestParamsForKeyword(@NonNull String keyword, boolean encoding, @NonNull Request request, Map<String, String> params) {
        if (null != params && !params.isEmpty()) {
            //需要先将搜索关键词添加到请求参数中
            Map<String, String> newParams = new HashMap<>();
            for (Map.Entry<String, String> param : params.entrySet()) {
                String key = param.getKey();
                String value = (String) param.getValue();
                key = applyKeywordByPlaceholder(key, keyword, encoding);
                value = applyKeywordByPlaceholder(value, keyword, encoding);
                newParams.put(key, value);
            }
            buildRequestParams(request, newParams);
        }
    }

    /**
     * @param request request
     * @param map     params
     */
    public static void buildRequestParams(Request request, Map<String, String> map) {
        if (null != request && null != map && !map.isEmpty())
            request.setRequestBody(HttpRequestBody.form(
                    MapUtils.convertMapToRequestParams(map),
                    StandardCharsets.UTF_8.name())
            );
    }

    public static void applyMethod(@NonNull Request request, @Nullable String method) {
        if (null == method || TextUtils.isEmpty(method))
            method = HttpConstant.Method.GET;//默认为GET请求
        //转大写
        switch (method.toUpperCase(Locale.ROOT)) {
            case HttpConstant.Method.POST:
                request.setMethod(HttpConstant.Method.POST);
                break;
            case HttpConstant.Method.DELETE:
                request.setMethod(HttpConstant.Method.DELETE);
                break;
            case HttpConstant.Method.CONNECT:
                request.setMethod(HttpConstant.Method.CONNECT);
                break;
            case HttpConstant.Method.HEAD:
                request.setMethod(HttpConstant.Method.HEAD);
                break;
            case HttpConstant.Method.PUT:
                request.setMethod(HttpConstant.Method.PUT);
                break;
            case HttpConstant.Method.TRACE:
                request.setMethod(HttpConstant.Method.TRACE);
                break;
            case HttpConstant.Method.GET:
            default:
                request.setMethod(HttpConstant.Method.GET);
                break;
        }
    }

    public static Spider buildSpider(PageProcessor processor, List<Request> requests, int threadNum) {
        if (null != processor && null != requests && threadNum >= 1) {
            Spider spider = Spider.create(processor)
                    .thread(threadNum);
            spider.addRequest(requests.toArray(new Request[0]));
            return spider;
        }
        return null;
    }

    public static void requestAttachToSpider(Spider spider, Request request) {
        if (null != spider && null != request) {
            spider.addRequest(request);
        }
    }

    @Nullable
    public static Spider buildSpider(PageProcessor processor, Request request, int threadNum) {
        if (null != processor && null != request && threadNum >= 1) {
            Spider spider = Spider.create(processor)
                    .thread(threadNum)
                    .setExitWhenComplete(true);
            requestAttachToSpider(spider, request);
            return spider;
        }
        return null;
    }

    public static void addListenerForSpider(@NonNull Spider spider, @NonNull SpiderListener... listeners) {
        if (listeners.length > 0) {
            List<SpiderListener> list = Arrays.asList(listeners);
            spider.setSpiderListeners(list);
        }
    }

    /**
     * 检查当前userAgent如果为空的话，则采用全局UserAgent
     *
     * @param userAgent ua
     * @param source    s
     * @return ua
     */
    public static String checkUserAgent(String userAgent, VideoSource source) {
        if (TextUtils.isEmpty(userAgent)) {
            if (TextUtils.isEmpty(source.getUserAgent()))
                return new UserAgentLibrary().getProxyUserAgent();
            else
                return source.getUserAgent();
        } else
            return userAgent;
    }

    /**
     * 传入两个参数，在两个参数不为空的情况下优先返回param1，如果param1为空则返回param2
     * 如果两者都为空则返回null
     *
     * @param param1 参数1
     * @param param2 参数2
     * @return param
     */
    public static <T> T getNotNullConfig(T param1, T param2) {
        if (null != param1)
            return param1;
        else return param2;
    }

    public static String encodingUrlPath(@NonNull String url) {
        return encodingUrlPath(url, false);
    }

    public static String encodingUrlPath(@NonNull String url, boolean isForceEncoding) {
        return encodingUrlPath(url, isForceEncoding, null);
    }

    /**
     * 将给定的url里path中包含中文或空格的part进行编码
     * @param url 待编码的链接
     * @param isForceEncoding 是否强制转码，false的情况下仅会在链接包含中文字符的情况下进行转码。
     * @return 如果给定的合法链接且有path，则返回处理后的url。否则返回传入的原始url
     */
    public static String encodingUrlPath(@NonNull String url, boolean isForceEncoding, @Nullable List<String> list) {
        url = url.replaceAll("\\s", " ");
        try {
            final URL mURL = new URL(url);
            final String[] paths = mURL.getPath().split("/");
            if (paths.length > 0) {
                final StringBuilder appender = new StringBuilder();
                for (String path : paths) {
                    if(!TextUtils.isEmpty(path)) {
                        if(!appender.toString().endsWith("/")) {
                            appender.append("/");
                        }
                        for (int i = 0; i < path.length(); i++) {
                            String str = String.valueOf(path.charAt(i));
                            //强制转码为true 且list不包含
                            if ((null == list || !list.contains(str)) && (isForceEncoding || StringUtils.isContainChinese(str))) {
                                appender.append(URLEncoder.encode(str, StandardCharsets.UTF_8.name()));
                            } else {
                                appender.append(str);
                            }
                        }
                    }
                }
                final String encodedPath = appender.toString()
                        .replace("+", "%20")//将+号转为空格转义符%20
                        .replace(" ", "%20");//防止未包含中文的path中包含空格
                return mURL.getProtocol() + "://" + mURL.getHost() + encodedPath;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }

    /**
     * 校验给定的url是否为一个正常的url
     *
     * @param url 需要校验的url
     * @return 如果该url不是正常的url则返回false，否则返回true
     */
    public static boolean isNotMalformedUrl(@Nullable String url) {
        if (TextUtils.isEmpty(url)) return false;
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public static String transformBrowserNameToUserAgent(@NonNull String shortcut) {
        return shortcut.replace("@edge", new UserAgentLibrary().USER_AGENT_EDGE)
                .replace("@chrome", new UserAgentLibrary().USER_AGENT_CHROME1)
                .replace("@firefox", new UserAgentLibrary().USER_AGENT_FIREFOX1)
                .replace("@opera", new UserAgentLibrary().USER_AGENT_OPERA1)
                .replace("@qq", new UserAgentLibrary().USER_AGENT_QQ1)
                .replace("@uc", new UserAgentLibrary().USER_AGENT_UC)
                .replace("@sougou", new UserAgentLibrary().USER_AGENT_SOGOU1);
    }

    public static String encodeStringToGbk(@NotNull String target) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return URLEncoder.encode(target, StandardCharsets.UTF_8);
        } else {
            try {
                return URLEncoder.encode(target, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                return target;
            }
        }
    }
}
