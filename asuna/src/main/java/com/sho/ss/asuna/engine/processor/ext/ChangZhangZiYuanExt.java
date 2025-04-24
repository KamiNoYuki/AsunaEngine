package com.sho.ss.asuna.engine.processor.ext;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.constant.EngineConstant;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.VideoProcessor;
import com.sho.ss.asuna.engine.processor.base.BaseLeLeVideoExtProcessor;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/10/7 14:08:14
 * @description 厂长资源-https://www.czspp.com/
 **/
public class ChangZhangZiYuanExt extends VideoProcessor
{

    public ChangZhangZiYuanExt(@NonNull Video entity, @NonNull VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
    }

    @Override
    public void extensionParse(Page page, Html html)
    {
        VideoSource source = entity.getVideoSource();
        //播放链接规则
        String playUrl = source.getPlayUrl();
        if(!TextUtils.isEmpty(playUrl))
        {
            System.out.println("playUrlRule: " + playUrl);
            String str = $(playUrl, html);
            System.out.println("playUrl: " + str);
            if(!TextUtils.isEmpty(str))
            {
                if(TextUtils.equals(source.getPlayExtractor(), EngineConstant.EXTRACTOR_REGEX))
                    postPlayUrl(page,extractUrlWithRegex(str));
                else if(TextUtils.equals(source.getPlayExtractor(),EngineConstant.EXTRACTOR_SUB))
                    postPlayUrl(page,extractUrlWithSubstring(str));
                else if(TextUtils.isEmpty(source.getPlayExtractor()))
                    postPlayUrl(page,str);
                else
                    notifyOnFailed(ErrorFlag.EXTRACTOR_UNKNOWN,"链接抽取方式错误");
            }
            else if(null != $("//div[@class='videoplay']/iframe/@src",page.getHtml()))
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING,"节点被加密，无法播放");
            else
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING,"播放器参数获取失败");
        }
        else
            notifyOnFailed(ErrorFlag.RULE_MISSING,"播放器信息规则缺失");
    }

    @Override
    protected void postPlayUrl(@NonNull Page page, @NonNull String videoUrl)
    {
        //该源需要AES解密
        //解析到解密所需的key和iv
        String[] vars = videoUrl.split(";");
        if (whenNullNotifyFail(vars, ErrorFlag.EXCEPTION_WHEN_PARSING, "抽取解密信息失败!"))
        {
            if (vars.length >= 5)
            {
                try
                {
                    //被加密得到js信息，里面包含视频链接
                    String js = vars[2];
                    js = js.substring(js.indexOf("\"") + 1, js.lastIndexOf("\""));
                    //解密的key
                    String key = vars[3];
                    key = key.substring(key.indexOf("\"") + 1, key.lastIndexOf("\""));
                    //解密的iv
                    String iv = vars[4];
                    iv = iv.substring(iv.indexOf("(") + 1, iv.lastIndexOf(")"));
                    if (whenNullNotifyFail(js, ErrorFlag.EXCEPTION_WHEN_PARSING, "待解密信息为空") &&
                            whenNullNotifyFail(key, ErrorFlag.EXCEPTION_WHEN_PARSING, "解密key为空") &&
                            whenNullNotifyFail(iv, ErrorFlag.EXCEPTION_WHEN_PARSING, "解密iv为空"))
                    {
                        System.out.println("解析到key：" + key);
                        System.out.println("解析到iv：" + iv);
                        //解密出原始js信息
                        String rawJs = BaseLeLeVideoExtProcessor.decryptLeLeVideoUrl(js, key, iv);
                        System.out.println("解密后js：" + rawJs);
                        if (whenNullNotifyFail(rawJs, ErrorFlag.EXCEPTION_WHEN_PARSING, "视频信息解密失败"))
                        {
                            //截取出视频链接
                            String realVideoUrl = rawJs.substring(rawJs.indexOf("video: {url: \"") + 14, rawJs.indexOf("\",type"));
                            System.out.println("抽取链接：" + realVideoUrl);
                            if (whenNullNotifyFail(realVideoUrl, ErrorFlag.EMPTY_VIDEO_URL, "抽取视频链接失败"))
                            {
                                super.postPlayUrl(page, realVideoUrl);
                            }
                        }
                    }
                } catch (Exception e)
                {
                    e.printStackTrace();
                    notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "尝试抽取解密信息出错");
                }
            } else
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "解密信息长度异常");
        }
    }
}
