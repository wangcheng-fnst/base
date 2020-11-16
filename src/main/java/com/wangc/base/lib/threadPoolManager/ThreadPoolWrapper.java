package com.wangc.base.lib.threadPoolManager;


import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public interface ThreadPoolWrapper {

    void execute(Runnable r);

//    void execute(Runnable task, long startTimeout);

    Future<?> submit(Runnable task);

    DefaultThreadPoolMetrics getMetrics();

    String getThreadKey();

    ThreadPoolProperties getThreadPoolProperties();

    void updatePoolProperties(ThreadPoolProperties poolProperties);

    ThreadPoolExecutor getExecutor();

}
