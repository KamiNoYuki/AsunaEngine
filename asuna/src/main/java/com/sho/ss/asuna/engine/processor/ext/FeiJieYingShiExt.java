package com.sho.ss.asuna.engine.processor.ext;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson2.JSONPath;
import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.Spider;
import com.sho.ss.asuna.engine.core.SpiderListener;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.core.utils.HttpConstant;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.common.BaseThirdLevelPageProcessor;
import com.sho.ss.asuna.engine.utils.SpiderUtils;
import com.sho.ss.asuna.engine.utils.UserAgentLibrary;
import com.sho.ss.asuna.engine.utils.Xpath;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/5/19 23:18:42
 * @description 飞捷影视-https://fjkkk.cn
 **/
public class FeiJieYingShiExt extends BaseThirdLevelPageProcessor
{
    public FeiJieYingShiExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
        addParseTarget(1,getAPIConfigParseInstance());
    }

    /**
     * 解析第三个页面的数据
     *
     * @param page page
     * @param html html
     * @param curPageUrl  url
     */
    @Override
    protected void doParseThirdPage(@NonNull Page page, @NonNull Html html, @NonNull String curPageUrl)
    {
        /**
         * {
         *     "code": "200",
         *     "success": "1",
         *     "name": "RongXingVR",
         *     "type": "hls",
         *     "url": "https://rongxingvr11.rx9696mv.com:8866/T2-Sy-vSj6Fk4TzFzYSLgGnujRhwxfn8qc8-7_cuDsHivgSqqLYSxk2Vv7Z5YM5xt4A5dnSYX-eVEfD6uipofA/RongXingVR.m3u8",
         *     "HongKongIDC": "idc.rongxingvr.com",
         *     "txt": "云服务器/物理机需求请访问楼上域名",
         *     "msg": "RongXingVR",
         *     "From": "https://vip.x1688mv.com:8866/",
         *     "From_Url": "RongXingVR-81125644060495"
         * }
         */
        System.out.println("解析请求响应：" + page.getRawText());
        String videoUrl = (String) JSONPath.eval(page.getRawText(),"$.url");
        if(whenNullNotifyFail(videoUrl,ErrorFlag.EMPTY_VIDEO_URL,"响应解析链接无效!"))
        {
            System.out.println("已解析到视频链接：" + videoUrl);
            notifyOnCompleted(videoUrl);
        }
    }

    /**
     * 二级页面到此方法就结束，重写该方法实现请求第三个页面的逻辑
     *
     * @param page page
     * @param param 解析视频所需的参数
     */
    @Override
    protected void handleVideoUrl(@NonNull Page page, @NonNull String param)
    {
        System.out.println("param-url: " + param);
        String parseApi = "https://pay.fjkkk.cn/API.php";
        Request request = new Request(parseApi).addHeader("origin",getHostByUrl(parseApi));
        SpiderUtils.initRequest(request,new UserAgentLibrary().getProxyUserAgent(),page.getUrl().get(),null,null);
        SpiderUtils.applyMethod(request, HttpConstant.Method.POST);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("url",param);
        SpiderUtils.buildRequestParams(request,params);
        Spider spider = SpiderUtils.buildSpider(this,request,2);
        if(null != spider)
        {
            SpiderUtils.addListenerForSpider(spider, new SpiderListener()
            {
                @Override
                public void onError(Request request, Exception e)
                {
                    e.printStackTrace();
                    notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING,"视频解析出错啦");
                }
            });
            spider.runAsync();
        }
        else
            notifyOnFailed(ErrorFlag.INIT_ENGINE_EXCEPTION,"引擎初始化失败");
    }

    /**
     * 负责解析存放播放器API的js文件内容
     * @return 解析器实例
     */
    protected IParser getAPIConfigParseInstance()
    {
        return (page, html, url) ->
        {
            String videoUrl = page.getRequest().getExtra("videoUrl");
            String from = page.getRequest().getExtra("from");
            if(whenNullNotifyFail(videoUrl, ErrorFlag.EMPTY_VIDEO_URL,"视频url参数缺失") && whenNullNotifyFail(from,ErrorFlag.API_MISSING,"播放器接口未知"))
            {
                String xml = page.getHtml().get();
                if(whenNullNotifyFail(xml,ErrorFlag.EXCEPTION_WHEN_PARSING,"空的播放器接口配置"))
                {
                    String config = extractUrlWithSubstring(xml,"player_list=",",MacPlayerConfig");
                    if(whenNullNotifyFail(config,ErrorFlag.EXCEPTION_WHEN_PARSING,"未解析到播放器配置"))
                    {
                        //播放器接口信息
                        String api = (String) JSONPath.eval(JSONPath.eval(config,"$." + from),"$.parse");
                        if(whenNullNotifyFail(api,ErrorFlag.EXCEPTION_WHEN_PARSING,"播放器接口无效!") && null != api)
                        {
                            //拼接为完整的链接
                            String playerUrl = api + videoUrl;
                            if(whenNullNotifyFail(playerUrl,ErrorFlag.EXCEPTION_WHEN_PARSING,"播放器链接无效"))
                            {
                                handlePlayerUrl(playerUrl);
                            }
                        }
                    }
                }
            }
        };
    }

    private void handlePlayerUrl(@NonNull String api)
    {
        api = toAbsoluteUrl(api);
        System.out.println("播放器链接：" + api);
        Request request = new Request(api);
        String userAgent = SpiderUtils.checkUserAgent(videoSource.getVideoApiUa(), videoSource);
        SpiderUtils.initRequest(request, userAgent, null, videoSource.getVideoApiCk(), videoSource.getVideoApiHd());
        SpiderUtils.applyMethod(request, videoSource.getVideoApiMd());
        SpiderUtils.addReferer(videoSource, request, videoSource.getVideoApiReferer(), true);
        System.out.println("播放器Request：" + request);
        Spider spider = Spider.create(this)
                .thread(1)
                .addRequest(request);
        SpiderUtils.addListenerForSpider(spider, new SpiderListener()
        {
            @Override
            public void onError(Request request, Exception e)
            {
                e.printStackTrace();
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING,"播放器解析请求失败");
            }
        });
        spider.runAsync();
    }

    /**
     * 播放器页面解析实例
     * @return 解析器实例
     */
    @Override
    protected IParser getPlayerPageTargetInstance()
    {
        return (page, html, url) ->
        {
            if (whenNullNotifyFail(getPlayerPageJsXpath(), ErrorFlag.RULE_MISSING, "视频信息规则缺失"))
            {
                String videoJs = $(getPlayerPageJsXpath(), html);
                //该源可能会出现播放器页面再嵌套一个iframe，因此如果未解析到播放链接，则再解析一下看是否包含iframe
                if (TextUtils.isEmpty(videoJs))
                {
                    String src = Xpath.select("//iframe/@src", html);
                    if(whenNullNotifyFail(src,ErrorFlag.EMPTY_VIDEO_URL,"未解析到视频链接"))
                    {
                        //iframe指向的播放器链接
                        String nextPlayerUrl = toAbsoluteUrl(page.getUrl().get(), src);
                        addParseTarget(getSecondaryPlayerParseInstance());
                        Request request = new Request(nextPlayerUrl)
                                .addHeader(HttpConstant.Header.USER_AGENT,new UserAgentLibrary().getProxyUserAgent());
                        Spider.create(this)
                                .thread(1)
                                .addRequest(request)
                                .runAsync();
                    }
                }
                else
                {
                    System.out.println("播放器Js：" + videoJs);
                    if (whenNullNotifyFail(videoJs, ErrorFlag.EXCEPTION_WHEN_PARSING, "解析播放器配置失败") && null != videoJs)
                    {
                        //处理解析所需的参数
                        handleVideoConfigJs(page,videoJs);
                    }
                }
            }
        };
    }

    @Override
    protected void onWatchPageVideoLinkParse(@NonNull Page page, @NonNull String url)
    {
        if(whenNullNotifyFail(url,ErrorFlag.EMPTY_VIDEO_URL,"播放信息解析失败!"))
        {
            String videoUrl = (String) JSONPath.eval(url,"$.url");
            String from = (String) JSONPath.eval(url,"$.from");
            if(whenNullNotifyFail(videoUrl,ErrorFlag.EMPTY_VIDEO_URL,"播放参数解析失败!") &&
                    whenNullNotifyFail(from,ErrorFlag.EXCEPTION_WHEN_PARSING,"接口名称解析失败!"))
            {
                System.out.println("视频解析参数：" + videoUrl + "|接口名：" + from);
                //播放器配置文件的链接
                String playerConfigUrl = getUrlOfPlayerPage(page,url);
                System.out.println("播放器配置文件链接：" + playerConfigUrl);
                if (whenNullNotifyFail(playerConfigUrl, ErrorFlag.EPISODE_URL_INVALIDATE, "播放器链接无效"))
                {
                    Request request = new Request(playerConfigUrl);
                    request.putExtra("videoUrl",videoUrl);
                    request.putExtra("from",from);
                    String userAgent = SpiderUtils.checkUserAgent(videoSource.getVideoApiUa(), videoSource);
                    SpiderUtils.initRequest(request, userAgent, null, videoSource.getVideoApiCk(), videoSource.getVideoApiHd());
                    SpiderUtils.applyMethod(request, videoSource.getVideoApiMd());
                    SpiderUtils.addRequestParamsForKeyword(playerConfigUrl, true, request, videoSource.getVideoApiPm());
                    SpiderUtils.addReferer(videoSource, request, videoSource.getVideoApiReferer(), true);
                    System.out.println("请求播放器页面：" + request);
                    Spider spider = SpiderUtils.buildSpider(this,request,1);
                    if(whenNullNotifyFail(spider,ErrorFlag.EXCEPTION_WHEN_PARSING,"解析视频时出错。") && null != spider)
                    {
                        SpiderUtils.addListenerForSpider(spider, new SpiderListener()
                        {
                            @Override
                            public void onError(Request request, Exception e)
                            {
                                String msg = null != e ? e.getMessage() : "解析视频时出错。";
                                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING,msg);
                            }
                        });
                        spider.runAsync();
                    }
//                System.out.println("正在请求解析播放器页面：\n" + request);
                }
            }
        }
    }

    protected IParser getSecondaryPlayerParseInstance()
    {
        return getPlayerPageTargetInstance();
    }

    @Override
    protected String getUrlOfPlayerPage(@NonNull Page page,@NonNull String fakeVideoUrl)
    {
        return super.getUrlOfPlayerPage(page,fakeVideoUrl) + new SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(new Date(System.currentTimeMillis()));
    }
}
