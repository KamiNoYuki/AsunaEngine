package com.sho.ss.asuna.engine.processor.common;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.Spider;
import com.sho.ss.asuna.engine.core.utils.HttpConstant;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.utils.SpiderUtils;
import com.sho.ss.asuna.engine.utils.UserAgentLibrary;
import com.sho.ss.asuna.engine.utils.Xpath;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/9/10 22:34:28
 * @description
 **/
public abstract class SecondaryMultiApiExtProcessor extends CommonSecondaryPageProcessor {
    public SecondaryMultiApiExtProcessor(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener) {
        super(entity, videoSource, episode, listener);
        addParseTarget(1, getApiConfigParseInstance());
    }

    @Override
    protected void onWatchPageVideoLinkParse(@NonNull Page page, @NonNull String url) {
        if (whenNullNotifyFail(url, ErrorFlag.EPISODE_URL_INVALIDATE, "播放器参数无效")) {
            //接口配置文件的链接
            String apiConfigUrl = getUrlOfPlayerPage(page, url);
            if (whenNullNotifyFail(apiConfigUrl, ErrorFlag.EPISODE_URL_INVALIDATE, "接口文件链接无效")) {
                Request request = new Request(apiConfigUrl);
                request.putExtra("videoInfoJson", url);
                String userAgent = SpiderUtils.checkUserAgent(videoSource.getVideoApiUa(), videoSource);
                SpiderUtils.initRequest(request, userAgent, null, videoSource.getVideoApiCk(), videoSource.getVideoApiHd());
                SpiderUtils.applyMethod(request, videoSource.getVideoApiMd());
                SpiderUtils.addRequestParamsForKeyword(apiConfigUrl, true, request, videoSource.getVideoApiPm());
                SpiderUtils.addReferer(videoSource, request, videoSource.getVideoApiReferer(), true);
                Spider.create(this)
                        .thread(1)
                        .addRequest(request)
                        .runAsync();
//                System.out.println("正在请求解析播放器页面：\n" + request);
            }
        }
    }

    protected IParser getApiConfigParseInstance() {
        return (page, html, url) ->
        {
            String videoInfoJsonStr = page.getRequest().getExtra("videoInfoJson");
            if (whenNullNotifyFail(videoInfoJsonStr, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频信息丢失")) {
                try {
                    JSONObject videoInfoJson = JSON.parseObject(videoInfoJsonStr);
                    if (whenNullNotifyFail(videoInfoJson, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频信息序列化失败")) {
                        //获取该源的接口配置文件
                        String response = Xpath.select("//body/text()", html);
                        if (whenNullNotifyFail(response, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频接口信息无效")) {
                            response = response.replace(" ", "");
                            String jsonStr = trimApiConfigToJsonStr(response);
                            if (whenNullNotifyFail(jsonStr, ErrorFlag.EXCEPTION_WHEN_PARSING, "接口配置无效")) {
                                try {
                                    JSONObject json = JSON.parseObject(jsonStr);
                                    if (whenNullNotifyFail(jsonStr, ErrorFlag.EXCEPTION_WHEN_PARSING, "接口配置序列化失败")) {
                                        //播放器页面链接
                                        String playerUrl = handleApiConfigJson(videoInfoJson, json);
                                        if (whenNullNotifyFail(playerUrl, ErrorFlag.EXCEPTION_WHEN_PARSING, "播放器链接无效") && null != playerUrl) {
                                            onApiConfigParseDone(playerUrl);
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    /**
     * 接口配置文件解析完毕，已解析出对应的播放器链接地址
     *
     * @param playerUrl 播放器链接地址
     */
    protected void onApiConfigParseDone(@NonNull String playerUrl) {
        Request request = new Request(playerUrl);
        SpiderUtils.initRequest(request, new UserAgentLibrary().getProxyUserAgent(), null, null, null);
        SpiderUtils.applyMethod(request, HttpConstant.Method.GET);
        Spider.create(this)
                .thread(1)
                .addRequest(request)
                .runAsync();
    }

    /**
     * 根据接口配置信息返回正确的播放器链接
     *
     * @param videoInfoJson 视频信息json，包含视频url参数和from接口信息
     * @param apiConfigJson 接口配置信息json，包含所有接口信息
     * @return 播放器链接地址
     */
    @Nullable
    private String handleApiConfigJson(@NonNull JSONObject videoInfoJson, @NonNull JSONObject apiConfigJson) {
        //根据from的接口名称获取配置文件中的对应接口信息
        JSONObject apiInfo = apiConfigJson.getJSONObject(videoInfoJson.getString("from"));
        if (null != apiInfo) {
            //获取api
            String parse = apiInfo.getString("parse");
            if (!TextUtils.isEmpty(parse) && !TextUtils.isEmpty(videoInfoJson.getString("url"))) {
                //拼接为完整的链接
                return parse + videoInfoJson.getString("url");
            }
        }
        return null;
    }

    /**
     * 将接口配置js存放接口信息的内容优化为标准的json字符串
     *
     * @param config
     * @return
     */
    protected String trimApiConfigToJsonStr(String config) {
        return extractUrlWithSubstring(config, "player_list=", ",Mac");
    }
}
