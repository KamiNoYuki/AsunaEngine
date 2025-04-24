package com.sho.ss.asuna.engine.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.entity.Video;

import java.util.List;

/**
 * @author Sho Tan.
 * @description: 节点与其他信息的解析监听器
 */
public interface DetailInfoParseListener extends ParseListener<Video>
{
    @Override
    @Deprecated
    default void onCompleted(@NonNull Video video){
        onCompleted(video,null);
    }

    void onCompleted(@NonNull Video video,@Nullable List<Video> relatedVideoList);
}
