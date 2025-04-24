package com.sho.ss.asuna.engine.processor.ext;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.processor.base.BaseVideoExtensionProcessor;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.utils.Xpath;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * @project: 启源视频
 * @author: Sho Tan.
 * @E-mail: 2943343823@qq.com
 * @created: 2022/4/21 3:58:42
 * @description: 橙子动漫扩展处理器
 * @deprecated 截至2022/9/9 该源的观看界面需要登陆账号以后才能访问
 **/
@Deprecated
public class OrangeExt extends BaseVideoExtensionProcessor<VideoSource, Episode>
{
    public OrangeExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
    }

    @Override
    protected void extensionParse(Page page, Html html)
    {
        VideoSource source = entity.getVideoSource();
        String playUrl = source.getPlayUrl();
        if(!TextUtils.isEmpty(playUrl))
        {
            String script = Xpath.select(playUrl, html);
            if(!TextUtils.isEmpty(script))
            {
                try
                {
                    script = URLDecoder.decode(script,StandardCharsets.UTF_8.name());
                }
                catch (UnsupportedEncodingException ignored)
                {

                }
                String json = script.substring(script.indexOf("{"), script.indexOf("}") + 1);
                JSONObject jsonObject = JSON.parseObject(json);
                String url = jsonObject.getString("url");
                if(!url.startsWith("http://")||!url.startsWith("https://"))
                    url = videoSource.getVideoApi() + url;
                System.out.println("播放链接：" + url);
                if(!TextUtils.isEmpty(url))
                {
                    episode.setVideoUrl(url);
                    notifyOnCompleted();
                }
                else
                    notifyOnFailed(ErrorFlag.EMPTY_VIDEO_URL,"视频解析失败");
            }
            else
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING,"视频信息解析失败");
        }
        else
            notifyOnFailed(ErrorFlag.RULE_MISSING,"缺少规则，无法播放!");
    }
}
