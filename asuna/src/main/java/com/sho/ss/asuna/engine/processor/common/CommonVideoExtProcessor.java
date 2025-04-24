package com.sho.ss.asuna.engine.processor.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.processor.base.BaseVideoExtensionProcessor;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.interfaces.ParseListener;

import java.util.Map;

/**
 * @project: 启源视频
 * @author: Sho Tan.
 * @E-mail: 2943343823@qq.com
 * @created: 2022/5/9 14:03:07
 * @description:
 **/
@WorkerThread
public abstract class CommonVideoExtProcessor extends BaseVideoExtensionProcessor<VideoSource, Episode>
{
    public CommonVideoExtProcessor(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
    }

    @Override
    protected void notifyOnCompleted()
    {
        if(videoSource.getEpHeader() != null) {
            final Map<String,String> header = videoSource.getEpHeader();
            if(episode.getHeader() != null) {
                header.putAll(episode.getHeader());
            }
            episode.setHeader(header);
        }
        super.notifyOnCompleted();
    }
}
