package com.sho.ss.asuna.engine.extension.model;

import com.sho.ss.asuna.engine.core.Task;
import com.sho.ss.asuna.engine.extension.pipeline.PageModelPipeline;

import org.apache.commons.lang3.builder.ToStringBuilder;


/**
 * Print page model in console.<br>
 * Usually used in test.<br>
 * @author code4crafter@gmail.com <br>
 * @since 0.2.0
 */
public class ConsolePageModelPipeline implements PageModelPipeline
{
    @Override
    public void process(Object o, Task task) {
        System.out.println(ToStringBuilder.reflectionToString(o));
    }
}
