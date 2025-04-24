package com.sho.ss.asuna.engine.processor;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Site;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.constant.EngineConstant;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.common.CommonVideoExtProcessor;
import com.sho.ss.asuna.engine.utils.RegexHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @project: 启源视频
 * @author: Sho Tan.
 * @E-mail: 2943343823@qq.com
 * @created: 2022/4/18 20:51:42
 * @description: 常规视频处理器
 **/
public class VideoProcessor extends CommonVideoExtProcessor {

    private boolean autoDecodePlayUrlByType = true;

    public VideoProcessor(@NonNull Video entity, @NonNull VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener) {
        super(entity, videoSource, episode, listener);
    }

    /**
     * 该方法负责通过常规方式进行解析视频直链的处理器
     *
     * @param page page
     * @param html html
     */
    @Override
    public void extensionParse(Page page, Html html) {
        VideoSource source = entity.getVideoSource();
        if (null != source) {
            //播放链接规则
            String playUrl = source.getPlayUrl();
            if (!TextUtils.isEmpty(playUrl)) {
                System.out.println("playUrlRule: " + playUrl);
                String str = $(playUrl, html);
                System.out.println("playUrl: " + str);
                if (!TextUtils.isEmpty(str)) {
                    if (TextUtils.equals(source.getPlayExtractor(), EngineConstant.EXTRACTOR_REGEX)) {
                        String regex = videoSource.getPlayUrlRegex();
                        if (whenNullNotifyFail(regex, ErrorFlag.REGEX_MISSING, "视频链接正则缺失") && null != regex) {
                            final Map<String, String> params = RegexHelper.INSTANCE.extractParamsWithRegex(str, regex, s ->
                            {
                                notifyOnFailed(ErrorFlag.REGEX_EXTRACT_ERROR, s);
                                return null;
                            });
                            if (null != params) {
                                handleParams(page, params);
                            }
                        }
                    } else if (TextUtils.equals(source.getPlayExtractor(), EngineConstant.EXTRACTOR_SUB))
                        postPlayUrl(page, extractUrlWithSubstring(str));
                    else if (TextUtils.isEmpty(source.getPlayExtractor()))
                        postPlayUrl(page, str);
                    else
                        notifyOnFailed(ErrorFlag.EXTRACTOR_UNKNOWN, "链接抽取方式错误");
                } else
                    notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "播放器参数获取失败");
            } else
                notifyOnFailed(ErrorFlag.RULE_MISSING, "播放器信息规则缺失");
        } else notifyOnFailed(ErrorFlag.NO_SOURCE_WHEN_VIDEO_PARSE, "空的视频源");
    }

    protected void handleParams(@NonNull Page page, @NonNull Map<String, String> params) {
        final String videoUrl = params.get("value") != null ? params.get("value") : ((ArrayList<String>) params.values()).get(0);
        if (whenNullNotifyFail(videoUrl, ErrorFlag.EMPTY_VIDEO_URL, "视频解析失败") && null != videoUrl) {
            postPlayUrl(page, videoUrl);
        }
    }

    protected void postPlayUrl(@NonNull Page page, @NonNull String videoUrl) {
        System.out.println("postPlayUrl: " + videoUrl);
        if (!TextUtils.isEmpty(videoUrl)) {
            //有些链接编码为gbk2312，因此转码为utf-8
            if (videoSource.isWatchTranscoding())
                videoUrl = transcoding(videoUrl);
            if (autoDecodePlayUrlByType && null != videoUrl) {
                videoUrl = decodeUrlByType(videoUrl);
            }
            //防止加密的是转码后的链接，因此解码后再尝试一次url解码操作
            if (videoSource.isWatchTranscoding())
                videoUrl = transcoding(videoUrl);
            Map<String, String> playUrlFilter = videoSource.getPlayUrlFilter();
            videoUrl = applyFilter(videoUrl, playUrlFilter, isUseDefaultFilter());
            //相对转绝对
            if (videoSource.isToAbsoluteUrlInPlay())
                videoUrl = toAbsoluteUrl(page.getUrl().get(), videoUrl);
            notifyOnCompleted(videoUrl);
            System.out.println("已过滤URL:[" + videoUrl + "],解析完毕");
        } else
            notifyOnFailed(ErrorFlag.EMPTY_VIDEO_URL, "视频链接为空");
    }

    public boolean isAutoDecodePlayUrlByType() {
        return autoDecodePlayUrlByType;
    }

    public void setAutoDecodePlayUrlByType(boolean autoDecodePlayUrlByType) {
        this.autoDecodePlayUrlByType = autoDecodePlayUrlByType;
    }

    @Override
    public Site getSite() {
        return super.getSite();
    }
}
