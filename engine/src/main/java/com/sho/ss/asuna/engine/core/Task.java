package com.sho.ss.asuna.engine.core;

/**
 * Interface for identifying different tasks.<br>
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.1.0
 * @see com.sho.ss.asuna.engine.core.scheduler.Scheduler
 * @see com.sho.ss.asuna.engine.core.pipeline.Pipeline
 */
public interface Task {

    /**
     * unique id for a task.
     *
     * @return uuid
     */
    public String getUUID();

    /**
     * site of a task
     *
     * @return site
     */
    public Site getSite();

}
