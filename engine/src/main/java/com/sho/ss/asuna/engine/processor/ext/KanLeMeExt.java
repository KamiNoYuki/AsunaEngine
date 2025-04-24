package com.sho.ss.asuna.engine.processor.ext;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/9/11 19:29:57
 * @description 看了么-https://www.ksksl.com
 **/
public class KanLeMeExt extends BaseThirdLevelPageProcessor
{
    public KanLeMeExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
        addParseTarget(1,getParsePlayerUrlConfigJsInstance());
    }

    /**
     * 解析存放播放器链接的js文件
     * @return iParseTarget
     */
    protected IParser getParsePlayerUrlConfigJsInstance()
    {
        return (page, html, url) ->
        {
            String videoUrl = page.getRequest().getExtra("videoUrl");
            if(whenNullNotifyFail(videoUrl,ErrorFlag.EMPTY_VIDEO_URL,"视频url参数缺失"))
            {
                //Js文件内容如下:
//                MacPlayer.Html = '<iframe border="0" src="https://ksksl.codjx.com/?url='+MacPlayer.PlayUrl+'&next='+ (!MacPlayer.PlayLinkNext?'':window.location.protocol+'//'+window.location.host+MacPlayer.PlayLinkNext) +'&title='+document.title.split("-")[0]+'" width="100%" height="100%" marginWidth="0" frameSpacing="0" allowfullscreen="true" marginHeight="0" frameBorder="0" scrolling="no" vspale="0" noResize></iframe>';
//                MacPlayer.Show();
                String body = $("//iframe/@src", html);
                System.out.println("播放器配置文件：" + body);
                if(whenNullNotifyFail(body,ErrorFlag.EXCEPTION_WHEN_PARSING,"接口信息无效!") && null != body)
                {
                    //截取src中的播放器接口
                    try
                    {
                        //播放器链接
                        String api = body.substring(0, body.indexOf("'"));
                        if(whenNullNotifyFail(api,ErrorFlag.API_MISSING,"播放器Api无效!"))
                        {
                            String playerUrl = api + videoUrl;
                            playerUrl = SpiderUtils.fixHostIfMissing(playerUrl,getHostByUrl(page.getUrl().get()));
                            handlePlayerUrl(playerUrl);
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING,e.getMessage());
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
        Spider.create(this)
                .thread(1)
                .addRequest(request)
                .runAsync();
    }

    protected IParser getPlayerPageTargetInstance()
    {
        return (page, html, url) ->
        {
//            System.out.println("正在解析播放器页面视频信息：" + html.get());
            if (whenNullNotifyFail(getPlayerPageJsXpath(), ErrorFlag.RULE_MISSING, "视频信息规则缺失"))
            {
//                System.out.println("播放器Js抽取规则：" + getPlayerPageJsXpath());
//                System.out.println("播放器页面：" + html.get());
                String videoJs = $(getPlayerPageJsXpath(), html);
                System.out.println("播放器Js配置信息：" + videoJs);
                if (whenNullNotifyFail(videoJs, ErrorFlag.EXCEPTION_WHEN_PARSING, "解析播放器参数失败,干扰节点可重试") && null != videoJs)
                {
                    handleVideoConfigJs(page,videoJs);
                }
            }
        };
    }

    @Override
    protected void doParseThirdPage(@NonNull Page page, @NonNull Html html, @NonNull String curPageUrl)
    {
        String json = Xpath.select("//body/text()",html);
        if(whenNullNotifyFail(json,ErrorFlag.EXCEPTION_WHEN_PARSING,"响应数据为空,干扰节点可重试"))
        {
            try
            {
                //视频播放链接
                String videoUrl = (String) JSONPath.eval(json,"$.url");
                String msg = (String) JSONPath.eval(json,"$.msg");
                if(whenNullNotifyFail(videoUrl,ErrorFlag.EMPTY_VIDEO_URL, TextUtils.isEmpty(msg) ? "视频链接为空" : msg))
                {
                    System.out.println("视频链接：" + videoUrl);
                    notifyOnCompleted(SpiderUtils.fixHostIfMissing(videoUrl,getHostByUrl(page.getUrl().get())));
                }
            }
            catch (JSONException e)
            {
                e.printStackTrace();
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING,"处理视频链接出错");
            }
        }
    }

    /**
     * 播放器页面解析完毕，重写该方法处理播放器页面解析到的参数
     *
     * @param page
     * @param videoUrl 视频链接
     */
    @Override
    protected void handleVideoUrl(@NonNull Page page, @NonNull String videoUrl)
    {
        try
        {
            JSONObject json = JSON.parseObject(videoUrl);
            if (whenNullNotifyFail(json, ErrorFlag.EXCEPTION_WHEN_PARSING, "空的视频参数,干扰节点可重试"))
            {
                String url = json.getString("url");
                String time = json.getString("time");
                String key = json.getString("key");
                if (whenNullNotifyFail(url, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频url参数为空") &&
                        whenNullNotifyFail(time, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频time参数为空") &&
                        whenNullNotifyFail(key, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频key参数为空"))
                {
                    Request request = new Request(getVideoParseApi(page));
                    SpiderUtils.initRequest(request,new UserAgentLibrary().getProxyUserAgent(),page.getUrl().get(),null,null);
                    SpiderUtils.applyMethod(request,HttpConstant.Method.POST);
                    Map<String, String> params = new LinkedHashMap<>();
                    params.put("url",url);
                    params.put("time",time);
                    params.put("key", key);
                    SpiderUtils.buildRequestParams(request,params);
                    Spider spider = SpiderUtils.buildSpider(this,request,2);
                    if(null != spider)
                    {
                        SpiderUtils.addListenerForSpider(spider, new SpiderListener()
                        {
                            @Override
                            public void onSuccess(Request request)
                            {

                            }

                            @Override
                            public void onError(Request request, Exception e)
                            {
                                e.printStackTrace();
                                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING,"解析视频出错啦");
                            }
                        });
                        spider.runAsync();
                    }
                    else
                        notifyOnFailed(ErrorFlag.INIT_ENGINE_EXCEPTION,"引擎初始化失败");
                }
            }
        } catch (JSONException e)
        {
            e.printStackTrace();
            notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "序列化视频参数出错");
        }
    }

    @Override
    protected void onWatchPageVideoLinkParse(@NonNull Page page, @NonNull String url)
    {
        if (whenNullNotifyFail(url, ErrorFlag.EPISODE_URL_INVALIDATE, "播放器参数无效"))
        {
            try
            {
                System.out.println("观看页面解析完毕:  " + url);
                //视频信息序列化为json
                com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(url);
                if(whenNullNotifyFail(json,ErrorFlag.EXCEPTION_WHEN_PARSING,"视频参数无效!"))
                {
                    String from = json.getString("from");
                    String videoUrl = json.getString("url");
                    if(whenNullNotifyFail(from,ErrorFlag.EXCEPTION_WHEN_PARSING,"接口类型无效!") && whenNullNotifyFail(videoUrl, ErrorFlag.EMPTY_VIDEO_URL,"视频链接参数无效!"))
                    {
                        //如果是可播放的直链，则直接回调播放
                        if(isAutoCheckVideoUrl() && SpiderUtils.isVideoFileBySuffix(videoUrl))
                        {
                            notifyOnCompleted(videoUrl);
                        }
                        else
                        {
                            //存放播放器地址的js文件链接
                            String playerUrl = getPlayerUrlConfigJsUrl(page,videoUrl,from);
                            if (whenNullNotifyFail(playerUrl, ErrorFlag.EPISODE_URL_INVALIDATE, "接口配置链接无效"))
                            {
                                Request request = new Request(playerUrl)
                                        .putExtra("videoUrl",videoUrl);//访问播放器页面需要该参数
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
                }
            }
            catch (JSONException e)
            {
                e.printStackTrace();
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING,e.getMessage());
            }
        }
    }

    private String getPlayerUrlConfigJsUrl(@NonNull Page page, String videoUrl, String from)
    {
        return super.getUrlOfPlayerPage(page,videoUrl)
                .replace("{from}",from);
    }

    /**
     * 解析视频播放链接的api
     * @return api
     */
    protected String getVideoParseApi(@NonNull Page page)
    {
        return getHostByUrl(page.getUrl().get()) + "/API.php";
    }

}
