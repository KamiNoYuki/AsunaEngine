package com.sho.ss.asuna.engine.processor.ext;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.Spider;
import com.sho.ss.asuna.engine.core.SpiderListener;
import com.sho.ss.asuna.engine.core.selector.Json;
import com.sho.ss.asuna.engine.core.utils.HttpConstant;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor;
import com.sho.ss.asuna.engine.utils.DecryptUtils;
import com.sho.ss.asuna.engine.utils.SpiderUtils;
import com.sho.ss.asuna.engine.utils.UserAgentLibrary;

import org.jetbrains.annotations.NotNull;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/9/25 2:19:39
 * @description LIBVIOExt-https://www.libvio.me/
 **/
public class LIBVIOExt extends CommonSecondaryPageProcessor {
    public LIBVIOExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener) {
        super(entity, videoSource, episode, listener);
        addParseTarget(1, getParsePlayerUrlConfigJsInstance());
    }

    /**
     * 解析存放播放器链接的js文件
     *
     * @return iParseTarget
     */
    protected IParser getParsePlayerUrlConfigJsInstance() {
        return (page, html, url) ->
        {
            String videoUrl = page.getRequest().getExtra("videoUrl");
            if (whenNullNotifyFail(videoUrl, ErrorFlag.EMPTY_VIDEO_URL, "视频url参数缺失")) {
                //Js文件内容如下:
//                MacPlayer.Html = '<iframe border="0" src="https://sh-data-s01.chinaeast2.cloudapp.chinacloudapi.cn/ai.php?url=' + MacPlayer.PlayUrl + '&next=' + MacPlayer.PlayLinkNext + '&id=' + MacPlayer.Id + '&nid=' + MacPlayer.Nid + '" width="100%" height="100%" marginWidth="0" frameSpacing="0" marginHeight="0" frameBorder="0" scrolling="no" vspale="0" allowfullscreen="allowfullscreen" mozallowfullscreen="mozallowfullscreen" msallowfullscreen="msallowfullscreen" oallowfullscreen="oallowfullscreen" webkitallowfullscreen="webkitallowfullscreen" noResize></iframe>';
//                MacPlayer.Show();
                String body = $("//iframe/@src", html);
                System.out.println("播放器API配置文件：" + body);
                if (whenNullNotifyFail(body, ErrorFlag.EXCEPTION_WHEN_PARSING, "接口信息无效!") && null != body) {
                    //截取src中的播放器接口
                    try {
                        //播放器链接
                        String api = body.substring(0, body.indexOf("'"));
                        if (whenNullNotifyFail(api, ErrorFlag.API_MISSING, "播放器Api无效!")) {
                            String playerUrl = api + videoUrl;
                            handlePlayerUrl(page, playerUrl);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, e.getMessage());
                    }
                }
            }
        };
    }

    private void handlePlayerUrl(@NotNull Page page, @NonNull String api) {
        api = toAbsoluteUrl(api);
        System.out.println("播放器链接：" + api);
        Request request = new Request(api);
        String userAgent = SpiderUtils.checkUserAgent(videoSource.getVideoApiUa(), videoSource);
        SpiderUtils.initRequest(request, userAgent, null, videoSource.getVideoApiCk(), videoSource.getVideoApiHd());
        SpiderUtils.applyMethod(request, videoSource.getVideoApiMd());
        SpiderUtils.addReferer(request, getHostByUrl(page.getUrl().get()));
        System.out.println("播放器Request: " + request);
        final Spider spider = Spider.create(this)
                .thread(1)
                .addRequest(request);
        SpiderUtils.addListenerForSpider(spider, new SpiderListener() {
            @Override
            public void onError(Request request, Exception e) {
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, null == e.getMessage() ? "视频解析请求失败" : e.getMessage());
            }
        });
        spider.runAsync();
    }

    @Override
    protected void onWatchPageVideoLinkParse(@NonNull Page page, @NonNull String url) {
        if (whenNullNotifyFail(url, ErrorFlag.EPISODE_URL_INVALIDATE, "播放器参数无效")) {
            try {
                System.out.println("观看页面解析完毕:  " + url);
                JsonObject json = JsonParser.parseString(url).getAsJsonObject();
                //视频信息序列化为json
//                JSONObject json = JSON.parseObject(url);
                if (whenNullNotifyFail(json, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频参数无效!")) {
                    String from = null == json.get("from") ? null : json.get("from").getAsString();
                    String videoUrl = null == json.get("url") ? null : json.get("url").getAsString();
                    if (whenNullNotifyFail(from, ErrorFlag.EXCEPTION_WHEN_PARSING, "接口名为空!") && whenNullNotifyFail(videoUrl, ErrorFlag.EMPTY_VIDEO_URL, "视频链接参数为空!")) {
                        //存放播放器地址的js文件链接
                        String playerUrl = getPlayerUrlConfigJsUrl(page, videoUrl, from);
                        if (whenNullNotifyFail(playerUrl, ErrorFlag.EPISODE_URL_INVALIDATE, "接口配置链接无效")) {
                            Request request = new Request(playerUrl)
                                    .putExtra("videoUrl", videoUrl);//访问播放器页面需要该参数
                            String userAgent = SpiderUtils.checkUserAgent(videoSource.getVideoApiUa(), videoSource);
                            SpiderUtils.initRequest(request, userAgent, null, videoSource.getVideoApiCk(), videoSource.getVideoApiHd());
                            SpiderUtils.applyMethod(request, videoSource.getVideoApiMd());
                            SpiderUtils.addRequestParamsForKeyword(playerUrl, true, request, videoSource.getVideoApiPm());
                            SpiderUtils.addReferer(videoSource, request, videoSource.getVideoApiReferer(), true);
                            Spider.create(this)
                                    .thread(1)
                                    .addRequest(request)
                                    .runAsync();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, e.getMessage());
            }
        }
    }

    protected String getPlayerUrlConfigJsUrl(@NonNull Page page, String url, @NonNull String from) {
        return super.getUrlOfPlayerPage(page, url)
                .replace("{from}", from);
    }

    @Override
    protected IParser getPlayerPageTargetInstance() {
        return (page, html, url) ->
        {
            //该源的js有两种情况：一种在div.id = player后的script
            // 另一种在div.id=loading后的script，并且这种情况需要再次向接口携带参数解析视频链接
            //如果第一种未获取到结果，则尝试第二种方式
            if (whenNullNotifyFail(getPlayerPageJsXpath(), ErrorFlag.RULE_MISSING, "播放器信息规则缺失")) {
                String videoJs = $(getPlayerPageJsXpath(), html);
                System.out.println("播放器Js配置信息：" + videoJs);
                if (TextUtils.isEmpty(videoJs)) {
                    System.out.println("尝试方式2进行解析");
                    String js = $("substring-before(substring-after(//div[@id='loading']/following-sibling::script/text(),'config = '),'player')", html);
                    if (whenNullNotifyFail(js, ErrorFlag.EMPTY_VIDEO_URL, "解析视频链接失败") && null != js) {
                        String paramUrl = new Json(js).jsonPath("$.url").get();
                        String vKey = new Json(js).jsonPath("$.vkey").get();
                        String token = new Json(js).jsonPath("$.token").get();
                        String sign = "F4penExTGogdt6U8";
                        if (whenNullNotifyFail(paramUrl, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频url参数为空") &&
                                whenNullNotifyFail(vKey, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频vkey参数为空") &&
                                whenNullNotifyFail(token, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频token参数为空")) {
                            String api = getVideoParseApi(page);
                            api += "?tm=" + System.currentTimeMillis() + "&url=" + paramUrl + "&vkey=" + vKey + "&token=" + token + "&sign=" + sign;
                            Request request = new Request(api);
                            SpiderUtils.initRequest(request, new UserAgentLibrary().getProxyUserAgent(), page.getUrl().get(), null, null);
//                            SpiderUtils.addReferer(request,videoSource.getHost());
                            SpiderUtils.applyMethod(request, HttpConstant.Method.GET);
                            Spider spider = SpiderUtils.buildSpider(this, request, 2);
                            if (null != spider) {
                                SpiderUtils.addListenerForSpider(spider, new SpiderListener() {

                                    @Override
                                    public void onError(Request request, Exception e) {
                                        e.printStackTrace();
                                        notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "视频解析出错啦");
                                    }
                                });
                                //添加一个视频解析器实例
                                addParseTarget(getVideoParserInstance());
                                spider.runAsync();
                            } else
                                notifyOnFailed(ErrorFlag.INIT_ENGINE_EXCEPTION, "引擎初始化失败");
                        }
                    }
                } else
                    handleVideoConfigJs(page, videoJs);
            }
        };
    }

    protected IParser getVideoParserInstance() {
        return (page, html, url) ->
        {
            String json = $("//body/text()", html);
            if (whenNullNotifyFail(json, ErrorFlag.EXCEPTION_WHEN_PARSING, "序列化响应信息失败") && null != json) {
                String msg = new Json(json).jsonPath("$.msg").get();
                if (whenNullNotifyFail(json, ErrorFlag.EMPTY_VIDEO_URL, TextUtils.isEmpty(msg) ? "视频解析失败" : msg)) {
                    String videoUrl = new Json(json).jsonPath("$.url").get();
                    if (whenNullNotifyFail(videoUrl, ErrorFlag.EMPTY_VIDEO_URL, "视频链接为空")) {
                        System.out.println("视频链接(raw)：" + videoUrl);
                        //解码链接
                        videoUrl = DecryptUtils.JsBase64Helper.atob(videoUrl);
                        videoUrl = videoUrl.substring(videoUrl.indexOf("http"), videoUrl.length() - 8);
                        System.out.println("视频链接(解码)：" + videoUrl);
                        notifyOnCompleted(videoUrl);
                    }
                }
            }
        };
    }

    private String getVideoParseApi(Page page) {
        return getHostByUrl(page.getUrl().get()) + "/xplay/555tZ4pvzHE3BpiO838.php";
    }
}
