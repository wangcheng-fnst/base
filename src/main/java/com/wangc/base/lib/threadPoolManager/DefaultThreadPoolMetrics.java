package com.wangc.base.lib.threadPoolManager;



import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultThreadPoolMetrics {

    public ThreadPoolExecutor pool;
    private final AtomicInteger concurrentExecutionCount = new AtomicInteger();
    private long[] costTimes = new long[20];

    public DefaultThreadPoolMetrics(ThreadPoolExecutor pool) {
        this.pool = pool;
    }

    public Number getCurrentActiveCount() {
        return pool.getActiveCount();
    }

    public Number getCurrentCompletedTaskCount() {
        return pool.getCompletedTaskCount();
    }

    public Number getCurrentCorePoolSize() {
        return pool.getCorePoolSize();
    }

    public Number getCurrentLargestPoolSize() {
        return pool.getLargestPoolSize();
    }

    public Number getCurrentMaximumPoolSize() {
        return pool.getMaximumPoolSize();
    }

    public Number getCurrentPoolSize() {
        return pool.getPoolSize();
    }

    public Number getCurrentTaskCount() {
        return pool.getTaskCount();
    }

    public Number getCurrentQueueSize() {
        return pool.getQueue().size();
    }

    public void markThreadRejection() {
        concurrentExecutionCount.getAndIncrement();
    }

    public int getRejectionCount(){
        return concurrentExecutionCount.get();
    }

    public void addCostTime(String threadName,Long costTime){
        int index = (int)(System.nanoTime() % 20);
        costTimes[index] = costTime;
    }

    public long[] getCostTimes() {
        return costTimes;
    }
}
