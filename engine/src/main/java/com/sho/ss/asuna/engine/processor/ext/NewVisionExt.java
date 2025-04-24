package com.sho.ss.asuna.engine.processor.ext;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.alibaba.fastjson2.JSON;
import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.SpiderListener;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.common.BaseThirdLevelPageProcessor;
import com.sho.ss.asuna.engine.processor.ext.VideoCopyrightChecker;
import com.sho.ss.asuna.engine.utils.AESUtils;
import com.sho.ss.asuna.engine.utils.JsonPathUtils;
import com.sho.ss.asuna.engine.utils.MapUtils;
import com.sho.ss.asuna.engine.utils.SpiderUtils;
import com.sho.ss.asuna.engine.utils.UserAgentLibrary;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2023/3/14 12:31:34
 * @description 新视觉影院-https://www.6080dy3.com
 **/
public class NewVisionExt extends BaseThirdLevelPageProcessor
{
    //存放视频参数的key
    final String keyParams = "params";
//    @NonNull
//    protected final Set<String> encryptedNodes = new LinkedHashSet<>(Arrays.asList("qq", "youku", "qiyi", "sohu", "xigua", "bilibili","lgzx1","lg2"));// xigua没在源站遇到，不知道啥情况。就一样解密碰碰运气。

    public NewVisionExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
        addParseTarget(1, this::doParseConfigInfo);
    }

    @Override
    protected IParser getWatchPageTargetInstance()
    {
        return (page, html, url) ->
        {
            //该源有些视频因版权限制无法播放，因此使用该版权视频检测器去解析。如果因无版权无法播放可感知到，由此提示给用户。
            VideoCopyrightChecker checker = new VideoCopyrightChecker(entity, Objects.requireNonNull(entity.getVideoSource()), episode, new ParseListener<>()
            {
                @Override
                public void onStarted()
                {
                }

                @Override
                public void onCompleted(@NonNull Episode episode)
                {
                    final String url = episode.getVideoUrl();
                    if(whenNullNotifyFail(url, ErrorFlag.EMPTY_VIDEO_URL, "视频解析失败") && null != url) {
                        checkVideoUrl(page, url);
                    }
                }

                @Override
                public void onFail(int flag, String errMsg)
                {
                    notifyOnFailed(flag, errMsg);
                }
            });
            checker.setUseDefaultFilter(isUseDefaultFilter());
            checker.process(page);
        };
    }

    /**
     * (第一个界面)观看页面数据解析，重写该方法，处理数据后请求接口配置文件
     *
     * @param page page
     * @param js   存放视频信息的js
     */
    @Override
    protected void onWatchPageVideoLinkParse(@NonNull Page page, @NonNull String js)
    {
        if (whenNullNotifyFail(js, ErrorFlag.NO_PARSED_DATA, "未解析到视频数据"))
        {
            //需要解析的视频链接
            String urlParam = JsonPathUtils.selAsString(js, "$.url");
            //解析接口名
            String apiName = JsonPathUtils.selAsString(js, "$.from");
            if (whenNullNotifyFail(urlParam, ErrorFlag.NO_PARSED_DATA, "未解析到url参数") &&
                    whenNullNotifyFail(apiName, ErrorFlag.NO_PARSED_DATA, "未解析到from参数") && null != urlParam)
            {
                boolean isVideoUrl = checkVideoUrl(page,urlParam,false);
                if(!isVideoUrl) {
                    final Request request = new Request(buildPlayerConfigApi(page));
                    SpiderUtils.initRequest(request,
                            new UserAgentLibrary().USER_AGENT_EDGE,
                            page.getUrl().get(), null, null);
                    request.putExtra(keyParams, new Pair<>(urlParam, apiName));
                    request(request, 2, new SpiderListener()
                    {
                        @Override
                        public void onError(Request request, Exception e)
                        {
                            e.printStackTrace();
                            notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, null == e.getMessage() ? "请求接口信息失败" : e.getMessage());
                        }
                    });
                }
            }
        }
    }

    /**
     * 负责解析第二个页面（即接口配置文件）的数据
     *
     * @param page page
     * @param html html
     * @param url  当前页面url
     */
    protected void doParseConfigInfo(@NonNull Page page, @NonNull Html html, @NonNull String url)
    {
        if (whenNullNotifyFail(page.getRawText(), ErrorFlag.NO_PARSED_DATA, "解析到空的接口信息!"))
        {
            String trimUrl = page.getRawText().replace(" ", "");
            String json = trimUrl.substring(trimUrl.indexOf("player_list=") + 12, trimUrl.indexOf(",MacPlayerConfig"));
            Pair<String, String> videoInfo = page.getRequest().getExtra(keyParams);
            //获取接口文件中对应的api解析接口
            String baseApi = JsonPathUtils.selAsString(json, String.format("$.%s.parse", videoInfo.second));
            if (whenNullNotifyFail(json, ErrorFlag.NO_PARSED_DATA, "未解析到接口配置文件") &&
                    whenNullNotifyFail(videoInfo, ErrorFlag.DATA_INVALIDATE, "未接收到视频参数") &&
                    whenNullNotifyFail(baseApi, ErrorFlag.API_MISSING, "没有获取到对应接口信息") &&
                    whenNullNotifyFail(videoInfo.first, ErrorFlag.EMPTY_VIDEO_URL, "视频url参数丢失"))
            {
                baseApi += videoInfo.first;
                System.out.println("接口名：" + videoInfo.second + " api：" + baseApi);
                Request request = new Request(baseApi);
                SpiderUtils.initRequest(request, null, null, videoSource.getVideoApiCk(), videoSource.getVideoApiHd());
                SpiderUtils.addUserAgent(request, videoSource.getVideoApiUa());
                SpiderUtils.addRequestParamsForKeyword(baseApi, true, request, videoSource.getVideoApiPm());
                SpiderUtils.addReferer(videoSource, request, videoSource.getVideoApiReferer(), true);
                request.putExtra(keyParams, videoInfo);
                page.addTargetRequest(request);
            }
        }
    }

    /**
     * 解析第三个页面（即播放器页面）的视频链接
     *
     * @param page page
     * @param js   播放器页面存放视频链接的js
     */
    @Override
    protected void handleVideoUrl(@NonNull Page page, @NonNull String js)
    {
//        System.out.println("js：" + js);
        Pair<String, String> videoInfo = page.getRequest().getExtra(keyParams);
        if (whenNullNotifyFail(videoInfo, ErrorFlag.DATA_INVALIDATE, "未接收到视频参数"))
        {
            //接口名称
//            String apiName = videoInfo.second;
            String config = js.substring(0, js.lastIndexOf("}") + 1);
            System.out.println("视频信息：" + config);
            if (whenNullNotifyFail(config, ErrorFlag.DATA_INVALIDATE, "未获取到视频数据"))
            {
                config = config.replace(",}", "}");
                config = applyFilter(config, videoSource.getPlayerJsFilter());
                System.out.println("视频信息标准化后：" + config);
                //视频链接
                String url = JsonPathUtils.selAsString(config, "$.url");
                //该值为解密所需的key的中间部分
                String uid = JsonPathUtils.selAsString(config, "$.config.uid");
                final HashMap<String, String> extras = videoSource.getExtras();
                if(whenNullNotifyFail(extras, ErrorFlag.EXCEPTION_WHEN_PARSING, "源extras数据丢失，请检查") && null != extras) {
                    String key = extras.get("key");
                    String iv = extras.get("iv");
                    if (whenNullNotifyFail(url, ErrorFlag.DATA_INVALIDATE, "参数url未找到") &&
                            whenNullNotifyFail(uid, ErrorFlag.EXCEPTION_WHEN_PARSING,"参数uid未找到") &&
                            whenNullNotifyFail(key, ErrorFlag.EXTRAS_MISSING, "extras@key缺失") &&
                            whenNullNotifyFail(iv, ErrorFlag.EXTRAS_MISSING, "extras@iv缺失") &&
                            null != url && null != uid && null != key && null != iv) {
                        key = key.replace("{uid}", uid);
                        String videoUrl = AESUtils.decrypt(url, key, iv);
                        if(whenNullNotifyFail(videoUrl, ErrorFlag.DECRYPT_ERROR, "视频解密失败") && null != videoUrl) {
                            notifyOnCompleted(toAbsoluteUrl(videoUrl));
                        }
                    }
                }
            }
        }
    }

    /**
     * 此处是第四个页面（即解析视频播放链接）
     *
     * @param page page
     * @param html html
     * @param curPageUrl  url
     */
    @Override
    protected void doParseThirdPage(@NonNull Page page, @NonNull Html html, @NonNull String curPageUrl)
    {
        String json = page.getRawText();
        System.out.println("解析响应结果：" + json);
        if (JSON.isValid(json))
        {
            int success = JsonPathUtils.selAsInt(json, "$.success");
            String msg = JsonPathUtils.selAsString(json, "$.msg");
            msg = TextUtils.isEmpty(msg) ? "视频链接解析失败!" : msg;
            //1表示解析成功
            if (success == 1)
            {
                //必须在解析成功时才获取url，解析失败时url是不存在的
                String videoUrl = JsonPathUtils.selAsString(json, "$.url");
                if (whenNullNotifyFail(videoUrl, ErrorFlag.DATA_INVALIDATE, msg) && null != videoUrl)
                {
                    notifyOnCompleted(videoUrl);
                }
            } else
            {
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, msg);
            }
        } else
            notifyOnFailed(ErrorFlag.DATA_INVALIDATE, "响应信息格式异常!");
    }

    /**
     * 构造解析视频播放链接的接口地址
     *
     * @param url url
     * @return 解析接口地址
     */
