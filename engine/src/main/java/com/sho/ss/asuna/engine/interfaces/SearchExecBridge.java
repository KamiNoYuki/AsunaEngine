package com.sho.ss.asuna.engine.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;

import java.util.List;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/5/16 13:57:28
 * @description 搜索爬虫监听器与搜索调度器的桥接器
 **/
public interface SearchExecBridge
{
    void onSuccess(VideoSource source, Request request);

    void onError(VideoSource source,Request request, Exception e);

    void onStarted(VideoSource source);
    /**
     * 搜索完成时回调
     * @param source 搜索源
     * @param videos 搜索结果
     * @param nextPage 下一页链接
     */
    void onComplete(@NonNull VideoSource source, @NonNull List<Video> videos, @Nullable String nextPage);

    void onFail(VideoSource source,int flag, String errMsg);

    void onSection(VideoSource source,Video video, int total);

    void onEmpty(VideoSource source);

    void onSearchCompleted();

    void onSearchFail();
}
