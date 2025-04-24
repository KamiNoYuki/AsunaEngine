package com.sho.ss.asuna.engine.processor.ext;

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
 * @created: 2022/5/13 1:10:41
 * @description: 麻豆扩展处理器-https://madoupj.com/
 **/
public class MaDouExt extends CommonVideoExtProcessor
{
    public MaDouExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
    }

    @Override
    public void process(Page page)
    {
        String url = page.getUrl().get();
        System.out.println("正在解析播放链接");
        //播放界面 -> 解析播放器链接参数 -> 进入播放器界面 -> 解析到播放链接
        if(whenNullNotifyFail(videoSource.getHost(),ErrorFlag.HOST_INVALIDATE,"Host无效!"))
        {
            if (url.startsWith(videoSource.getHost() + videoSource.getPlayApi()))
                parsePlayerUrl(page, page.getHtml());
            else if (url.startsWith(videoSource.getHost() + videoSource.getVideoApi()))
                parseVideoUrl(page.getHtml());
            else
                notifyOnFailed(ErrorFlag.UN_EXPECTED_URL,"未知的解析链接");
        }
    }

    private void parseVideoUrl(Html html)
    {
        String script = $("//div[@id='video']/following-sibling::script/text()", html);
        System.out.println("存放播放链接的script: ");
        if(whenNullNotifyFail(script,ErrorFlag.EXCEPTION_WHEN_PARSING,"解析播放链接失败!") && null != script)
        {
            script = script.replace(" ","");
            String url = extractUrlWithSubstring(script, "urls=\"", "\";varj");
            if(whenNullNotifyFail(url,ErrorFlag.EMPTY_VIDEO_URL,"视频链接解析失败!"))
            {
                if(url.startsWith("http"))
                {
                    episode.setVideoUrl(url);
                    notifyOnCompleted();
                }
                else
                    notifyOnFailed(ErrorFlag.URL_ILLEGAL,"非法的视频链接!");
            }
        }
    }

    private void parsePlayerUrl(Page page, Html html)
    {
        if(whenNullNotifyFail(videoSource.getPlayUrl(), ErrorFlag.RULE_MISSING,"视频链接解析规则缺失!"))
        {
            String playUrl = $(videoSource.getPlayUrl(),html);
            if(whenNullNotifyFail(playUrl,ErrorFlag.EXCEPTION_WHEN_PARSING,"解析播放链接失败!"))
            {
                String param = extractUrlWithSubstring(playUrl);
                if(whenNullNotifyFail(videoSource.getHost(),ErrorFlag.HOST_INVALIDATE,"Host无效!") && whenNullNotifyFail(videoSource.getVideoApi(),ErrorFlag.API_MISSING,"视频API缺失!"))
                {
                    String fullUrl = videoSource.getHost() + videoSource.getVideoApi() + param;
                    System.out.println("播放器链接：" + fullUrl);
                    Request request = new Request(fullUrl);
                    request.addHeader(HttpConstant.Header.REFERER,videoSource.getReferer())
                            .addHeader(HttpConstant.Header.USER_AGENT,new UserAgentLibrary().getProxyUserAgent());
                    page.addTargetRequest(request);
                }
            }
        }
    }
}
