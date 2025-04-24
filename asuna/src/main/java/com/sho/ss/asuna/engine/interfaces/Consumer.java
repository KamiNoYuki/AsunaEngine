package com.sho.ss.asuna.engine.interfaces;

/**
 * @project: 启源视频
 * @author: Sho Tan.
 * @E-mail: 2943343823@qq.com
 * @created: 2022/4/9 10:41:53
 * @description: Because of the Android SDK limit replace the Consumer of JDK
 **/
public interface Consumer<T,U>
{
    void accept(T var1, U var2);
}
