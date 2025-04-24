package com.sho.ss.asuna.engine.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;

import java.util.List;


/**
 * @author Sho tan
 */
public interface SearchListener extends ParseListener<List<Video>>
{
    /**
     * 解析完成时回调
     * @deprecated Use {@link #onCompleted(VideoSource,List, String)} instead.
     * @param videos 解析到的全部video
     */
    @Override
    @Deprecated
    default void onCompleted(@NonNull List<Video> videos)
    {
    }

    /**
     * 单个源搜索解析完毕
     * @param source 搜索源
     * @param videos 搜索的结果
     * @param nextPage 下一页链接，可能为空，为空则表示没有下一页或解析下一页的规则错误
     */
    void onCompleted(@NonNull VideoSource source, @NonNull List<Video> videos, @Nullable String nextPage);

    /**
     * 搜索开始时回调
     * @param keyword 搜索关键词
     */
    void searchStart(String keyword);
    /**
     * 部分搜索结果回调
     * @param video 部分结果
     * @param total 搜索结果总数
     */
    void onSection(@NonNull Video video,int total);

    /**
     * 所有源搜索完毕时回调
     */
    void onSearchCompleted();

    @Override
    default void onFail(int flag, String errMsg)
    {

    }

    /**
     * 单个源搜索失败
     * @param videoSource 搜索源
     * @param flag 错误标志
     * @param errMsg 错误信息
     */
    void onFail(@NonNull VideoSource videoSource,int flag,String errMsg);

    /**
     * 所有源搜索失败时回调
     */
    void onSearchFail();

    /**
     * 搜索完成，无相关搜索结果
     */
    void onEmpty(@NonNull VideoSource source);
}
