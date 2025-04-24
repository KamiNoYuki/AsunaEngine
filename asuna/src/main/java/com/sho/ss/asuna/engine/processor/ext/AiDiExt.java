package com.sho.ss.asuna.engine.processor.ext;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.core.utils.HttpConstant;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.base.BaseVideoExtensionProcessor;
import com.sho.ss.asuna.engine.utils.SpiderUtils;
import com.sho.ss.asuna.engine.utils.UserAgentLibrary;
import com.sho.ss.asuna.engine.utils.Xpath;


/**
 * @project: SourcesEngine
 * @author: Sho Tan.
 * @E-mail: 2943343823@qq.com
 * @created: 2022/4/9 7:21:23
 * @description: 负责解析AiDi视频源的视频Url
 **/
public class AiDiExt extends BaseVideoExtensionProcessor<VideoSource,Episode>
{

    public AiDiExt(@NonNull Video entity, @NonNull VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity,videoSource,episode, listener);
    }

    @Override
    public void process(Page page)
    {
//        System.out.println("正在解析艾迪视频");
        String url = page.getUrl().get();
        if(whenNullNotifyFail(videoSource.getHost(),ErrorFlag.HOST_INVALIDATE,"源Host缺失!"))
        {
            if (url.startsWith(videoSource.getHost() + videoSource.getPlayApi()))
            {
                extensionParse(page, page.getHtml());
//                System.out.println("解析播放页");
            }
            else if (url.startsWith(videoSource.getHost() + videoSource.getVideoApi()))
            {
                parseVideoUrl(page, page.getHtml());
//                System.out.println("解析播放器页面");
            }
            else
                notifyOnFailed(ErrorFlag.PARSE_URL_UNKNOWN, "未知的解析链接");
        }
    }

    /**
     * 解析出视频的url
     *
     * @param page page
     * @param html html
     */
    private void parseVideoUrl(Page page, Html html)
    {
        String script = Xpath.select("//div[@class='tj']/following-sibling::script/text()]", html);
        if(whenNullNotifyFail(script,ErrorFlag.EMPTY_VIDEO_URL,"播放配置解析失败!"))
        {
            //过滤掉script中的空格
            script = script.replace(" ", "");
            //截取存放有播放链接地址的变量内容
            String url = extractUrlWithSubstring(script, "url\":\"", "\",//视频链接");
            if(whenNullNotifyFail(url,ErrorFlag.EMPTY_VIDEO_URL,"播放链接无效!"))
            {
                url = SpiderUtils.fixHostIfMissing(url,videoSource.getHost());
                System.out.println("播放链接：" + url);
                notifyOnCompleted(url);
            }
        }
        setIsRunning(false);
    }

    /**
     * 该方法负责解析无法通过常规方式进行解析视频直链的源
     *
     * @param page page
     * @param html html
     */
    @Override
    protected void extensionParse(Page page, Html html)
    {
        VideoSource videoSource = entity.getVideoSource();
        String playUrl = videoSource.getPlayUrl();
        if (!TextUtils.isEmpty(playUrl))
        {
            String script = $(playUrl, html);
            if(!TextUtils.isEmpty(script) && null != script)
            {
                String json = script.substring(script.indexOf("{"), script.lastIndexOf("}") + 1);
                try
                {
                    JSONObject jsonObject = JSON.parseObject(json);
                    if(whenNullNotifyFail(jsonObject,ErrorFlag.EXCEPTION_WHEN_PARSING,"视频参数无效"))
                    {
                        if(whenNullNotifyFail(videoSource.getVideoApi(),ErrorFlag.API_MISSING,"播放链接API缺失!") &&
                                whenNullNotifyFail(jsonObject.get("url"),ErrorFlag.EMPTY_VIDEO_URL,"视频链接为空!"))
                        {
                            //获取到script中的url值，拼接为播放器链接
                            String playerUrl = SpiderUtils.fixHostIfMissing(videoSource.getVideoApi() + jsonObject.get("url"),videoSource.getHost());
                            if(whenNullNotifyFail(playerUrl,ErrorFlag.EMPTY_VIDEO_URL,"播放链接无效"))
                            {
                                Request request = new Request(playerUrl);
                                request.addHeader(HttpConstant.Header.USER_AGENT,new UserAgentLibrary().getProxyUserAgent());
                                SpiderUtils.addReferer(videoSource,request,videoSource.getVideoApiReferer(),true);
                                //加入到请求队列
                                page.addTargetRequest(request);
                            }
                        }
                    }
                }
                catch (JSONException e)
                {
                    notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING,"视频参数序列化失败");
                }
            }
            else//获取视频参数失败
            {
                //vip线路标志，如果该元素存在，则说明当前线路是vip线路，vip线路需要登录且是会员才能播放
                boolean needVip = !TextUtils.isEmpty($("//div[@class='play_tips lock_tips']",page.getHtml()));
                String errMsg = "未获取到视频参数!";
                if(needVip)
                    errMsg = "VIP线路无法播放(需要会员)";
                notifyOnFailed(ErrorFlag.EMPTY_VIDEO_URL,errMsg);
            }
        }
        else
            notifyOnFailed(ErrorFlag.RULE_MISSING, "播放链接规则缺失!");
    }
}
