package com.sho.ss.asuna.engine.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.entity.Page;
import com.sho.ss.asuna.engine.entity.PageSource;
import com.sho.ss.asuna.engine.entity.Video;

import java.util.List;

public interface PageParseListener extends ParseListener<List<Video>>
{
    @Override
    @Deprecated
    default void onCompleted(@NonNull List<Video> videos)
    {

    }
    /**
     * 解析完成时回调
     * @param pageSource page所属的page源
     * @param page 解析页对象
     * @param nextUrl 下一页链接
     * @param videos result
     */
    void onCompleted(@NonNull PageSource pageSource, @NonNull Page page, @Nullable String nextUrl, @NonNull List<Video> videos);
}
