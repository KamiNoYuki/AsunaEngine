package com.sho.ss.asuna.engine.extension.handler;

import com.sho.ss.asuna.engine.core.ResultItems;
import com.sho.ss.asuna.engine.core.Task;
import com.sho.ss.asuna.engine.core.pipeline.Pipeline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author code4crafer@gmail.com
 */
public class CompositePipeline implements Pipeline
{

    private List<SubPipeline> subPipelines = new ArrayList<>();

    @Override
    public void process(ResultItems resultItems, Task task) {
        for (SubPipeline subPipeline : subPipelines) {
            if (subPipeline.match(resultItems.getRequest())) {
                RequestMatcher.MatchOther matchOtherProcessorProcessor = subPipeline.processResult(resultItems, task);
                if (matchOtherProcessorProcessor != RequestMatcher.MatchOther.YES) {
                    return;
                }
            }
        }
    }

    public CompositePipeline addSubPipeline(SubPipeline subPipeline) {
        this.subPipelines.add(subPipeline);
        return this;
    }

    public CompositePipeline setSubPipeline(SubPipeline... subPipelines) {
        this.subPipelines = new ArrayList<>();
        this.subPipelines.addAll(Arrays.asList(subPipelines));
        return this;
    }

}
