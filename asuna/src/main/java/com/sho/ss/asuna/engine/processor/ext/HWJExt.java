package com.sho.ss.asuna.engine.processor.ext;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.core.utils.HttpConstant;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.processor.base.BaseVideoExtensionProcessor;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.utils.UserAgentLibrary;
import com.sho.ss.asuna.engine.utils.Xpath;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @project: 启源视频
 * @author: Sho Tan.
 * @E-mail: 2943343823@qq.com
 * @created: 2022/4/30 14:54:18
 * @description: HWJ影视
 * 2022/12/3 网页已恢复访问，可正常使用
 **/

public class HWJExt extends BaseVideoExtensionProcessor<VideoSource, Episode>
{

    private JSONObject urlJSON = null;

    public HWJExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
    }

    @Override
    public void process(Page page)
    {
        String url = page.getUrl().get();
        if (url.startsWith(videoSource.getHost() + videoSource.getPlayApi()))
            extensionParse(page, page.getHtml());
        else if (url.startsWith("https://hwjyingshi.xyz/static/js/playerconfig.js"))
            parseApi(page, page.getHtml());
        else
            parseVideoUrl(page, page.getHtml());
    }

    /**
     * 解析视频播放链接
     *
     * @param page page
     * @param html html
     * @deprecated 废弃
     */
    private void parseVideoUrl(Page page, Html html)
    {
        System.out.println("源[" + videoSource.getName() + "]的播放器界面：" + html.get());
        String javascript = Xpath.select("//div[@id='loading']/following-sibling::*[@type='text/javascript']/text()", html);
        if (null != javascript && !TextUtils.isEmpty(javascript))
        {
            String config = javascript.substring(javascript.indexOf("{"), javascript.lastIndexOf("}") + 1);
            if (!TextUtils.isEmpty(config))
            {
                JSONObject jsonObject = JSON.parseObject(config);
                String url = jsonObject.getString("url");
                if (!TextUtils.isEmpty(url))
                {
                    episode.setVideoUrl(url);
                    notifyOnCompleted();
                } else
                    notifyOnFailed(ErrorFlag.PARSE_URL_UNKNOWN, "播放链接解析失败!");
            } else
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "解析视频配置信息失败!");
        } else
            notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "解析播放信息失败!");
    }

    @Override
    protected void extensionParse(Page page, Html html)
    {
        super.extensionParse(page, html);
        String playUrl = videoSource.getPlayUrl();
        if (!TextUtils.isEmpty(playUrl))
        {
            String script = Xpath.select(playUrl, html);
            if (!TextUtils.isEmpty(script))
            {
                String json = script.substring(script.indexOf("{"), script.lastIndexOf("}") + 1);
                if (!TextUtils.isEmpty(json))
                {
                    JSONObject jsonObject = JSON.parseObject(json);
                    if (null != jsonObject)
                    {
                        //接口类型，目前已知腾讯视频解析接口的播放链接被加密，无法播放
                        String apiType = jsonObject.getString("from");
                        //因此腾讯接口直接回调解析失败
                        if (!TextUtils.equals(apiType, "qq"))
                        {
                            String url = jsonObject.getString("url");
                            if (!TextUtils.isEmpty(url))
                            {
                                //加密类型：1、未加密    2、加密（需要解密）
                                int encrypt = jsonObject.getIntValue("encrypt");
                                //解密url
                                if (encrypt == 2)
                                    url = decodeUrlWithJtType(url);
                                //解密后是GBK2312编码，将其解码为UTF-8
                                try
                                {
                                    url = URLDecoder.decode(url, StandardCharsets.UTF_8.name());
                                } catch (UnsupportedEncodingException ignored)
                                {
                                }
                                episode.setVideoUrl(url);
                                notifyOnCompleted();
                            } else
                                notifyOnFailed(ErrorFlag.PARSE_URL_UNKNOWN, "解析视频失败!");
                        } else
                            notifyOnFailed(ErrorFlag.PARSE_URL_UNKNOWN, "接口被加密，无法解析");
                    } else
                        notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "解析失败!");
                } else
                    notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "播放解析失败!");
            } else
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "播放解析失败!");
        } else
            notifyOnFailed(ErrorFlag.RULE_MISSING, "解析规则缺失!");
    }

    /**
     * @param page page
     * @deprecated 其他接口未做针对性处理，因此不可用
     */
    protected void requestApi(Page page)
    {
        String script = "https://hwjyingshi.xyz/static/js/playerconfig.js?t=" + getTime();
        Request request = new Request(script);
        request.addHeader(HttpConstant.Header.USER_AGENT, new UserAgentLibrary().getProxyUserAgent());
        page.addTargetRequest(script);
    }

    /**
     * @param page page
     * @param html html
     * @deprecated 其他接口未做针对性处理，因此不可用
     */
    protected void parseApi(Page page, Html html)
    {
        if (null != urlJSON)
        {
            String vars = Xpath.select("//body/text()", html);
            if (null != vars)
            {
                vars = vars.replace(" ", "")
                        .replace("\n", "");
                String allApi = vars.substring(vars.indexOf("MacPlayerConfig.player_list=") + 28, vars.indexOf(",MacPlayerConfig"));
                //所有接口信息的JSON
                JSONObject apiJSON = JSON.parseObject(allApi);
                if (null != apiJSON)
                {
                    //接口类型，目前已知腾讯视频解析接口的播放链接被加密，无法播放
                    String apiType = urlJSON.getString("from");
                    //因此腾讯接口直接回调解析失败
                    if (!TextUtils.equals(apiType, "qq"))
                    {
                        //根据urlJSON中的接口类型，获取apiJSON中对应接口信息
                        JSONObject api = apiJSON.getJSONObject(apiType);
                        //获取接口请求链接前置
                        String requestApi = api.getString("parse");
                        //urlJSON中的url
                        String url = urlJSON.getString("url");
                        //加密类型：1、未加密    2、加密（需要解密）
                        int encrypt = urlJSON.getIntValue("encrypt");
                        //解密url
                        if (encrypt == 2)
                            url = decodeUrlWithJtType(url);
                        //解密后是GBK2312编码，将其解码为UTF-8
                        try
                        {
                            url = URLDecoder.decode(url, StandardCharsets.UTF_8.name());
                        } catch (UnsupportedEncodingException ignored)
                        {
                        }
                        //补全接口和url
                        String playerUrl = requestApi + url;
                        //构建请求
                        Request request = new Request(playerUrl)
                                .addHeader(HttpConstant.Header.USER_AGENT, new UserAgentLibrary().getProxyUserAgent())
                                .addHeader(HttpConstant.Header.REFERER, page.getUrl().get());
                        //添加请求
                        page.addTargetRequest(request);
                    } else
                        notifyOnFailed(ErrorFlag.PARSE_URL_UNKNOWN, "接口被加密，无法解析");
                } else
                    notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "接口配置获取失败!");
            } else
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "解析接口失败!");
        } else
            notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "JSON_OBJECT为空!");
    }

    protected String getTime()
    {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
        Date date = new Date(System.currentTimeMillis());
        return dateFormat.format(date);
    }
}
