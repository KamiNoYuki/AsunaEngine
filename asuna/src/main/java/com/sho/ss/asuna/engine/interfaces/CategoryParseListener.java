package com.sho.ss.asuna.engine.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.entity.Category;
import com.sho.ss.asuna.engine.entity.Video;

import java.util.List;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/9/15 16:14:58
 * @description
 **/
public interface CategoryParseListener extends ParseListener<List<Video>>
{
    @Override
    @Deprecated
    default void onCompleted(@NonNull List<Video> videos)
    {

    }

    void onCompleted(@NonNull List<Category> categories, @NonNull List<Video> videos, @Nullable String nextPageUrl);

    void onEmpty(@NonNull String tips);
}
