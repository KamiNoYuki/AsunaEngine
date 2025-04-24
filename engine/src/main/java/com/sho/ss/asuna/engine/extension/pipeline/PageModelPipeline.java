package com.sho.ss.asuna.engine.extension.pipeline;


import com.sho.ss.asuna.engine.core.Task;

/**
 * Implements PageModelPipeline to persistent your page model.
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.2.0
 */
public interface PageModelPipeline<T> {

    public void process(T t, Task task);

}
