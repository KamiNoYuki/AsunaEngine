package com.sho.ss.asuna.engine.processor.ext;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.SpiderListener;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.core.utils.HttpConstant;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.common.BaseThirdLevelPageProcessor;

import java.util.HashMap;

/**
 * @project: 启源视频
 * @author: Sho Tan.
 * @E-mail: 2943343823@qq.com
 * @created: 2022/5/7 0:17:56
 * @updated: 2023/4/22 1:32
 * @description: 久久影院
 * url: https://www.jiujiukanpian.com
 **/
public class JiuJiuExt extends BaseThirdLevelPageProcessor
{

    public JiuJiuExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
    }

    @Override
    protected void doParseThirdPage(@NonNull Page page, @NonNull Html html, @NonNull String curPageUrl)
    {
        System.out.println(html.get());
        HashMap<String, String> extras = videoSource.getExtras();
        if(whenNullNotifyFail(extras,ErrorFlag.RULE_MISSING,"视频链接规则缺失!") && null != extras) {
            String urlRule = (String) extras.get("videoJs");
            String prefix = (String) extras.get("urlPrefix");
            String suffix = (String) extras.get("urlSuffix");
            if(whenNullNotifyFail(urlRule,ErrorFlag.RULE_MISSING,"视频链接规则无效!") &&
            whenNullNotifyFail(prefix,ErrorFlag.PREFIX_MISSING,"前置标识符缺失!") &&
            whenNullNotifyFail(suffix,ErrorFlag.EXCEPTION_WHEN_PARSING,"后置标识符缺失!")) {
                String videoJs = $(urlRule,html);
                if(whenNullNotifyFail(videoJs,ErrorFlag.EXCEPTION_WHEN_PARSING,"未解析到链接信息!") && null != videoJs) {
                    videoJs = videoJs.replace(" ","");
                    String videoUrl = extractUrlWithSubstring(videoJs,prefix,suffix);
                    if(whenNullNotifyFail(videoUrl,ErrorFlag.EMPTY_VIDEO_URL,"获取到无效的视频链接!")) {
                        notifyOnCompleted(videoUrl);
                    }
                }
            }
        }
    }


    /**
     * 二级页面到此方法就结束，重写该方法实现请求第三个页面的逻辑
     * @param videoUrl 视频链接
     */
    @Override
    protected void handleVideoUrl(@NonNull Page page, @NonNull String videoUrl)
    {
        Request request = new Request(videoUrl);
        request.addHeader(HttpConstant.Header.USER_AGENT,getProxyUserAgent());
        request(request, 1, new SpiderListener()
        {
            @Override
            public void onError(Request request, Exception e)
            {
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING,"发送视频解析请求时失败!");
            }
        });
    }
}
