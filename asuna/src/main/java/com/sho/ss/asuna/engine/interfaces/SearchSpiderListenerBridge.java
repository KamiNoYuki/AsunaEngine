package com.sho.ss.asuna.engine.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.SpiderListener;
import com.sho.ss.asuna.engine.entity.Video;

import java.util.List;

public abstract class SearchSpiderListenerBridge implements SpiderListener,SearchListener
{

    public abstract void onEmpty();

    public abstract void onCompleted(@NonNull List<Video> videos, @Nullable String nextPage);
}
