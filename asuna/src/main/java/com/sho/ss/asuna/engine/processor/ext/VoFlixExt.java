package com.sho.ss.asuna.engine.processor.ext;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.Spider;
import com.sho.ss.asuna.engine.core.SpiderListener;
import com.sho.ss.asuna.engine.core.processor.PageProcessor;
import com.sho.ss.asuna.engine.core.utils.HttpConstant;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor;
import com.sho.ss.asuna.engine.utils.DecryptUtils;
import com.sho.ss.asuna.engine.utils.SpiderUtils;

import java.util.Date;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/7/30 0:39:27
 * @description VoFlix-https://www.voflix.com   该源是三级页面
 **/
@Deprecated
public class VoFlixExt extends CommonSecondaryPageProcessor
{
    private final PageProcessor processor = this::parseResponseInfo;
    public VoFlixExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
    }

    @Override
    protected void handleVideoUrl(@NonNull Page page,@NonNull String videoUrl)
    {
        //转为Json
        JSONObject json = JSON.parseObject(videoUrl);
        if(whenNullNotifyFail(json, ErrorFlag.EMPTY_VIDEO_URL,"视频配置信息无效"))
        {
            String videoApi = videoSource.getVideoApi();
            if(whenNullNotifyFail(videoApi,ErrorFlag.API_MISSING,"视频前置Api缺失"))
            {
                String baseUrl = getHostByUrl(videoApi) + "/xplay/555tZ4pvzHE3BpiO838.php?";
                String builder = baseUrl + "tm=" + new Date().getTime() + "&" +
                        "url=" + json.getString("url") + "&" +
                        "vkey=" + json.getString("vkey") + "&" +
                        "token=" + json.getString("token") + "&" +
                        "sign=F4penExTGogdt6U8";
                Request request = new Request(builder)
                        .addHeader(HttpConstant.Header.REFERER,videoSource.getVideoApiReferer())
                        .addHeader(HttpConstant.Header.USER_AGENT,SpiderUtils.checkUserAgent(videoSource.getVideoApiUa(), videoSource));
                Spider spider = SpiderUtils.buildSpider(processor,request,1);
                if(whenNullNotifyFail(spider,ErrorFlag.INIT_ENGINE_EXCEPTION,"初始化引擎时出错!") && null != spider)
                {
                    SpiderUtils.addListenerForSpider(spider, new SpiderListener()
                    {
                        @Override
                        public void onError(Request request, Exception e)
                        {
                            String msg = null != e ? e.getMessage() : "解析播放链接时出错。";
                            notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING,msg);
                        }
                    });
                    spider.runAsync();
                }
            }
        }
    }

    protected void parseResponseInfo(Page response)
    {
        String responseMsg = $("//body/text()",response.getHtml());
        if(whenNullNotifyFail(responseMsg,ErrorFlag.EMPTY_VIDEO_URL,"视频请求无响应"))
        {
            JSONObject json = JSON.parseObject(responseMsg);
            if(whenNullNotifyFail(json, ErrorFlag.EMPTY_VIDEO_URL,"响应体信息无效") && null != json)
            {
                System.out.println("响应信息：" + json.toJSONString());
                if(TextUtils.equals(json.getString("msg"),"success"))
                {
                    String videoUrl = json.getString("url");
                    if(whenNullNotifyFail(json, ErrorFlag.EMPTY_VIDEO_URL,"视频链接获取失败!") && null != videoUrl)
                    {
                        //解密视频链接
                        videoUrl = DecryptUtils.removePretendChars(videoUrl);
                        notifyOnCompleted(videoUrl);
                    }
                }
                else
                    notifyOnFailed(ErrorFlag.EMPTY_VIDEO_URL,json.getString("msg"));
            }
        }
    }

    @Override
    protected void notifyOnCompleted()
    {
        super.notifyOnCompleted();
    }

    @Override
    protected void notifyOnCompleted(@NonNull String videoUrl)
    {
        super.notifyOnCompleted(videoUrl);
    }
}
