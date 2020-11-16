package com.wangc.base.lib.threadPoolManager;


public abstract class AbstractThreadPoolWrapper implements ThreadPoolWrapper {
      String threadKey;
      DefaultThreadPoolMetrics metrics;
      ThreadPoolProperties threadPoolProperties;

    public AbstractThreadPoolWrapper(ThreadPoolProperties threadPoolProperties) {
        this.threadPoolProperties = threadPoolProperties;
        this.threadKey = threadPoolProperties.getThreadKey();
    }

    public AbstractThreadPoolWrapper() {
    }
    @Override
    public DefaultThreadPoolMetrics getMetrics() {
        if (metrics == null) {
            metrics = new DefaultThreadPoolMetrics(getExecutor());
        }
        return metrics;
    }


    public void setThreadKey(String threadKey) {
        this.threadKey = threadKey;
    }

    @Override
    public String getThreadKey() {
        return threadKey;
    }

    @Override
    public ThreadPoolProperties getThreadPoolProperties() {
        return threadPoolProperties;
    }

    public void setThreadPoolProperties(ThreadPoolProperties threadPoolProperties) {
        this.threadPoolProperties = threadPoolProperties;
    }


    abstract void init();

}
