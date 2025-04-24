package com.sho.ss.asuna.engine.interceptor;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.base.BaseSearchSpiderListener;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.SearchExecBridge;
import com.sho.ss.asuna.engine.interfaces.SearchListener;

import java.util.List;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/5/15 21:25:20
 * @description 搜索监听拦截器
 **/
public class SearchInterceptor implements SearchExecBridge
{
    @Nullable
    private List<BaseSearchSpiderListener> searchSpiderListeners;
    @Nullable
    private final SearchListener searchListener;
    private boolean isStarted = false;

    public SearchInterceptor(@Nullable SearchListener searchListener)
    {
        this.searchListener = searchListener;
    }

    public SearchInterceptor(@Nullable List<BaseSearchSpiderListener> searchSpiderListeners, @Nullable SearchListener searchListener)
    {
        this.searchSpiderListeners = searchSpiderListeners;
        this.searchListener = searchListener;
    }

    @Nullable
    public SearchListener getSearchListener()
    {
        return searchListener;
    }

    public void addSearchListenerQueue(@NonNull List<BaseSearchSpiderListener> searchSpiderListeners)
    {
        this.searchSpiderListeners = searchSpiderListeners;
    }

    @Override
    @Deprecated
    public void onSuccess(VideoSource source, Request request)
    {
        //根据测试发现Spider回调此方法不严谨，因此弃用
        //未解析到界面会回调该方法，报某些Exception的情况下也会回调该方法
    }

    @Override
    public void onError(VideoSource source, Request request, Exception e)
    {
        onFail(source, ErrorFlag.EXCEPTION_WHEN_PARSING,e.getMessage());
    }

    @Override
    public void onStarted(VideoSource source)
    {
        //仅第一次解析时onStarted
        if(null != searchListener && !isStarted)
        {
            searchListener.onStarted();
            isStarted = true;
        }
    }

    /**
     * 所有源搜索完毕回调
     */
    @Override
    public void onSearchCompleted()
    {
        boolean result = checkAllStateNot(BaseSearchSpiderListener.ExecState.INIT);
        if(null != searchListener && result)
            searchListener.onSearchCompleted();
    }

    /**
     * 所有源搜索失败时回调
     */
    @Override
    public void onSearchFail()
    {
        //所有源都fail才通知listener搜索fail
        if(null != searchListener && checkAllStateIs(BaseSearchSpiderListener.ExecState.ERROR))
            searchListener.onSearchFail();
        onSearchCompleted();
    }

    /**
     * 搜索完成时回调
     *
     * @param source   搜索源
     * @param videos   搜索结果
     * @param nextPage 下一页链接
     */
    @Override
    public void onComplete(@NonNull VideoSource source, @NonNull List<Video> videos, @Nullable String nextPage)
    {
        if(null != searchListener)
            searchListener.onCompleted(source,videos,nextPage);
    }

    /**
     * 单个源解析失败时回调
     * @param source 解析失败的源
     * @param flag 错误标志
     * @param errMsg 错误信息
     */
    @Override
    public void onFail(VideoSource source, int flag, String errMsg)
    {
        new Handler(Looper.getMainLooper()).post(() ->
        {
            if(null != searchListener)
                searchListener.onFail(flag,errMsg);
            onSearchFail();
        });
        Log.e(SearchInterceptor.class.getSimpleName() , " -> onFail: sourceName=[" + source.getName() + "] flag=[" + flag + "] errMsg=[" + errMsg + "]");
    }

    @Override
    public void onSection(VideoSource source, Video video, int total)
    {
        if(null != searchListener)
            searchListener.onSection(video,total);
        onSearchCompleted();
    }

    @Override
    public void onEmpty(VideoSource source)
    {
        if(null != searchListener && checkAllStateIs(BaseSearchSpiderListener.ExecState.EMPTY))
            searchListener.onEmpty(source);
        onSearchCompleted();
    }

    protected boolean checkAllStateNot(BaseSearchSpiderListener.ExecState execState)
    {
        return checkAllState(execState,false);
    }

    protected boolean checkAllStateIs(BaseSearchSpiderListener.ExecState execState)
    {
        return checkAllState(execState,true);
    }

    protected boolean checkAllState(BaseSearchSpiderListener.ExecState execState,boolean is)
    {
        if(null != searchSpiderListeners && searchSpiderListeners.size() > 0)
        {
            for (BaseSearchSpiderListener searchSpiderListener : searchSpiderListeners)
            {
                if(is)
                {
                    if(execState != searchSpiderListener.getExecState())
                        return false;
                }
                else
                {
                    if(execState == searchSpiderListener.getExecState())
                        return false;
                }
            }
            return true;
        }
        else
            return false;
    }
}
