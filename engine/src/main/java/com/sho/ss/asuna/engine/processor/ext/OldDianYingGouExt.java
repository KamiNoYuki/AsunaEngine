package com.sho.ss.asuna.engine.processor.ext;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONPath;
import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.Spider;
import com.sho.ss.asuna.engine.core.SpiderListener;
import com.sho.ss.asuna.engine.core.model.HttpRequestBody;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.core.utils.HttpConstant;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.common.BaseThirdLevelPageProcessor;
import com.sho.ss.asuna.engine.utils.JsonPathUtils;
import com.sho.ss.asuna.engine.utils.SpiderUtils;
import com.sho.ss.asuna.engine.utils.UserAgentLibrary;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/9/26 2:32:48
 * @description 电影狗-https://www.dydog.cc
 **/
public class OldDianYingGouExt extends BaseThirdLevelPageProcessor {
    public OldDianYingGouExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener) {
        super(entity, videoSource, episode, listener);
    }

    @Override
    protected void doParseThirdPage(@NonNull Page page, @NonNull Html html, @NonNull String curPageUrl) {
        String json = page.getRawText();
        System.out.println("response: " + json);
        if (whenNullNotifyFail(json, ErrorFlag.EXCEPTION_WHEN_PARSING, "响应数据为空")) {
            try {
                //视频播放链接
                String videoUrl = (String) JSONPath.eval(json, "$.url");
                String msg = (String) JSONPath.eval(json, "$.msg");
                if (whenNullNotifyFail(videoUrl, ErrorFlag.EMPTY_VIDEO_URL, TextUtils.isEmpty(msg) ? "视频解析失败" : msg)) {
                    System.out.println("视频链接：" + videoUrl);
                    notifyOnCompleted(SpiderUtils.fixHostIfMissing(videoUrl, getHostByUrl(page.getUrl().get())));
                }
            } catch (JSONException e) {
                e.printStackTrace();
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "处理视频链接出错");
            }
        }
    }

    /**
     * 处理播放器页面js信息
     *
     * @param page         page
     * @param playerExtras 视频url参数
     */
    @Override
    protected void handleVideoUrl(@NonNull Page page, @NonNull String playerExtras) {
        System.out.println("playerExtras -> " + playerExtras);
        playerExtras = applyFilter(playerExtras, videoSource.getPlayerJsFilter(), isUseDefaultFilter());
        System.out.println("applied filter: " + playerExtras);
        String url = JsonPathUtils.selAsString(playerExtras, "$.url");
        String key = JsonPathUtils.selAsString(playerExtras, "$.key");
        String time = JsonPathUtils.selAsString(playerExtras, "$.time");
        final String parseApi = getVideoParseApi(page);
        if (whenNullNotifyFail(url, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频url参数为空") &&
                whenNullNotifyFail(key, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频key参数为空") &&
                whenNullNotifyFail(time, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频time参数为空") &&
                whenNullNotifyFail(parseApi, ErrorFlag.API_MISSING, "videoApi缺失")) {
            Request request = new Request(parseApi);
            SpiderUtils.initRequest(request, new UserAgentLibrary().USER_AGENT_EDGE, page.getUrl().get(), null, null);
            SpiderUtils.applyMethod(request, HttpConstant.Method.POST);
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("url", url);
            params.put("key", key);
            params.put("time", time);
            request.setRequestBody(HttpRequestBody.form(params, "UTF-8"));
            System.out.println("parseRequest: " + request);
            Spider spider = SpiderUtils.buildSpider(this, request, 2);
            if (null != spider) {
                SpiderUtils.addListenerForSpider(spider, new SpiderListener() {
                    @Override
                    public void onError(Request request, Exception e) {
                        e.printStackTrace();
                        notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "视频解析失败啦");
                    }
                });
                spider.runAsync();
            } else
                notifyOnFailed(ErrorFlag.INIT_ENGINE_EXCEPTION, "引擎初始化失败");
        }
    }

    protected String getVideoParseApi(Page page) {
        final String api = "/API.php";
        try {
            final URL url = new URL(page.getUrl().get());
            var port = url.getPort();
            if (-1 != port) {
                return getHostByUrl(page.getUrl().get()) + ":" + port + api;
            }
        } catch (MalformedURLException ignored) {

        }
        return getHostByUrl(page.getUrl().get()) + api;
    }
}
