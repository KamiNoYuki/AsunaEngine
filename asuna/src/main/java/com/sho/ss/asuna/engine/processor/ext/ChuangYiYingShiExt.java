package com.sho.ss.asuna.engine.processor.ext;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.processor.common.CommonLeLeExtVideoProcessor;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.interfaces.ParseListener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/5/20 13:45:07
 * @description 创意影视-https://www.30dian.cn
 **/
public class ChuangYiYingShiExt extends CommonLeLeExtVideoProcessor
{
    public ChuangYiYingShiExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
    }

    /**
     * 存放接口配置信息的js链接
     * @return js path
     */
    @NonNull
    @Override
    protected String getApiConfigJsPath()
    {
        return "https://www.30dian.cn/static/js/playerconfig.js";
    }

    /**
     * 解密视频链接所需的Aes key所在的js文件链接
     * @return js url
     */
    @Override
    protected String getAesKeyJsPath(@NonNull String contextUrl)
    {
        return "https://vip.30dian.cn/js/play.js";
    }

    @Nullable
    @Override
    protected String extractKey(@NonNull String js)
    {
        Pattern pattern = Pattern.compile("token_key\\s=\\sCryptoJS.enc.Utf8.parse\\(\".*\"\\)");
        Matcher matcher = pattern.matcher(js);
        if(matcher.find())
        {
            String group = matcher.group();
            return group.substring(group.indexOf("\"") + 1, group.lastIndexOf("\")"));
        }
        return null;
    }

    @Nullable
    @Override
    protected String extractIv(@NonNull Html html)
    {
        String ivScript = $("concat(//script[contains(text(),'bt_token')]/text(),//script[contains(text(),'le_token')]/text())",html);
        if (null != ivScript && !isNullStr(ivScript))
            return ivScript.substring(ivScript.indexOf("\"") + 1, ivScript.lastIndexOf("\""));
        else
            return null;
    }

    @Nullable
    @Override
    protected JSONObject extractPlayerInfoJs(@NonNull String js)
    {
        String playerInfoJs = js.substring(js.indexOf("player_data=") + 12, js.lastIndexOf("}") + 1);
        if(!isNullStr(playerInfoJs))
            return JSON.parseObject(playerInfoJs);
        else
            return null;
    }

    @Nullable
    @Override
    protected String extractVideoUrl(String js)
    {
        String fakeUrl = js.substring(js.indexOf("\"url\":") + 6, js.indexOf("//视频链接"));
        return fakeUrl.substring(fakeUrl.indexOf("\"") + 1,fakeUrl.lastIndexOf("\""));
    }
}
