package com.sho.ss.asuna.engine.processor.ext;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.model.HttpRequestBody;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.core.utils.HttpConstant;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.processor.common.CommonMultiApiExtVideoProcessor;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.utils.SpiderUtils;
import com.sho.ss.asuna.engine.utils.UserAgentLibrary;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/6/5 22:56:03
 * @description 京广航影视-https://www.jgh123.com
 **/
public class JingGuangHangExt extends CommonMultiApiExtVideoProcessor
{
    /**
     * 视频解析api
     */
    private final String video_parse_api = "/api.php";

    public JingGuangHangExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
    }

    @Override
    public void process(Page page)
    {
        String url = page.getUrl().get();
        if(whenNullNotifyFail(videoSource.getHost(), ErrorFlag.HOST_INVALIDATE,"主域名缺失!")
                && whenNullNotifyFail(videoSource.getVideoApi(),ErrorFlag.API_MISSING,"API缺失!"))
        {
            if(url.startsWith(videoSource.getHost() + videoSource.getPlayApi()))
                parsePlayPage(page,page.getHtml());
            else if(url.endsWith(video_parse_api))
                parseVideoJson(page.getHtml());
            else
                parsePlayerPage(page,page.getHtml());
        }
    }

    private void parseVideoJson(Html html)
    {
        String responseJson = $("//body/text()", html);
        System.out.println("响应信息：" + responseJson);
        if(whenNullNotifyFail(responseJson,ErrorFlag.EXCEPTION_WHEN_PARSING,"视频链接解析失败!") && null != responseJson)
        {
            try
            {
                JSONObject json = JSON.parseObject(responseJson);
                if(whenNullNotifyFail(json,ErrorFlag.EXCEPTION_WHEN_PARSING,"无响应信息"))
                {
                    //视频链接
                    String url = json.getString("url");
                    if(whenNullNotifyFail(url,ErrorFlag.EMPTY_VIDEO_URL,json.getString("msg")))
                    {
                        notifyOnCompleted(url);
                    }
                }
            }
            catch (JSONException e)
            {
                e.printStackTrace();
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING,"序列化响应信息失败!");
            }
        }
    }

    private void parsePlayerPage(Page page,Html html)
    {
        String js = $("//div[@id='error']/preceding-sibling::script[last()]/text()", html);
        if(whenNullNotifyFail(js,ErrorFlag.EXCEPTION_WHEN_PARSING,"解析视频信息失败!") && null != js)
        {
            //播放链接，如果该链接为空，需要向/api.php携带url 请求播放链接
            String vip = js.split(";")[3];
            if(!TextUtils.isEmpty(vip))
                vip = vip.substring(vip.indexOf("'") + 1,vip.lastIndexOf("'"));
            System.out.println("视频链接：" + vip);
            //不是变量为null，而是该变量的值为null
            if(TextUtils.isEmpty(vip)||vip.contains("null"))
            {
                String url = js.split(";")[2];
                if(whenNullNotifyFail(url,ErrorFlag.EXCEPTION_WHEN_PARSING,"url参数无效"))
                {
                    String apiParam = extractUrlWithSubstring(url, "'", "'");
                    System.out.println("url参数：" + apiParam);
                    if(whenNullNotifyFail(apiParam,ErrorFlag.EXCEPTION_WHEN_PARSING,"url参数无效"))
                    {
                        String api = getHostByUrl(page.getUrl().get()) + video_parse_api;
                        Request request = new Request(api);
                        Map<String,Object> params = new HashMap<>();
                        params.put("url",apiParam);
                        request.addHeader(HttpConstant.Header.USER_AGENT,new UserAgentLibrary().getProxyUserAgent())
                                .setMethod(HttpConstant.Method.POST)
                                .setRequestBody(HttpRequestBody.form(params, StandardCharsets.UTF_8.name()));
                        SpiderUtils.addReferer(videoSource,request,videoSource.getReferer(),true);
                        page.addTargetRequest(request);
                    }
                }
            }
            //解析到播放链接
            else
            {
                //转绝对链接
                vip = SpiderUtils.fixHostIfMissing(vip,getHostByUrl(page.getUrl().get()));
                notifyOnCompleted(vip);
            }
        }
    }

    private void parsePlayPage(Page page, Html html)
    {
        if(whenNullNotifyFail(videoSource.getPlayUrl(),ErrorFlag.RULE_MISSING,"播放链接规则缺失!"))
        {
            System.out.println("播放信息规则：" + videoSource.getPlayUrl());
            String info = $(videoSource.getPlayUrl(), html);
            System.out.println("info：" + info);
            if(whenNullNotifyFail(info,ErrorFlag.EXCEPTION_WHEN_PARSING,"解析播放信息失败!") && null != info)
            {
                JSONObject json = extractPlayerInfoJs(info);
                System.out.println("播放信息的js：" + json);
                if(whenNullNotifyFail(json,ErrorFlag.EXCEPTION_WHEN_PARSING,"解析播放信息异常!") && null != json)
                {
                    String from = json.getString("from");
                    String url = json.getString("url");
                    if(whenNullNotifyFail(url,ErrorFlag.EXCEPTION_WHEN_PARSING,"视频链接无效!"))
                    {
                        //大部分情况直接就是可播放的直链
                        if(url.endsWith(".m3u8"))
                            notifyOnCompleted(url);
                        else
                        {
                            if(whenNullNotifyFail(from,ErrorFlag.EXCEPTION_WHEN_PARSING,"接口信息无效!"))
                            {
                                SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
                                String t = format.format(new Date());
                                //存放api信息的js文件链接
                                String api_config_url = "/static/js/playerconfig.js?t=";
                                //解析api配置文件
                                getApiJson(videoSource.getHost() + api_config_url + t, new InnerParseListener<JSONObject>()
                                {
                                    @Override
                                    public void done(@NonNull JSONObject jsonObject)
                                    {
                                        JSONObject apiJson = jsonObject.getJSONObject(from);
                                        if(whenNullNotifyFail(apiJson,ErrorFlag.EXCEPTION_WHEN_PARSING,"接口信息异常!") && null != apiJson)
                                        {
                                            String player_url = apiJson.getString("parse");
                                            if(whenNullNotifyFail(player_url,ErrorFlag.EXCEPTION_WHEN_PARSING,"解析播放器链接异常!"))
                                            {
                                                player_url = SpiderUtils.fixHostIfMissing(player_url,"https:") + url;
                                                Request request = new Request(player_url);
                                                SpiderUtils.addUserAgent(request,SpiderUtils.checkUserAgent(videoSource.getPlayUrlUA(),videoSource));
                                                SpiderUtils.addReferer(videoSource,request,videoSource.getReferer(),true);
                                                request.setMethod(HttpConstant.Method.GET);
                                                page.addTargetRequest(request);
                                            }
                                        }
                                    }

                                    @Override
                                    public void fail(String msg)
                                    {
                                        notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING,"解析接口文件失败: " + msg);
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }
    }

    @Nullable
    @Override
    protected JSONObject extractPlayerInfoJs(@NonNull String js)
    {
        String prefix = "player_xxxaaa222=";
        if(js.contains(prefix))
        {
            String jsString = js.substring(js.indexOf(prefix) + prefix.length(), js.lastIndexOf("; var v"));
            if(!isNullStr(jsString))
                return JSON.parseObject(jsString);
        }
        return null;
    }
}
