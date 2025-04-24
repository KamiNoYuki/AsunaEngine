package com.sho.ss.asuna.engine.base;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.SearchExecBridge;
import com.sho.ss.asuna.engine.interfaces.SearchSpiderListenerBridge;

import java.util.List;


/**
 * @project: 启源视频
 * @author: Sho Tan.
 * @E-mail: 2943343823@qq.com
 * @created: 2022/4/19 17:27:56
 * @description:
 **/
public class BaseSearchSpiderListener extends SearchSpiderListenerBridge
{
    @NonNull
    private final VideoSource source;
    private ExecState execState = ExecState.INIT;
    private final SearchExecBridge execBridge;
    private int id;

    public BaseSearchSpiderListener(@NonNull VideoSource source,@NonNull SearchExecBridge bridge)
    {
        this.source = source;
        this.execBridge = bridge;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public int getId()
    {
        return id;
    }

    public ExecState getExecState()
    {
        return execState;
    }

    public boolean isCompleted()
    {
        return execState == ExecState.COMPLETED;
    }

    public boolean isError()
    {
        return execState == ExecState.ERROR;
    }

    public boolean isEmpty()
    {
        return execState == ExecState.EMPTY;
    }

    public boolean isInit()
    {
        return execState == ExecState.INIT;
    }

    @Override
    public void onSuccess(Request request)
    {
        execBridge.onSuccess(source,request);
    }

    @Override
    public void onError(Request request, Exception e)
    {
        this.execState = ExecState.ERROR;
        super.onError(request, e);
        execBridge.onError(source,request,e);
        onSearchFail();
    }

    @Override
    public void onStarted()
    {
        execBridge.onStarted(source);
    }

    @Override
    public void onCompleted(@NonNull List<Video> videos)
    {
        onCompleted(source,videos,null);
    }

    @Override
    public void onCompleted(@NonNull List<Video> videos, @Nullable String nextPage)
    {
        onCompleted(source,videos,nextPage);
    }

    /**
     * 单个源搜索解析完毕
     *
     * @param source 搜索源
     * @param videos   搜索的结果
     * @param nextPage 下一页链接，可能为空，为空则表示没有下一页或解析下一页的规则错误
     */
    @Override
    public void onCompleted(@NonNull VideoSource source, @NonNull List<Video> videos, @Nullable String nextPage)
    {
        this.execState = ExecState.COMPLETED;
        execBridge.onComplete(source,videos,nextPage);
        onSearchCompleted();
    }

    @Override
    public void onFail(int flag, String errMsg)
    {
        onFail(source,flag,errMsg);
    }

    @Override
    public void onFail(@NonNull VideoSource videoSource, int flag, String errMsg)
    {
        this.execState = ExecState.ERROR;
        execBridge.onFail(source,flag,errMsg);
        onSearchFail();
    }

    @Override
    @Deprecated
    public void searchStart(String keyword)
    {
    }

    @Override
    public void onSection(@NonNull Video video, int total)
    {
        execBridge.onSection(source,video,total);
    }

    /**
     * 所有源搜索完毕时回调
     */
    @Override
    public void onSearchCompleted()
    {
        execBridge.onSearchCompleted();
    }

    /**
     * 所有源搜索失败时回调
     */
    @Override
    public void onSearchFail()
    {
        execBridge.onSearchFail();
    }

    @Override
    public void onEmpty(@NonNull VideoSource source)
    {
        this.execState = ExecState.EMPTY;
        execBridge.onEmpty(source);
        onSearchCompleted();
    }

    @Override
    public void onEmpty()
    {

    }

    public enum ExecState
    {
        INIT(0x1),
        ERROR(0x2),
        COMPLETED(0x3),
        EMPTY(0x4);

        int value;
        ExecState(int value)
        {
            this.value = value;
        }
    }
}
