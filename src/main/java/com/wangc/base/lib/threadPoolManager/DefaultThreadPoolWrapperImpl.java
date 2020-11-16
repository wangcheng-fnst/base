package com.wangc.base.lib.threadPoolManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

public class DefaultThreadPoolWrapperImpl extends AbstractThreadPoolWrapper {

    public static final Logger LOGGER = LoggerFactory.getLogger(DefaultThreadPoolWrapperImpl.class);

    ThreadPoolTaskExecutor taskExecutor;

    public DefaultThreadPoolWrapperImpl(ThreadPoolProperties threadPoolProperties) {
        super(threadPoolProperties);
        init();
    }


    @Override
    public void execute(Runnable r) {
        taskExecutor.execute(r);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return taskExecutor.submit(task);
    }

    @Override
    public void updatePoolProperties(ThreadPoolProperties poolProperties) {
        if (poolProperties.getCoreSize() != null){
            this.threadPoolProperties.setCoreSize(poolProperties.getCoreSize());
            taskExecutor.setCorePoolSize(poolProperties.getCoreSize());
        }
        if (poolProperties.getMaxSize() != null){
            this.threadPoolProperties.setMaxSize(poolProperties.getMaxSize());
            taskExecutor.setMaxPoolSize(poolProperties.getMaxSize());
        }
    }


    @Override
    public ThreadPoolExecutor getExecutor() {
        return taskExecutor.getThreadPoolExecutor();
    }
    @Override
    void init() {
        int coreSize = threadPoolProperties.getCoreSize();
        int maxSize = threadPoolProperties.getMaxSize();
        int keepAliveTimeInMinutes = threadPoolProperties.getKeepAliveTimeInMinutes();
        int workingQueueSize = threadPoolProperties.getWorkingQueueSize();
        taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(coreSize);
        taskExecutor.setKeepAliveSeconds(keepAliveTimeInMinutes * 60);
        taskExecutor.setMaxPoolSize(maxSize);
        taskExecutor.setQueueCapacity(workingQueueSize);
        taskExecutor.setThreadNamePrefix(threadKey);
        MonitorRejectedHandler handler = new MonitorRejectedHandler();
        taskExecutor.setRejectedExecutionHandler(handler);
        taskExecutor.initialize();
        handler.setKey(threadKey);
        handler.setMetrics(getMetrics());


    }

    static class MonitorRejectedHandler implements RejectedExecutionHandler {

        DefaultThreadPoolMetrics metrics;
        String key;

        public MonitorRejectedHandler() {
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            LOGGER.error(key+"{key} 线程池出现拥挤，开始丢弃任务！");
            metrics.markThreadRejection();
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public DefaultThreadPoolMetrics getMetrics() {
            return metrics;
        }

        public void setMetrics(DefaultThreadPoolMetrics metrics) {
            this.metrics = metrics;
        }
    }
}
