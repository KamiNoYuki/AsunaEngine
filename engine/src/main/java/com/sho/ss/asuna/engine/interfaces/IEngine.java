package com.sho.ss.asuna.engine.interfaces;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.entity.CategorySource;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Page;
import com.sho.ss.asuna.engine.entity.PageSource;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.CategoryParseListener;
import com.sho.ss.asuna.engine.interfaces.DetailInfoParseListener;
import com.sho.ss.asuna.engine.interfaces.NewSearchListener;
import com.sho.ss.asuna.engine.interfaces.PageParseListener;
import com.sho.ss.asuna.engine.interfaces.ParseListener;

import java.util.List;

/**
 * @author Sho tan
 */
public interface IEngine<T>
{
    /**
     * 执行搜索
     * @param keyword 搜索关键词
     * @param searchListener 搜索监听
     */
    void search(List<VideoSource> sources, @NonNull String keyword, @NonNull NewSearchListener searchListener);

    /**
     * 解析搜索下一页
     * @param nextPage       下一页链接
     * @param source         该搜索对应得到搜索源
     * @param searchListener 监听器
     */
    void searchNextPage(@NonNull String nextPage, @NonNull VideoSource source, @NonNull NewSearchListener searchListener);

    /**
     * 解析视频的相关详情页面信息
     * @param t t
     * @param detailInfoParseListener 解析结果监听
     */
    void parseDetailInfo(@NonNull T t, @Nullable DetailInfoParseListener detailInfoParseListener);

    /**
     * 解析分类页面数据
     * @param videoSource 视频源
     * @param categoryUrl 分类页面链接
     * @param parseListener 解析监听器
     */
    void parseCategory(VideoSource videoSource, CategorySource categorySource, String categoryUrl, @NonNull CategoryParseListener parseListener);

    void parseCategory(VideoSource videoSource, CategorySource categorySource, @NonNull CategoryParseListener parseListener);

    void parseCategory(@NonNull VideoSource videoSource, @IntRange(from = 0) int categoryIndex,String categoryUrl, @NonNull CategoryParseListener parseListener);
    /**
     * 解析分类页面数据
     * @param videoSource 视频源
     * @param categoryIndex videoSource中的分类源索引
     * @param parseListener 解析监听器
     */
    void parseCategory(VideoSource videoSource, @IntRange(from = 0) int categoryIndex, @NonNull CategoryParseListener parseListener);

    /**
     * 解析视频url/播放直链
     * @param t t
     * @param episode 要解析的对应集数
     * @param parseListener 解析结果监听
     */
    void parseVideoUrl(@NonNull T t, @NonNull Episode episode, @NonNull ParseListener<Episode> parseListener);

    void parsePage(PageSource pageSource,boolean trendFirst, @Nullable PageParseListener listener);

    void parsePage(PageSource pageSource, @Nullable PageParseListener listener);

    void parsePage(List<PageSource> pageSources, boolean trendingFirstBySource, boolean trendingFirst, @Nullable PageParseListener listener);

    /**
     * 解析页面数据
     * @param pageSource pageSource
     * @param trendingFirst 是否优先使用热门数据解析规则
     * @param listener listener
     */
    void parsePage(PageSource pageSource, @Nullable Page page, boolean trendingFirst, @Nullable PageParseListener listener);
}
