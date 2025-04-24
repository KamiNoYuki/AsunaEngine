package com.sho.ss.asuna.engine.processor.ext;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.VideoProcessor;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2023/3/14 10:55:21
 * @description 继承自VideoProcessor，主要检测该源的视频是否因版权原因禁止播放
 **/
public class VideoCopyrightChecker extends VideoProcessor {
    public VideoCopyrightChecker(@NonNull Video entity, @NonNull VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener) {
        super(entity, videoSource, episode, listener);
    }

    @Override
    public void extensionParse(Page page, Html html) {
        //尝试获取版权提示元素，如果存在则说明该视频因版权问题禁止播放。
        boolean noCopyright = !TextUtils.isEmpty($(getCopyrightTipTagRule(), html));
        if (noCopyright)
            notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "该源因版权限制已下架此视频");
        else
            super.extensionParse(page, html);
    }

    /**
     * 提示版权问题的标签Xpath解析规则，通过解析该规则获取其标签，若该标签存在则直接判定无版权
     * @return xpathRule
     */
    @NonNull
    public String getCopyrightTipTagRule() {
        return "//div[@class='mxonenotice']";
    }
}
