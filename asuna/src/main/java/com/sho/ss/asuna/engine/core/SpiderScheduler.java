package com.sho.ss.asuna.engine.core;

import com.sho.ss.asuna.engine.core.scheduler.Scheduler;
import com.sho.ss.asuna.engine.core.thread.CountableThreadPool;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author ShoTan.
 * @project 启源视频
 * @email 2943343823@qq.com
 * @created 2024/6/11 21:16
 * @description
 **/
public class SpiderScheduler {
    private Scheduler scheduler;
    private final ReentrantLock newUrlLock = new ReentrantLock();
    private final Condition newUrlCondition = newUrlLock.newCondition();

    public SpiderScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Request poll(Spider spider) {
        return scheduler.poll(spider);
    }

    public void push(Request request, Spider spider) {
        scheduler.push(request, spider);
    }

    public boolean waitNewUrl(CountableThreadPool threadPool, long emptySleepTime) {
        newUrlLock.lock();
        try {
            if (threadPool.getThreadAlive() == 0) {
                return false;
            }
            return newUrlCondition.await(emptySleepTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return true;
        } finally {
            newUrlLock.unlock();
        }
    }

    public void signalNewUrl() {
        try {
            newUrlLock.lock();
            newUrlCondition.signalAll();
        } finally {
            newUrlLock.unlock();
        }
    }

}