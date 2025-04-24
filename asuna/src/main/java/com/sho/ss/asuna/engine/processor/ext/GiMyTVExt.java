package com.sho.ss.asuna.engine.processor.ext;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.Spider;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.core.utils.HttpConstant;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.base.BaseLeLeVideoExtProcessor;
import com.sho.ss.asuna.engine.processor.common.BaseThirdLevelPageProcessor;
import com.sho.ss.asuna.engine.utils.SpiderUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/10/6 16:36:36
 * @description 剧迷TV-https://gimytv.tv/
 **/
public class GiMyTVExt extends BaseThirdLevelPageProcessor {
    public GiMyTVExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener) {
        super(entity, videoSource, episode, listener);
    }

    /**
     * 负责解析存放Key的js内容
     *
     * @param page       page
     * @param html       html
     * @param curPageUrl url
     */
    @Override
    protected void doParseThirdPage(@NonNull Page page, @NonNull Html html, @NonNull String curPageUrl) {
        final String iv = page.getRequest().getExtra("iv");
        final String videoUrl = page.getRequest().getExtra("videoUrl");
        if (whenNullNotifyFail(iv, ErrorFlag.EXCEPTION_WHEN_PARSING, "未接收到iv") &&
                whenNullNotifyFail(videoUrl, ErrorFlag.EXCEPTION_WHEN_PARSING, "未接收到videoUrl")) {
            String content = page.getRawText();
            if (whenNullNotifyFail(content, ErrorFlag.EXCEPTION_WHEN_PARSING, "解密Key获取失败") && null != content) {
                String key = extractKey(content);
                System.out.println("抽取的key：" + key);
                if (whenNullNotifyFail(key, ErrorFlag.EXCEPTION_WHEN_PARSING, "解密Key抽取失败!")) {
                    try {
                        String realVideoUrl = BaseLeLeVideoExtProcessor.decryptLeLeVideoUrl(videoUrl, key, iv);
                        System.out.println("解密后的视频链接：" + realVideoUrl);
                        notifyOnCompleted(realVideoUrl);
                    } catch (Exception e) {
                        e.printStackTrace();
                        notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "播放链接解密失败!");
                    }
                }
            }
        }
    }

    /**
     * 抽取js中的解密key
     *
     * @param js js
     * @return key
     */
    protected String extractKey(@NonNull String js) {
        Pattern pattern = Pattern.compile("_token_key\\s=\\sCryptoJS.enc.Utf8.parse\\(\"\\w+\"\\)");
        Matcher matcher = pattern.matcher(js);
        if (matcher.find()) {
            String group = matcher.group();
            if (!isNullStr(group)) {
                String key = group.substring(group.indexOf("\"") + 1, group.lastIndexOf("\""));
                if (!isNullStr(key))
                    return key;
            }
        }
        return null;
    }

    @Override
    protected void handleVideoUrl(@NonNull Page page, @NonNull String videoUrl) {
        //解析当前界面的iv_token
        String iv = $("//script[contains(text(),'le_token')]/text()", page.getHtml());
        if (whenNullNotifyFail(iv, ErrorFlag.EXCEPTION_WHEN_PARSING, "iv获取失败!")) {
            iv = extractUrlWithSubstring(iv, "\"", "\"");
            System.out.println("处理后的iv：" + iv);
            //该源的高清节点播放链接需要解密
            //首先获取存放解密所需的key的js文件
            Request request = new Request(getAesKeyJsPath())
                    .putExtra("videoUrl", videoUrl)
                    .putExtra("iv", iv);
            SpiderUtils.initRequest(request, getProxyUserAgent(), page.getUrl().get(), null, null);
            SpiderUtils.applyMethod(request, HttpConstant.Method.GET);
            Spider spider = SpiderUtils.buildSpider(this, request, 1);
            if (null != spider)
                spider.runAsync();
            else
                notifyOnFailed(ErrorFlag.INIT_ENGINE_EXCEPTION, "引擎初始化失败");
        }
    }

    public static void main(String[] args) {
//        String url = "QYv9GR9AGIULx1W/1pQuQDIdGlyBdIPT7KAHsYX0w1vRtpcopcQXoxRlPOS1p5kOxJkeAz/kl52hPXxZ84wl1b3esvJL1V5AVY1R12WTrkhtwMpq0pDR/2hkHzAsoFyzx+ozpypSAVp6UeEwpIshmruvrQIBwp442VXkvYkOa49wyqyz+mIS5GTnJTnxAkVQ";
//        String key = "A42EAC0C2B408472";
//        String iv = "9dc90c301456439c";
//        System.out.println(decryptLeLeVideoUrl(url,key,iv));
    }

    /**
     * 存放解密key的js链接
     *
     * @return js链接地址
     */
    @NonNull
    protected String getAesKeyJsPath() {
        return videoSource.getHost() + "/jcplayer/js/play.js";
    }
}
