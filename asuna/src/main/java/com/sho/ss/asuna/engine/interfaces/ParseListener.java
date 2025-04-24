package com.sho.ss.asuna.engine.interfaces;

import androidx.annotation.NonNull;

/**
 * @author Sho Tan.
 * @description: 解析监听器
 */
public interface ParseListener<V>
{
    /**
     * 解析开始时回调
     */
    void onStarted();

    /**
     * 解析完成时回调
     * @param v result
     */
    void onCompleted(@NonNull V v);

    /**
     * 解析错误，可能为规则错误导致，也有可能网页内容发生变化、或网络问题导致
     * @param flag {@link com.sho.ss.asuna.engine.constant.ErrorFlag} 错误标志
     * @param errMsg 错误信息
     */
    void onFail(int flag,String errMsg);
}
