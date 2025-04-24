package com.sho.ss.asuna.engine.processor.ext;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor;
import com.sho.ss.asuna.engine.utils.SpiderUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/10/5 23:46:51
 * @description 电影先生-http://dyxs20.com/
 **/
public class MovieSirExt extends CommonSecondaryPageProcessor
{
    public MovieSirExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
    }

    @Override
    protected void onWatchPageVideoLinkParse(@NonNull Page page, @NonNull String url)
    {
        //该源的大部分线路在播放界面就可拿到播放直链
        if(whenNullNotifyFail(url, ErrorFlag.EPISODE_URL_INVALIDATE,"视频链接获取失败"))
        {
            try
            {
                //先对url进行转义
                url = URLDecoder.decode(url, StandardCharsets.UTF_8.name());
                System.out.println("播放界面的VideoUrl转义后：" + url);
            } catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }
            //url例子：https://m3u8.zhisongip.com:38741/video/ddf6ccce31c7550168996cdc5492d5a8.m3u8&dianshiju&next=http://dyxs11.com/paly-231794-12-2/
            //根据符号&分割获取链接，并判断是否为播放直链
            final String[] urls = url.split("&");
            if(whenNullNotifyFail(urls,ErrorFlag.EXCEPTION_WHEN_PARSING,"处理视频链接时出错"))
            {
                String videoUrl = urls[0];
                if(SpiderUtils.isVideoFileBySuffix(videoUrl))
                {
                    videoUrl = SpiderUtils.fixHostIfMissing(videoUrl,getHostByUrl(page.getUrl().get()));
                    System.out.println("检测到播放直链，不再向后解析：" + videoUrl);
                    notifyOnCompleted(videoUrl);
                }
                else
                {
                    super.onWatchPageVideoLinkParse(page, url);
                }
            }
        }
    }
}
