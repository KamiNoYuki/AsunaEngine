package com.sho.ss.asuna.engine.processor.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.base.BaseMultiApiExtVideoProcessor;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/5/20 13:52:42
 * @description
 **/
public class CommonMultiApiExtVideoProcessor extends BaseMultiApiExtVideoProcessor
{
    public CommonMultiApiExtVideoProcessor(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
    }


    @Nullable
    @Override
    protected String extractApiJson(@NonNull String js)
    {
        return js.substring(js.indexOf("player_list=") + 12, js.indexOf(",MacPlayerConfig.downer_list"));
    }

    @Nullable
    @Override
    protected JSONObject extractPlayerInfoJs(@NonNull String js)
    {
        String playerInfoJs = js.substring(js.indexOf("player_aaaa=") + 12, js.lastIndexOf("}") + 1);
        if(!isNullStr(playerInfoJs))
            return JSON.parseObject(playerInfoJs);
        else
            return null;
    }
}
