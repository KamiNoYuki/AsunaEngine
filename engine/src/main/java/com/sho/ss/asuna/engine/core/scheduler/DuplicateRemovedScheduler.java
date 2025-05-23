package com.sho.ss.asuna.engine.core.scheduler;

import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.Task;
import com.sho.ss.asuna.engine.core.scheduler.component.DuplicateRemover;
import com.sho.ss.asuna.engine.core.scheduler.component.HashSetDuplicateRemover;
import com.sho.ss.asuna.engine.core.utils.HttpConstant;


/**
 * Remove duplicate urls and only push urls which are not duplicate.<br><br>
 *
 * @author code4crafer@gmail.com
 * @since 0.5.0
 */
public abstract class DuplicateRemovedScheduler implements Scheduler {

//    protected Logger logger = LoggerFactory.getLogger(getClass());

    private DuplicateRemover duplicatedRemover = new HashSetDuplicateRemover();

    public DuplicateRemover getDuplicateRemover() {
        return duplicatedRemover;
    }

    public DuplicateRemovedScheduler setDuplicateRemover(DuplicateRemover duplicatedRemover) {
        this.duplicatedRemover = duplicatedRemover;
        return this;
    }

    @Override
    public void push(Request request, Task task) {
//        logger.trace("get a candidate url {}", request.getUrl());
        System.out.println("Get a candidate url {" + request.getUrl() + "}");
        if (shouldReserved(request) || noNeedToRemoveDuplicate(request) || !duplicatedRemover.isDuplicate(request, task)) {
//            logger.debug("push to queue {}", request.getUrl());
            System.out.println("Push to queue {" + request.getUrl() + "}");
            pushWhenNoDuplicate(request, task);
        }
    }

    protected boolean shouldReserved(Request request) {
        return request.getExtra(Request.CYCLE_TRIED_TIMES) != null;
    }

    protected boolean noNeedToRemoveDuplicate(Request request) {
        return HttpConstant.Method.POST.equalsIgnoreCase(request.getMethod());
    }

    protected void pushWhenNoDuplicate(Request request, Task task) {

    }
}