//    @NonNull
//    protected String buildVideoParseApi(@NonNull String url)
//    {
//        final String api = "API.php";
//        try
//        {
//            //在当前页面路径后面拼接即可
//            return new URL(url).getPath() + api;
//        } catch (MalformedURLException e)
//        {
//            e.printStackTrace();
//            //尝试把末尾的?url=去除然后再拼接
//            return url.replace("?url=", "") + api;
//        }
//    }

    /**
     * 构造接口配置文件的url
     *
     * @param page page
     * @return url
     */
    @NonNull
    protected String buildPlayerConfigApi(@NonNull Page page)
    {
        String host = getHostByUrl(page.getUrl().get());
        String relativeUrl = "/static/js/playerconfig.js?t=" +
                new SimpleDateFormat("yyyyMMdd", Locale.CHINA)
                        .format(new Date());
        if (!TextUtils.isEmpty(host))
            return toAbsoluteUrl(host, relativeUrl);
        else
            return toAbsoluteUrl(relativeUrl);
    }

    /**
     * 解密所需的id和text进行排序，按照数字id升序排列
     * @param ids id
     * @param texts text
     * @return 排序后组合的字符串
     */
    @NonNull
    public static String sortById(@NonNull String ids,@NonNull String texts) {
        TreeMap<Integer,Character> map = new TreeMap<>();
        for(int i = 0;i < ids.length() && i < texts.length();i++) {
            //必须先转为string，否则char转int转的是char的ascii码
            String id = String.valueOf(ids.charAt(i));
            map.put(Integer.valueOf(id),texts.charAt(i));
        }
        StringBuilder builder = new StringBuilder();
        //将排序好的text组合成新的字符串
        MapUtils.proxy(map, (id, text) ->
                builder.append(text));
        return builder.toString();
    }

}
