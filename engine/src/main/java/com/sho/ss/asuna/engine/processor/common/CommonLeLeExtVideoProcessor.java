package com.sho.ss.asuna.engine.processor.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSONObject;
import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.base.BaseLeLeVideoExtProcessor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/5/20 13:28:25
 * @description
 **/
public abstract class CommonLeLeExtVideoProcessor extends BaseLeLeVideoExtProcessor
{
    public CommonLeLeExtVideoProcessor(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
    }
    @Override
    public void process(Page page)
    {
        String host = videoSource.getHost();
        String playApi = videoSource.getPlayApi();
        String url = page.getUrl().get();
        if(whenNullNotifyFail(host, ErrorFlag.HOST_INVALIDATE,"Host无效") && whenNullNotifyFail(playApi,ErrorFlag.API_MISSING,"播放Api缺失"))
        {
            if(url.startsWith(host + playApi))
                parsePlayerApi(page,page.getHtml());
            else
                parsePlayer(page,page.getHtml());
        }
    }

    protected void parsePlayer(Page page,Html html)
    {
        //解析存放播放链接的script
        String videoInfoJs = $(getVideoInfoJsXpath(), html);
        System.out.println("解析播放器");
        if(whenNullNotifyFail(videoInfoJs,ErrorFlag.EXCEPTION_WHEN_PARSING,"解析视频信息失败") && null != videoInfoJs)
        {
            System.out.println("抽取加密链接");
            //被加密的url，可能包含getVideoInfo，因此不采用jsonObject
            String cipherUrl = extractVideoUrl(videoInfoJs);
            System.out.println("加密链接：" + cipherUrl);
            if(whenNullNotifyFail(cipherUrl,ErrorFlag.EXCEPTION_WHEN_PARSING,"解析播放链接失败"))
            {
                //获取解密所需的iv
                String iv = extractIv(html);
                System.out.println("iv: " + iv);
                if(whenNullNotifyFail(iv,ErrorFlag.EXCEPTION_WHEN_PARSING,"视频链接iv解析失败") && null != iv )
                {
                    System.out.println("正在解析解密Key");
                    //解密所需的key
                    parseKey(getAesKeyJsPath(page.getUrl().get()), new InnerParseListener<String>()
                    {
                        @Override
                        public void done(@NonNull String key)
                        {
                            System.out.println("key: " + key);
                            String videoUrl = decryptLeLeVideoUrl(cipherUrl, key, iv);
                            System.out.println("videoUrl: " + videoUrl);
                            if(whenNullNotifyFail(videoUrl,ErrorFlag.EMPTY_VIDEO_URL,"解析视频链接失败"))
                            {
                                System.out.println("解析完毕，播放链接：" + videoUrl);
                                episode.setVideoUrl(videoUrl);
                                notifyOnCompleted();
                            }
                        }

                        @Override
                        public void fail(String msg)
                        {
                            notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING,"Key解析失败: " + msg);
                            System.out.println("解密Key解析失败: " + msg);
                        }
                    });
                }
            }
        }
    }

    /**
     * 解析播放界面的解析接口信息
     * @param page page
     * @param html html
     */
    protected void parsePlayerApi(Page page, Html html)
    {
        System.out.println("解析api信息");
        String playUrl = videoSource.getPlayUrl();
        if(whenNullNotifyFail(playUrl,ErrorFlag.RULE_MISSING,"播放链接规则缺失!"))
        {
            String playerInfoJs = $(playUrl, html);
            System.out.println("playerInfoJs： " + playerInfoJs);
            if(whenNullNotifyFail(playerInfoJs,ErrorFlag.EXCEPTION_WHEN_PARSING,"解析播放信息失败") && null != playerInfoJs)
            {
                JSONObject playerInfo = extractPlayerInfoJs(playerInfoJs);
                System.out.println("playerInfo： " + playerInfo);
                if(whenNullNotifyFail(playerInfo,ErrorFlag.EXCEPTION_WHEN_PARSING,"解析播放js失败") && null != playerInfo)
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
                    System.out.println("接口from: " + from);
                    if(whenNullNotifyFail(from,ErrorFlag.EXCEPTION_WHEN_PARSING,"解析接口信息失败") &&
                            whenNullNotifyFail(url,ErrorFlag.EXCEPTION_WHEN_PARSING,"解析链接失败"))
                    {
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

    @Nullable
    @Override
    protected String extractKey(@NonNull String js)
    {
        Pattern pattern = Pattern.compile("_token_key\\s=\\sCryptoJS.enc.Utf8.parse\\(\"\\w+\"\\)");
        Matcher matcher = pattern.matcher(js);
        if(matcher.find())
        {
            String group = matcher.group();
            if(!isNullStr(group))
            {
                String key = group.substring(group.indexOf("\"") + 1, group.lastIndexOf("\""));
                if(!isNullStr(key))
                    return key;
            }
        }
        return null;
    }

    @Nullable
    @Override
    protected String extractIv(@NonNull Html html)
    {
        //存放iv字段的script
        String ivScript = $("//script[contains(text(),'le_token')]/text()",html);
        if (null != ivScript && !isNullStr(ivScript))
            return ivScript.substring(ivScript.indexOf("\"") + 1, ivScript.lastIndexOf("\""));
        else
            return null;
    }


    /**
     * 存放接口配置信息的js链接
     * @return js path
     */
    @NonNull
    protected String getApiConfigJsPath()
    {
        return videoSource.getHost() + "/static/js/playerconfig.js";
    }

    /**
     * 解析播放器界面中存放视频信息的javascript的规则
     * @return xpath 1.0 rule
     */
    @NonNull
    protected String getVideoInfoJsXpath()
    {
        return "//div[@id='ADtip']/following-sibling::script/text()";
    }

    @Override
    @Nullable
    protected String extractVideoUrl(String js)
    {
        String fakeUrl = js.substring(js.indexOf("\"url\":") + 6, js.indexOf("\"id\""));
        return fakeUrl.substring(fakeUrl.indexOf("\"") + 1,fakeUrl.lastIndexOf("\""));
    }

    /**
     * 解密视频链接所需的Aes key所在的js文件链接
     * @param contextUrl 上下文链接，即播放器界面链接
     * @return js url
     */
    protected String getAesKeyJsPath(@NonNull String contextUrl)
    {
        return videoSource.getHost() + "/js/play.js";
    }

}
