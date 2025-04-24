package com.sho.ss.asuna.engine.processor.ext;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.core.utils.HttpConstant;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.processor.common.CommonVideoExtProcessor;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.utils.UserAgentLibrary;

/**
 * @project: 启源视频
 * @author: Sho Tan.
 * @E-mail: 2943343823@qq.com
 * @created: 2022/5/9 14:01:55
 * @description: 樱花动漫-http://www.yinghuacd.com
 **/
public class YingHuaAcdExt extends CommonVideoExtProcessor
{
    public YingHuaAcdExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
    }

    @Override
    protected void extensionParse(Page page, Html html)
    {
        String url = page.getUrl().get();
        String host = videoSource.getHost();
        String videoApi = videoSource.getVideoApi();
        if(whenNullNotifyFail(host,ErrorFlag.HOST_INVALIDATE,"源Host缺失") &&
                whenNullNotifyFail(videoApi, ErrorFlag.API_MISSING, "videoApi缺失") && null != videoApi)
        {
            if(url.startsWith(host + videoSource.getPlayApi()))
                parsePlayerUrl(page,page.getHtml());
            else if(url.startsWith(videoApi))
                parsePlayUrl(page.getHtml());
            else
                notifyOnFailed(ErrorFlag.UN_EXPECTED_URL,"解析到未知链接");
        }
    }

    private void parsePlayUrl(Html html)
    {
//        System.out.println("播放界面：" + html.get());
        String script = $("//script[@id='videosid']/following-sibling::script/text()", html);
        if(whenNullNotifyFail(script,ErrorFlag.EXCEPTION_WHEN_PARSING,"配置信息解析失败!") && null != script)
        {
            String url = script.substring(script.indexOf("url: \"") + 6, script.indexOf("\", pi"));
            if(whenNullNotifyFail(url,ErrorFlag.EMPTY_VIDEO_URL,"解析视频链接失败!"))
            {
                episode.setVideoUrl(url);
                notifyOnCompleted();
                setIsRunning(false);
            }
        }
    }

    private void parsePlayerUrl(Page page, Html html)
    {
        String playUrlRule = videoSource.getPlayUrl();
        if(whenNullNotifyFail(playUrlRule, ErrorFlag.RULE_MISSING,"播放链接规则缺失!"))
        {
            String playerUrl = $(playUrlRule,html);
            if(null != playerUrl && whenNullNotifyFail(playerUrl, ErrorFlag.EMPTY_VIDEO_URL,"播放链接解析失败!"))
            {
                String videoApi = videoSource.getVideoApi();
                if(whenNullNotifyFail(videoApi,ErrorFlag.API_MISSING,"视频API缺失!"))
                {
                    if(!playerUrl.startsWith(videoApi))
                        playerUrl = videoApi + playerUrl;
                    Request request = new Request(playerUrl);
                    String playUrlUA = TextUtils.isEmpty(videoSource.getPlayUrlUA()) ? new UserAgentLibrary().getProxyUserAgent() : videoSource.getPlayUrlUA();
                    request.addHeader(HttpConstant.Header.USER_AGENT,playUrlUA);
                    page.addTargetRequest(request);
                }
            }
        }
    }
}
