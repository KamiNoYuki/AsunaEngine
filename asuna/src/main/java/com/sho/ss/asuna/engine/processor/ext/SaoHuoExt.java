package com.sho.ss.asuna.engine.processor.ext;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.SpiderListener;
import com.sho.ss.asuna.engine.core.model.HttpRequestBody;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.core.utils.HttpConstant;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.processor.base.BaseVideoExtensionProcessor;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.utils.SpiderUtils;
import com.sho.ss.asuna.engine.utils.UserAgentLibrary;
import com.sho.ss.asuna.engine.utils.Xpath;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @project: 启源视频
 * @author: Sho Tan.
 * @E-mail: 2943343823@qq.com
 * @created: 2022/4/29 15:56:00
 * @description: 骚火电影扩展爬虫处理器
 * @deprecated 使用SaoHuoExtNew
 **/
@Deprecated
public class SaoHuoExt extends BaseVideoExtensionProcessor<VideoSource, Episode> {
    private String referer = "";

    public SaoHuoExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener) {
        super(entity, videoSource, episode, listener);
    }

    @Override
    public void process(Page page) {
        String url = page.getUrl().get();
        //播放界面
        if (url.startsWith(videoSource.getHost() + videoSource.getPlayApi()))
            extensionParse(page, page.getHtml());
        else if (url.startsWith(getHostByUrl(videoSource.getVideoApi()) + "/index.php?url="))
            parseParam(page, page.getHtml());
        else if (url.startsWith(videoSource.getVideoApi()))
            requestPlayUrl(page, page.getHtml());
        else
            notifyOnFailed(ErrorFlag.UN_EXPECTED_URL, "匹配到超出预期的URL");
    }

    /**
     * 解析播放链接
     *
     * @param page page
     * @param html html
     */
    private void requestPlayUrl(Page page, Html html) {
        String responseMsg = Xpath.select("//body/text()", html);
        if (responseMsg != null && !TextUtils.isEmpty(responseMsg)) {
            responseMsg = applyFilter(responseMsg, "&.*;", "");
            JSONObject jsonObject = JSON.parseObject(responseMsg);
//            System.out.println("jsonObject: " + jsonObject.toJSONString());
            //响应信息
            String responseInfo = jsonObject.getString("msg");
            if (TextUtils.equals(responseInfo, "success")) {
                String url = jsonObject.getString("url");
                if (!TextUtils.isEmpty(url)) {
                    url = SpiderUtils.fixHostIfMissing(url, getHostByUrl(videoSource.getVideoApi()));
                    episode.setVideoUrl(url);
//                    System.out.println("[" + episode.getName() + "]播放链接：" + url);
                    //referer
                    if (null != jsonObject.getString("referer") && jsonObject.getString("referer").startsWith("http"))
                        episode.setReferer(jsonObject.getString("referer"));
                    notifyOnCompleted();
                } else
                    notifyOnFailed(ErrorFlag.PARSE_URL_UNKNOWN, "视频链接无效");
            } else
                notifyOnFailed(ErrorFlag.PARSE_URL_UNKNOWN, responseInfo);
        } else
            notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "解析播放链接失败");

    }

    /**
     * 解析接口需要的参数
     */
    private void parseParam(Page page, Html html) {
        //取最后一个script
        String script = Xpath.select("//script[contains(text(),'*')]/text()", html);
        Map<String, Object> params = new HashMap<>();
        if (null != script) {
            System.out.println(script);
            //所有js变量
            String[] vars = script.split(";");
            for (int i = 0; i <= vars.length && i <= 4; i++) {
                String var = vars[i];
                String key = var.substring(var.indexOf("var") + 3, var.lastIndexOf("=")).replaceAll("\\s", "");
                String value = var.substring(var.indexOf("\"") + 1, var.lastIndexOf("\""));
                params.put(key, value);
            }
            if (!params.isEmpty()) {
                String videoApi = videoSource.getVideoApi();
                if (!TextUtils.isEmpty(videoApi)) {
                    //构造Post请求
                    Request request = new Request(videoApi);
                    request.setMethod(HttpConstant.Method.POST)
                            //重定向必须携带，否则404
                            .addHeader(HttpConstant.Header.REFERER, referer)
                            .addHeader(HttpConstant.Header.USER_AGENT, new UserAgentLibrary().getProxyUserAgent())
                            .setRequestBody(HttpRequestBody.form(params, StandardCharsets.UTF_8.name()));
                    request(request, 1, new SpiderListener() {
                        @Override
                        public void onError(Request request, Exception e) {
                            notifyOnFailed(ErrorFlag.ERROR_ON_REQUEST, null == e ? "发送参数解析出错" : e.getMessage());
                        }
                    });
                } else
                    notifyOnFailed(ErrorFlag.API_MISSING, "无法解析,缺少Api!");
            } else
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "解析播放失败");
        } else
            notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "播放解析失败");
    }

    @Override
    protected void extensionParse(Page page, Html html) {
        super.extensionParse(page, html);
        String playUrlRule = videoSource.getPlayUrl();
        if (!TextUtils.isEmpty(playUrlRule)) {
            String playerUrl = Xpath.select(playUrlRule, html);
            if (!TextUtils.isEmpty(playerUrl)) {
                //补全协议头
                if (!playerUrl.startsWith("http"))
                    playerUrl = "https:" + playerUrl;//跟随主域名协议头
                //添加referer
                referer = playerUrl;
                //请求播放器页面
                request(new Request(playerUrl), 1, new SpiderListener() {
                    @Override
                    public void onError(Request request, Exception e) {
                        notifyOnFailed(ErrorFlag.ERROR_ON_REQUEST, null == e ? "发送解析请求失败" : e.getMessage());
                    }
                });
            } else
                notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "播放解析失败!");
        } else
            notifyOnFailed(ErrorFlag.RULE_MISSING, "播放规则缺失!");
    }
}
