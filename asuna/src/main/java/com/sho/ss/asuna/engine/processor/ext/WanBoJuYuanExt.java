package com.sho.ss.asuna.engine.processor.ext;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor;
import com.sho.ss.asuna.engine.utils.UserAgentLibrary;
import com.sho.ss.asuna.engine.utils.Xpath;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/10/9 18:41:36
 * @description 万博剧院-https://www.wanbotv.com/
 **/
public class WanBoJuYuanExt extends CommonSecondaryPageProcessor
{
    public WanBoJuYuanExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
        //注册视频链接解析的ParseTarget实例
        addParseTarget(getVideoUrlTargetInstance());
    }

    @Override
    protected void onWatchPageVideoLinkParse(@NonNull Page page, @NonNull String url)
    {
        String videoUrl = decodeUrlWithJtType(url);
        try
        {
            videoUrl = URLDecoder.decode(videoUrl, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
        videoUrl = toAbsoluteUrl(page.getUrl().get(),videoUrl);
        if (videoUrl.endsWith(".m3u8") || videoUrl.endsWith(".mp4"))
            notifyOnCompleted(videoUrl);
        else
            super.onWatchPageVideoLinkParse(page, videoUrl);
    }

    @Override
    protected void handleVideoUrl(@NonNull Page page,@NonNull String videoUrl)
    {
        //转为Json
        JSONObject json = JSON.parseObject(videoUrl);
        if (whenNullNotifyFail(json, ErrorFlag.EMPTY_VIDEO_URL, "视频配置信息无效"))
        {
            String parseApi = "https://tongyong.codjx.com/API.php";
            Map<String, String> params = new TreeMap<>();
            params.put("url", json.getString("url"));
            params.put("time", json.getString("time"));
            params.put("key", json.getString("key"));
            System.out.println("请求参数：" + params);
            try
            {
                String html = Jsoup.connect(parseApi)
                        .userAgent(new UserAgentLibrary().USER_AGENT_EDGE)
                        .ignoreContentType(true)
                        .method(Connection.Method.POST)
                        .data(params)
                        .execute()
                        .body();
                getVideoUrlTargetInstance().onPageReadied(null, Html.create(html),parseApi);
            } catch (IOException e)
            {
                e.printStackTrace();
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, TextUtils.equals(e.getMessage(),"timeout") ? "视频连接超时" : e.getMessage());
            }

        }
    }

    @NonNull
    protected String getParseApi()
    {
        return "https://wbavi.codjx.com/API.php";
    }

    /**
     * 该ParseTarget负责请求API解析出视频链接
     *
     * @return iParseTargetInstance
     */
    protected IParser getVideoUrlTargetInstance()
    {
        return (page, html, url) ->
        {
            String response = Xpath.select("//body/text()", html);
            System.out.println("解析视频响应：" + html);
            if (whenNullNotifyFail(response, ErrorFlag.EMPTY_VIDEO_URL, "解析接口无数据响应!"))
                handleParseVideoApiResponse(response);
        };
    }

    protected void handleParseVideoApiResponse(@NonNull String response)
    {
        /*
         * 解析视频链接的接口返回的响应数据json
         * {
         *   "code": 200,
         *   "success": 1,
         *   "url": "https://ajeee.codjx.com/API.php?time=1661780159697&key=cb4a1e26c4d8f85732659c7659fc62bb&path=6fab1c5a44981591084f7615154e0af3.m3u8",
         *   "type": "hls",
         *   "msg": "解析成功!",
         *   "From_Url": "RongXingVR-8072682309817",
         *   "From": "A4_1",
         *   "ip": "113.251.53.242",
         *   "time": "2022-08-29 21:35:59",
         *   "ua": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.5112.102 Safari/537.36 Edg/104.0.1293.70"
         * }
         */
        JSONObject responseJson = JSON.parseObject(response);
        if (whenNullNotifyFail(responseJson, ErrorFlag.EMPTY_VIDEO_URL, "序列化API数据失败"))
        {
            String msg = responseJson.getString("msg");
            String url = responseJson.getString("url");
            if (whenNullNotifyFail(url, ErrorFlag.EMPTY_VIDEO_URL, !TextUtils.isEmpty(msg) ? msg : "API响应链接为空!"))
                notifyOnCompleted(url);
        }
    }
}
