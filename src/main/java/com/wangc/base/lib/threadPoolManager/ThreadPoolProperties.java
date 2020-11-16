package com.wangc.base.lib.threadPoolManager;


/**
 * 线程池参数配置
 */
public class ThreadPoolProperties {
    static int default_coreSize = 10;            // core size of thread pool
    static int default_maximumSize = 10;         // maximum size of thread pool
    static int default_keepAliveTimeMinutes = 5; // minutes to keep a thread alive
    static int default_maxQueueSize = -1;       //working queue size,default in infinity
    static String default_threadKey = "default-pool";       //
    static String default_aClass = "com.suning.union.lib.threadPoolManager.DefaultThreadPoolWrapperImpl";


    private String threadKey;

    private  Integer coreSize;

    private  Integer maxSize;

    private  Integer keepAliveTimeInMinutes;

    private  Integer workingQueueSize;

    private String aClass;

    public ThreadPoolProperties() {
        this(new Builder());
    }

    public ThreadPoolProperties(ThreadPoolProperties.Builder builder) {
        coreSize = builder.coreSize == null ? default_coreSize : builder.coreSize;
        maxSize = builder.maxSize == null ? default_maximumSize : builder.maxSize;
        keepAliveTimeInMinutes = builder.keepAliveTimeInMinutes == null ? default_keepAliveTimeMinutes : builder.keepAliveTimeInMinutes;
        workingQueueSize = builder.workingQueueSize == null ? default_maxQueueSize : builder.workingQueueSize;
        threadKey = builder.threadKey == null ? default_threadKey: builder.threadKey;
        aClass = builder.aClass == null ? default_aClass: builder.aClass;
    }

    public String getaClass() {
//        if (StringUtils.isEmpty(aClass)){
//            aClass = default_aClass;
//        }
        return aClass;
    }

    public void setaClass(String aClass) {
        this.aClass = aClass;
    }

    public Integer getCoreSize() {
        return coreSize;
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public Integer getKeepAliveTimeInMinutes() {
        return keepAliveTimeInMinutes;
    }

    public Integer getWorkingQueueSize() {
        return workingQueueSize;
    }

    public String getThreadKey() {
        return threadKey;
    }

    public void setThreadKey(String threadKey) {
        this.threadKey = threadKey;
    }

    public void setCoreSize(Integer coreSize) {
        this.coreSize = coreSize;
    }

    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

    public void setKeepAliveTimeInMinutes(Integer keepAliveTimeInMinutes) {
        this.keepAliveTimeInMinutes = keepAliveTimeInMinutes;
    }

    public void setWorkingQueueSize(Integer workingQueueSize) {
        this.workingQueueSize = workingQueueSize;
    }

    public static class Builder {
        private Integer coreSize;

        private Integer maxSize;

        private Integer keepAliveTimeInMinutes;

        private Integer workingQueueSize;
        private String threadKey;
        private String aClass;

        public Builder() {}

        public Integer getCoreSize() {
            return coreSize;
        }

        public Integer getMaximumSize() {
            return maxSize;
        }

        public Integer getKeepAliveTimeMinutes() {
            return keepAliveTimeInMinutes;
        }

        public Integer getMaxQueueSize() {
            return workingQueueSize;
        }

        public String getThreadKey() {
            return threadKey;
        }

        public String getaClass(){
            return  aClass;
        }

        public Builder withCoreSize(int value) {
            this.coreSize = value;
            return this;
        }

        public Builder withMaximumSize(int value) {
            this.maxSize = value;
            return this;
        }

        public Builder withKeepAliveTimeMinutes(int value) {
            this.keepAliveTimeInMinutes = value;
            return this;
        }

        public Builder withMaxQueueSize(int value) {
            this.workingQueueSize = value;
            return this;
        }
        public Builder withThreadKey(String threadKey){
            this.threadKey = threadKey;
            return this;
        }

        public Builder withAClass(String aClass){
            this.aClass = aClass;
            return this;
        }

        public ThreadPoolProperties build() {
            return new ThreadPoolProperties(this);
        }
    }

}
