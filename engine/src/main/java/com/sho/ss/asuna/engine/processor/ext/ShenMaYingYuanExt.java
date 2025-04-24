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
import com.sho.ss.asuna.engine.utils.DecryptUtils;
import com.sho.ss.asuna.engine.utils.SpiderUtils;
import com.sho.ss.asuna.engine.utils.UserAgentLibrary;
import com.sho.ss.asuna.engine.utils.Xpath;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/9/25 0:53:35
 * @description 神马影院-https://www.smdyy.cc/
 * @deprecated 2022/10/1 00:14 该源已加入反爬机制，目前无法绕过
 **/
@Deprecated
public class ShenMaYingYuanExt extends BaseThirdLevelPageProcessor
{
    public ShenMaYingYuanExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
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
                    System.out.println("视频链接(raw)：" + videoUrl);
                    //视频链接解码
                    videoUrl = DecryptUtils.JsBase64Helper.atob(videoUrl);
                    videoUrl = videoUrl.substring(videoUrl.indexOf("http"),videoUrl.length() - 8);
                    System.out.println("视频链接(解码)：" + videoUrl);
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
            if (whenNullNotifyFail(json, ErrorFlag.EXCEPTION_WHEN_PARSING, "空的视频参数"))
            {
                String url = json.getString("url");
                String vKey = json.getString("vkey");
                String token = json.getString("token");
                //sign固定的
                String sign = "smdyycc";
                if (whenNullNotifyFail(url, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频url参数为空") &&
                        whenNullNotifyFail(vKey, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频vkey参数为空") &&
                        whenNullNotifyFail(token, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频token参数为空"))
                {
                    Request request = new Request(getVideoParseApi(page));
                    SpiderUtils.initRequest(request,new UserAgentLibrary().getProxyUserAgent(),page.getUrl().get(),null,null);
                    SpiderUtils.applyMethod(request,HttpConstant.Method.POST);
                    Map<String, String> params = new LinkedHashMap<>();
                    params.put("url",url);
                    params.put("vkey",vKey);
                    params.put("token",token);
                    params.put("sign",sign);
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
        } catch (JSONException e)
        {
            e.printStackTrace();
            notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "序列化视频参数出错");
        }
    }

    /**
     * 解析视频播放链接的api
     * @return api
     */
    protected String getVideoParseApi(@NonNull Page page)
    {
        return getHostByUrl(page.getUrl().get()) + "/player/xinapi.php";
    }
}
