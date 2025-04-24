package com.sho.ss.asuna.engine.processor.ext;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.Spider;
import com.sho.ss.asuna.engine.core.processor.PageProcessor;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.core.utils.HttpConstant;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.processor.common.CommonVideoExtProcessor;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.VideoProcessor;
import com.sho.ss.asuna.engine.utils.UserAgentLibrary;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/7/6 2:33:22
 * @description 511美剧-https://www.511mj.com
 * @deprecated 截至2022/11/24 该源站已恢复访问，但播放界面访问404
 **/
@Deprecated
public class MeiJu511 extends CommonVideoExtProcessor implements ParseListener<Episode>
{
    private final VideoProcessor videoProcessor;
    /**
     * 负责解析FQ线路播放器界面的视频链接
     */
    private final PageProcessor fqPageProcessor = page -> parseVideoUrl(page.getHtml(),"//div[@id='playerCnt']/following-sibling::script[1]/text()","varurl='",";vartype");
    /**
     * 负责解析超清线路播放器界面的视频链接
     */
    private final PageProcessor hdPageProcessor = page -> parseVideoUrl(page.getHtml(),"//div[@id='ADtip']/following-sibling::script[1]/text()","\"url\":\"","\",//视频链接");

    public MeiJu511(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
        this.videoProcessor = new VideoProcessor(entity,entity.getVideoSource(),episode,this);
    }

    @Override
    protected void extensionParse(Page page, Html html)
    {
        super.extensionParse(page, html);
        String host = videoSource.getHost();
        String playApi = videoSource.getPlayApi();
        if(whenNullNotifyFail(host,ErrorFlag.HOST_INVALIDATE,"Host缺失!!") && whenNullNotifyFail(playApi,ErrorFlag.API_MISSING,"播放API前置缺失!!"))
        {
            if(page.getUrl().get().startsWith(host + playApi))
                videoProcessor.extensionParse(page,html);
            else
                notifyOnFailed(ErrorFlag.UN_EXPECTED_URL,"未知的解析链接");
        }
    }


    private void parseVideoUrl(Html html,@NonNull String configJsXpath,@NonNull String extractPrefix,@NonNull String extractSuffix)
    {
        System.out.println("解析视频链接");
        String config = $(configJsXpath, html);
        System.out.println("视频配置信息：" + config);
        if(whenNullNotifyFail(config,ErrorFlag.EXCEPTION_WHEN_PARSING,"解析视频信息失败!") && null != config)
        {
            String js = config.replace(" ", "");
            String url = extractUrlWithSubstring(js, extractPrefix, extractSuffix);
            if(whenNullNotifyFail(url,ErrorFlag.EXCEPTION_WHEN_PARSING,"视频链接解析失败!"))
            {
                notifyOnCompleted(url);
            }
        }
    }

    @Override
    @Deprecated
    public void onStarted()
    {

    }

    @Override
    public void onCompleted(@NonNull Episode episode)
    {
        //FQ线路的链接比较特殊，需要解析到播放器页面才能拿到播放链接
        if(episode.getVideoUrl().startsWith("FQ:"))
        {
            System.out.println("FQ线路，准备解析!");
            String videoApi = videoSource.getVideoApi();
            if(whenNullNotifyFail(videoApi, ErrorFlag.API_MISSING,"视频API前置缺失"))
                postRequest(videoApi,fqPageProcessor);
        }
        //结尾为html结束的说明需要二次解析
        else if(episode.getVideoUrl().endsWith(".html"))
        {
            //超高清线路的api
            String api  = "https://jk.58ttk.com/?url=";
            postRequest(api,hdPageProcessor);
        }
        else
        {
            if(!episode.getVideoUrl().startsWith("http"))
            {
                System.out.println("解析完毕：视频链接无效{" + episode.getVideoUrl() + "}");
                notifyOnFailed(ErrorFlag.URL_ILLEGAL,"视频链接无效!");
            }
            else
            {
                System.out.println("非FQ线路，解析完毕!");
                notifyOnCompleted(episode.getVideoUrl());
            }
        }
    }

    private void postRequest(String baseApi,@NonNull PageProcessor processor)
    {
        //播放器界面链接
        String playerUrl = baseApi + episode.getVideoUrl();
        Request request = new Request(playerUrl);
        request.setMethod(HttpConstant.Method.GET)
                .addHeader(HttpConstant.Header.REFERER,videoSource.getHost())
                .addHeader(HttpConstant.Header.USER_AGENT,new UserAgentLibrary().getProxyUserAgent());
        Spider.create(processor)
                .thread(5)
                .addRequest(request)
                .runAsync();
        System.out.println("解析请求已提交：" + playerUrl);
    }

    @Override
    public void onFail(int flag, String errMsg)
    {
        notifyOnFailed(flag,errMsg);
    }
}
