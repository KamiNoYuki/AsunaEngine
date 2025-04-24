package com.sho.ss.asuna.engine.processor.ext;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSONObject;
import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.model.HttpRequestBody;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.core.utils.HttpConstant;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.processor.base.BaseVideoExtensionProcessor;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.utils.UserAgentLibrary;
import com.sho.ss.asuna.engine.utils.Xpath;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @project: 启源视频
 * @author: Sho Tan.
 * @E-mail: 2943343823@qq.com
 * @created: 2022/4/17 4:23:58
 * @description: 乐猪TV爬虫扩展处理器
 * 解析视频的md5参数加密非常变态，因此废弃
 **/
@Deprecated
public class LeZhuExt extends BaseVideoExtensionProcessor<VideoSource,Episode>
{
    public LeZhuExt(@NonNull Video entity, @NonNull VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity,videoSource,episode, listener);
        System.out.println("LeZhuExt已创建");
    }

    @Override
    public void process(Page page)
    {
        if(page.getUrl().get().startsWith(entity.getVideoSource().getHost() + entity.getVideoSource().getPlayApi()))
        {
            extensionParse(page,page.getHtml());
        }
        else if(page.getUrl().get().startsWith(entity.getVideoSource().getHost() + "/hls2/index.php?url="))
        {
            parseRequestParams(page,page.getHtml());
        }
        else if(page.getUrl().get().startsWith(entity.getVideoSource().getHost() + entity.getVideoSource().getVideoApi()))
        {
            parseVideoJson(page,page.getHtml());
        }
    }

    private void parseVideoJson(Page page,Html html)
    {
        String responseBody = Xpath.select("//body", html);
        responseBody = responseBody.substring(responseBody.indexOf("{")-1,responseBody.lastIndexOf("}")+1);
        String json = responseBody.replaceAll("&amp;quot;","\"")
                .replaceAll("\\\\/","/")
                .replace("&amp;amp;","&");
        JSONObject jsonObject = JSONObject.parseObject(json);
        System.out.println("视频数据：" + jsonObject.toJSONString());
        String msg = jsonObject.getString("msg");
        if(TextUtils.equals(msg,"success"))
        {
            //获取url
            String videoUrl = jsonObject.getJSONObject("media").getString("url");
            episode.setVideoUrl(videoUrl);
            notifyOnCompleted();
        }
        else
        {
            notifyOnFailed(ErrorFlag.EMPTY_VIDEO_URL,msg);
        }
    }

    private void parseRequestParams(Page page,Html html)
    {
        String md5 = Xpath.select("//input[@id='hdMd5']/@value",html);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("id",page.getRequest().getExtra("param"));
        requestMap.put("type","vid");
        requestMap.put("siteuser","");
        requestMap.put("md5",md5);
        requestMap.put("referer","");
        requestMap.put("hd","");
        requestMap.put("lg","");
//        System.out.println("requestMap: " + requestMap);
        String url = entity.getVideoSource().getHost() + entity.getVideoSource().getVideoApi();
//        System.out.println("url=" + url);
        if(!TextUtils.isEmpty(url))
        {
            Request request = new Request(url)
                    .setMethod(HttpConstant.Method.POST);
            request.setRequestBody(HttpRequestBody.form(requestMap, StandardCharsets.UTF_8.name()));
            request.addHeader(HttpConstant.Header.USER_AGENT, new UserAgentLibrary().getProxyUserAgent());
            page.addTargetRequest(request);
        }
        else
            notifyOnFailed(ErrorFlag.PARSE_URL_UNKNOWN,"无效链接: " + url);
    }

    /**
     * 该方法负责解析无法通过常规方式进行解析视频直链的源
     *
     * @param page page
     * @param html html
     */
    @Override
    public void extensionParse(Page page, Html html)
    {
        //获取播放界面的Iframe[src]
        String script = Xpath.select(entity.getVideoSource().getPlayUrl(), html);
        //获取script中的变量
        String variables = script.split(";")[9];
        //得到后置url
        String param = variables.substring(variables.indexOf("'")+1,variables.lastIndexOf("'"));
//        System.out.println("解析到的Param:" + param);
        String url = entity.getVideoSource().getHost() + "/hls2/index.php?url=" + param;
        if(!TextUtils.isEmpty(url))
        {
            Request request = new Request(url).putExtra("param",param);
            //随机用户代理
            request.addHeader(HttpConstant.Header.USER_AGENT,new UserAgentLibrary().getProxyUserAgent());
            page.addTargetRequest(request);
        }
        else
            notifyOnFailed(ErrorFlag.PARSE_URL_UNKNOWN,"无效链接: " + url);
    }
}
