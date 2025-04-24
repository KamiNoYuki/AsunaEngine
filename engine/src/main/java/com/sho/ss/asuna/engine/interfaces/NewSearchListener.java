package com.sho.ss.asuna.engine.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;

import java.util.List;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/10/11 11:26:32
 * @description 搜索监听器
 **/
public interface NewSearchListener extends ParseListener<List<Video>>
{
    /**
     * 搜索完成
     * @param videos 搜索结果
     * @deprecated 已废弃，使用{@link #onSingleSourceCompleted(VideoSource, Pair)} 或者 {@link #onAllCompleted(List)} 代替
     */
    @Override
    @Deprecated
    default void onCompleted(@NonNull List<Video> videos)
    {

    }

    /**
     * 搜索失败
     * @param flag {@link com.sho.ss.asuna.engine.constant.ErrorFlag} 错误标志
     * @param errMsg 错误信息
     * @deprecated 使用 {@link #onInitFailed(Pair)} 或者 {@link #onSingleSourceFailed(Pair, VideoSource, Pair)} 或者 {@link #onAllFailed(List)} 代替
     */
    @Deprecated
    @Override
    default void onFail(int flag, String errMsg)
    {

    }


    /**
     * 搜索任务开始
     * @deprecated 使用 {@link #onStarted(String)} 代替
     */
    @Override
    @Deprecated
    default void onStarted()
    {

    }

    /**
     * 搜索任务开始，仅回调一次
     * @param keyword 搜索关键词
     */
    void onStarted(@NonNull String keyword);

    /**
     * 在初始化搜索任务期间出错时回调
     * @param errInfo 错误信息
     */
    void onInitFailed(@NonNull Pair<Integer,String> errInfo);

    /**
     * 单个源搜索完毕时回调
     * @param videos 单个源的搜索结果，以及下一页链接，下一页链接可能为空
     * @param videoSource 搜索完毕的源
     */
    void onSingleSourceCompleted(@NonNull VideoSource videoSource, @NonNull Pair<List<Video>,String> videos);

    /**
     * 在单个源未搜索到结果时回调
     * @param videoSource 没有搜索结果的源
     */
    void onSingleSourceEmpty(@NonNull VideoSource videoSource);

    /**
     * 在单个源执行搜索失败时回调
     * @param requestEx 该参数仅在执行请求搜索时出错才会有，可能为空！
     * @param videoSource 搜索失败的源
     * @param errInfo 错误标志及错误信息
     */
    void onSingleSourceFailed(@Nullable Pair<Request,Exception> requestEx, @NonNull VideoSource videoSource, @NonNull Pair<Integer,String> errInfo);

    default void onSingleSourceFailed(@NonNull VideoSource videoSource, @NonNull Pair<Integer,String> errInfo)
    {
        onSingleSourceFailed(null,videoSource,errInfo);
    }

    /**
     * 在搜索百分比进度变化时回调
     * @param currentPer 当前搜索进度百分比
     * @param currentCount 到目前为止已搜索的任务数量
     * @param totalCount 总搜索任务数量
     */
    void onSearchingProgress(int currentPer,int currentCount, int totalCount);

    /**
     * 全部源搜索执行完毕且都有数据时回调
     * @param sources 全部搜索完成的源，以及搜索结果、下一页链接
     */
    void onAllCompleted(@NonNull List<Pair<VideoSource,Pair<List<Video>,String>>> sources);

    /**
     * 全部源搜索结果为空时回调
     * @param sources 搜索结果为空的源
     */
    void onAllEmpty(@NonNull List<VideoSource> sources);

    /**
     * 全部源搜索失败时回调
     * @param errSources 搜索失败的源以及错误信息
     */
    void onAllFailed(@NonNull List<Pair<VideoSource,Pair<Integer,String>>> errSources);

    /**
     * 全部搜索任务执行完毕时回调（不论加载是否成功，仅在全部解析任务执行完毕后回调）
     * @param completedSources 加载成功的源列表
     * @param failedSources 加载失败的源列表
     */
    void onSearchTaskCompleted(@NonNull List<Pair<VideoSource,Pair<List<Video>,String>>> completedSources,@NonNull List<VideoSource> emptySources,@NonNull List<Pair<VideoSource,Pair<Integer,String>>> failedSources);

    /**
     * 解析下一页时出错
     * @param requestEx 仅请求失败时才会传递该参数，可能为空！
     * @param videoSource 该下一页对应的源
     * @param errInfo 错误信息
     */
    void onNextPageFailed(@Nullable Pair<Request,Exception> requestEx, @NonNull VideoSource videoSource, @NonNull Pair<Integer,String> errInfo);

    /**
     * 在初始化搜索任务期间出错时回调
     * @param errInfo 错误信息
     */
    void onNextPageInitFailed(@NonNull Pair<Integer,String> errInfo);
}
