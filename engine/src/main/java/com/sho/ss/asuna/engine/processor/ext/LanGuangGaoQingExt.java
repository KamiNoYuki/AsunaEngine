package com.sho.ss.asuna.engine.processor.ext;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSONObject;
import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.processor.common.CommonLeLeExtVideoProcessor;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.interfaces.ParseListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/5/21 1:52:16
 * @description 蓝光高情-https://v.lvdi.vip
 * @deprecated 该源加密过于变态，懒得解密了，因此废弃
 **/
@Deprecated
public class LanGuangGaoQingExt extends CommonLeLeExtVideoProcessor
{
    public LanGuangGaoQingExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
    }

    @NonNull
    @Override
    protected String getApiConfigJsPath()
    {
        //参数t为时间:20220521
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
        String time = format.format(new Date());
        return videoSource.getHost() + "/static/js/playerconfig.js?t=" + time;
    }

    @Override
    protected String getAesKeyJsPath(@NonNull String contextUrl)
    {
        return contextUrl.substring(0, contextUrl.indexOf("/?url=")) + "/js/play.js";
    }

    @Override
    protected void parsePlayerApi(Page page, Html html)
    {
        System.out.println("解析api信息");
        String playUrl = videoSource.getPlayUrl();
        if(whenNullNotifyFail(playUrl,ErrorFlag.RULE_MISSING,"播放链接规则缺失!"))
        {
            String playerInfoJs = $(playUrl, html);
            System.out.println("playerInfoJs： " + playerInfoJs);
            if(null != playerInfoJs && whenNullNotifyFail(playerInfoJs,ErrorFlag.EXCEPTION_WHEN_PARSING,"解析播放信息失败"))
            {
                JSONObject playerInfo = extractPlayerInfoJs(playerInfoJs);
                System.out.println("playerInfo： " + playerInfo);
                if(null != playerInfo && whenNullNotifyFail(playerInfo,ErrorFlag.EXCEPTION_WHEN_PARSING,"解析播放js失败"))
                {
                    //链接加密类型
                    // 0: 未加密
                    // 1: gbk2312编码 URLDecoder.decode可转换为原始链接
                    // 2: aes加密，调用解密方法可解
                    int encrypt = playerInfo.getIntValue("encrypt");
                    //视频链接，并非播放直链
                    String url = encrypt == 2 ? decodeUrlWithJtType(playerInfo.getString("url")) : playerInfo.getString("url");
                    //接口类型 根据接口类型获取对应请求接口
                    String from = playerInfo.getString("from");
                    System.out.println("from: " + from);
                    System.out.println("该源采用固定接口，因此废弃多接口解析策略");
                    if(whenNullNotifyFail(from,ErrorFlag.EXCEPTION_WHEN_PARSING,"解析接口信息失败") &&
                            whenNullNotifyFail(url,ErrorFlag.EXCEPTION_WHEN_PARSING,"解析链接失败"))
                    {
                        //该源采用固定接口，因此废弃多接口策略
                        String videoApi = videoSource.getVideoApi();
                        //获取接口配置文件
                        getApiJson(getApiConfigJsPath(), new InnerParseListener<JSONObject>()
                        {
                            @Override
                            public void done(@NonNull JSONObject jsonObject)
                            {
                                //获取对应apiJson对象
                                JSONObject apiJson = jsonObject.getJSONObject(from);
                                //api接口前置
                                String api = apiJson.getString("parse");
                                //该源弃用了接口配置文件，采用了固定接口
                                //但为了防止后续恢复多接口使用，因此基于多接口逻辑进行判空，为空时采用固定api接口
                                if(isNullStr(api))
                                    api = videoSource.getVideoApi();
                                System.out.println("接口：" + api);
                                if(whenNullNotifyFail(api, ErrorFlag.EXCEPTION_WHEN_PARSING,"前置接口解析失败"))
                                {
                                    //拼合api全链接
                                    String playerUrl = api + url;
                                    //添加请求
                                    page.addTargetRequest(playerUrl);
                                    System.out.println("playerUrl: " + playerUrl);
                                }
                            }

                            @Override
                            public void fail(String msg)
                            {
                                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING,"解析播放接口配置文件失败!");
                            }
                        });
                    }
                }
            }
        }
    }
}
