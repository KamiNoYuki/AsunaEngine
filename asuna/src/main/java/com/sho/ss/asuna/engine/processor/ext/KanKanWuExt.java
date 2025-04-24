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
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.core.utils.HttpConstant;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.common.BaseThirdLevelPageProcessor;
import com.sho.ss.asuna.engine.utils.SpiderUtils;
import com.sho.ss.asuna.engine.utils.Xpath;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/9/26 3:08:09
 * @description 看看屋-https://www.kkw361.com/
 *  2022/12/3 网页反爬已关闭，可正常访问
 **/
public class KanKanWuExt extends BaseThirdLevelPageProcessor
{
    public KanKanWuExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
    }

    @Override
    protected void doParseThirdPage(@NonNull Page page, @NonNull Html html, @NonNull String curPageUrl)
    {
        String json = Xpath.select("//body/text()",html);
        if(whenNullNotifyFail(json,ErrorFlag.EXCEPTION_WHEN_PARSING,"响应数据为空"))
        {
            try
            {
                //视频播放链接
                String videoUrl = (String) JSONPath.eval(json,"$.url");
                String msg = (String) JSONPath.eval(json,"$.msg");
                if(whenNullNotifyFail(videoUrl,ErrorFlag.EMPTY_VIDEO_URL, TextUtils.isEmpty(msg) ? "视频链接为空" : msg))
                {
                    System.out.println("视频链接：" + videoUrl);
                    episode.setReferer(videoSource.getHost());
                    //这个源的高清线路，对userAgent有限制，必须与访问解析视频接口时携带的userAgent一致才能播放
                    Map<String,String> map = new LinkedHashMap<>();
                    map.put(HttpConstant.Header.USER_AGENT, (String) JSONPath.eval(json,"$.ua"));
                    System.out.println("processor -> 剧集Header = " + map);
                    episode.setHeader(map);
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

    @Override
    protected String getUrlOfPlayerPage(@NonNull Page page,@NonNull String fakeVideoUrl)
    {
        String api = (String) JSONPath.eval(fakeVideoUrl,"$.apiurl");
        String url = (String) JSONPath.eval(fakeVideoUrl,"$.url");
        return api + url;
    }

    @Override
    protected void onWatchPageVideoLinkParse(@NonNull Page page, @NonNull String url)
    {
        String videoUrl = (String) JSONPath.eval(url,"$.url");
        if(isAutoCheckVideoUrl() && SpiderUtils.isVideoFileBySuffix(videoUrl))
            notifyOnCompleted(videoUrl);
        else
            super.onWatchPageVideoLinkParse(page, url);
    }

    @Override
    protected void handleVideoUrl(@NonNull Page page, @NonNull String videoJson)
    {
        String url = (String) JSONPath.eval(videoJson,"$.url");
        String key = (String) JSONPath.eval(videoJson,"$.key");
        String time = (String) JSONPath.eval(videoJson,"$.time");
        if (whenNullNotifyFail(url, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频url参数为空") &&
                whenNullNotifyFail(key, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频key参数为空") &&
                whenNullNotifyFail(time, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频time参数为空"))
        {
            Request request = new Request(getVideoParseApi(page));
            SpiderUtils.initRequest(request,videoSource.getVideoApiUa(),page.getUrl().get(),null,null);
            SpiderUtils.applyMethod(request, HttpConstant.Method.POST);
            Map<String, String> params = new LinkedHashMap<>();
            params.put("url",url);
            params.put("key",key);
            params.put("time",time);
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
                        notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING,"解析视频出错啦");
                    }
                });
                spider.runAsync();
            }
            else
                notifyOnFailed(ErrorFlag.INIT_ENGINE_EXCEPTION,"引擎初始化失败");
        }
    }

    @NonNull
    private String getVideoParseApi(Page page)
    {
        return getHostByUrl(page.getUrl().get()) + "/API.php";
    }
}
