package com.sho.ss.asuna.engine.processor.ext;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.Spider;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor;
import com.sho.ss.asuna.engine.utils.DecryptUtils;
import com.sho.ss.asuna.engine.utils.SpiderUtils;

import java.io.UnsupportedEncodingException;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/8/3 16:45:37
 * @description 蓝光影院-https://www.lgyy.cc
 **/
public class LanGuangYingYuan extends CommonSecondaryPageProcessor
{
    public LanGuangYingYuan(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
        addParseTarget(1,getParsePlayerUrlConfigJsInstance());
    }

    public static void main(String[] args) throws UnsupportedEncodingException
    {

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
                //MacPlayer.Html = '<iframe border="0" src="/static/player/qq.php?url='+MacPlayer.PlayUrl+'&jump='+MacPlayer.PlayNewLinkNext+'&title='+MacPlayer.Vodname+'&thumb='+MacPlayer.Thumb+'&id='+MacPlayer.Id+'&nid='+MacPlayer.Nid+'" width="100%" height="100%" marginWidth="0" frameSpacing="0" marginHeight="0" frameBorder="0" scrolling="no" vspale="0" allowfullscreen="allowfullscreen" mozallowfullscreen="mozallowfullscreen" msallowfullscreen="msallowfullscreen" oallowfullscreen="oallowfullscreen" webkitallowfullscreen="webkitallowfullscreen" noResize></iframe>';
                //MacPlayer.Show();
                String body = $("//iframe/@src", html);
                System.out.println("播放器配置文件：" + body);
                if(whenNullNotifyFail(body,ErrorFlag.EXCEPTION_WHEN_PARSING,"接口信息无效!") && null != body)
                {
                    //截取src中的播放器接口
                    try
                    {
                        //播放器链接
                        String api = body.substring(0, body.indexOf("'+"));
                        if(whenNullNotifyFail(api,ErrorFlag.API_MISSING,"播放器Api无效!"))
                        {
                            String playerUrl = api + videoUrl;
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

    @Override
    protected void onWatchPageVideoLinkParse(@NonNull Page page, @NonNull String url)
    {
        if (whenNullNotifyFail(url, ErrorFlag.EPISODE_URL_INVALIDATE, "播放器参数无效"))
        {
            try
            {
                System.out.println("观看页面解析完毕:  " + url);
                //视频信息序列化为json
                JSONObject json = JSON.parseObject(url);
                if(whenNullNotifyFail(json,ErrorFlag.EXCEPTION_WHEN_PARSING,"视频参数无效!"))
                {
                    String from = json.getString("from");
                    String videoUrl = json.getString("url");
                    if(whenNullNotifyFail(from,ErrorFlag.EXCEPTION_WHEN_PARSING,"接口类型无效!") && whenNullNotifyFail(videoUrl, ErrorFlag.EMPTY_VIDEO_URL,"视频链接参数无效!"))
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
            catch (JSONException e)
            {
                e.printStackTrace();
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING,e.getMessage());
            }
        }
    }

   protected String getPlayerUrlConfigJsUrl(@NonNull Page page, String url,@NonNull String from)
   {
       return super.getUrlOfPlayerPage(page,url)
               .replace("{from}",from);
   }

    @Override
    protected void handleVideoUrl(@NonNull Page page, @NonNull String videoUrl)
    {
        System.out.println("原始视频链接：" + videoUrl);
        //解密后的链接
        String decUrl = DecryptUtils.JsBase64Helper.atob(videoUrl);
        try
        {
            System.out.println("解密后视频链接："+decUrl);
            decUrl = decUrl.substring(decUrl.indexOf("http"),decUrl.length() - 8);
            System.out.println("去干扰字符后：" + decUrl);
            super.handleVideoUrl(page, decUrl);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING,"解密链接错误");
            System.err.println("解密链接失败：" + e.getMessage());
        }
    }
}
