package com.sho.ss.asuna.engine.core.scheduler;


import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import com.sho.ss.asuna.engine.core.Site;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Basic Scheduler implementation.<br>
 * Store urls to fetch in LinkedBlockingQueue and remove duplicate urls by HashMap.
 *
 * Note: if you use this {@link QueueScheduler}
 * with {@link Site#getCycleRetryTimes()} enabled, you may encountered dead-lock
 * when the queue is full.
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.1.0
 */
public class QueueScheduler extends DuplicateRemovedScheduler implements MonitorableScheduler {

    private final BlockingQueue<Request> queue;

    public QueueScheduler() {
        this.queue = new LinkedBlockingQueue<>();
    }

    /**
     * Creates a {@code QueueScheduler} with the given (fixed) capacity.
     *
     * @param capacity the capacity of this queue,
     * see {@link LinkedBlockingQueue#LinkedBlockingQueue(int)}
     * @since 0.8.0
     */
    public QueueScheduler(int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
    }

    @Nullable
    public List<Request> getAllRequests() {
        if(!queue.isEmpty()) {
            final ArrayList<Request> data = new ArrayList<>();
            queue.drainTo(data);
            return data;
        } else {
            return null;
        }
    }

    public boolean remove(@NotNull Request request) {
        return queue.remove(request);
    }

    @Override
    public void pushWhenNoDuplicate(Request request, Task task) {
        System.out.println("Remaining capacity: " + queue.remainingCapacity());
        try {
            queue.put(request);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Request poll(Task task) {
        return queue.poll();
    }

    @Override
    public int getLeftRequestsCount(Task task) {
        return queue.size();
    }

    @Override
    public int getTotalRequestsCount(Task task) {
        return getDuplicateRemover().getTotalRequestsCount(task);
    }
}