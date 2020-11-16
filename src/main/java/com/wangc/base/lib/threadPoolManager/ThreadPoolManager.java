package com.wangc.base.lib.threadPoolManager;

import com.alibaba.fastjson.JSONArray;
import com.wcking.base.threadPoolManager.exception.ThreadPoolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolManager implements InitializingBean {

    public static final Logger LOGGER = LoggerFactory.getLogger(ThreadPoolManager.class);

    private ConcurrentHashMap<String, ThreadPoolWrapper> keyThreadPoolMap = new ConcurrentHashMap<>();

    private List<ThreadPoolProperties> threadPoolPropertiesList;
    private ThreadPoolInfoListener poolInfoListener;
    private long delayTime = 30 * 1000L;// 监控上报时间

    public List<ThreadPoolProperties> getThreadPoolPropertiesList() {
        return threadPoolPropertiesList;
    }

    public void setThreadPoolPropertiesList(List<ThreadPoolProperties> threadPoolPropertiesList) {
        this.threadPoolPropertiesList = threadPoolPropertiesList;
    }

    public ThreadPoolInfoListener getPoolInfoListener() {
        return poolInfoListener;
    }

    public void setPoolInfoListener(ThreadPoolInfoListener poolInfoListener) {
        this.poolInfoListener = poolInfoListener;
    }

    public long getDelayTime() {
        return delayTime;
    }

    public void setDelayTime(long delayTime) {
        this.delayTime = delayTime;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
        startMonitor();
    }

    public void init() {
        if (threadPoolPropertiesList != null && threadPoolPropertiesList.size() > 0) {
            threadPoolPropertiesList.forEach(threadPoolProperties -> {
                keyThreadPoolMap.putIfAbsent(threadPoolProperties.getThreadKey(), ThreadPoolCreateFactory.getInstance(threadPoolProperties));
            });
        }
    }

    private void startMonitor() {
        ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(5);
        keyThreadPoolMap.forEach((key, pool) -> {
            scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    Map<String, String> poolInfo = new HashMap<>();
                    try {
                        poolInfo.put("ip", InetAddress.getLocalHost().getHostAddress());
                    } catch (UnknownHostException e) {
                        LOGGER.error("获取ip信息失败！",e.getLocalizedMessage());
                    }
                    poolInfo.put("ActiveCount", pool.getMetrics().getCurrentActiveCount() + "");
                    poolInfo.put("CompletedTaskCount", pool.getMetrics().getCurrentCompletedTaskCount() + "");
                    poolInfo.put("LargestPoolSize", pool.getMetrics().getCurrentLargestPoolSize() + "");
                    poolInfo.put("QueueSize", pool.getMetrics().getCurrentQueueSize() + "");
                    poolInfo.put("TaskCount", pool.getMetrics().getCurrentTaskCount() + "");
                    poolInfo.put("RejectionCount", pool.getMetrics().getRejectionCount() + "");
                    poolInfo.put("CostTimes",JSONArray.toJSONString(pool.getMetrics().getCostTimes()));
                    poolInfo.put("key", key);
                    ThreadPoolInfoListener threadPoolInfoListener = getPoolInfoListener() != null ? getPoolInfoListener() : new ThreadPoolInfoListener() {
                        @Override
                        public void exposeInfo(Map<String, String> poolInfo, ThreadPoolProperties poolProperties) {
                            System.out.println(poolInfo);
                        }
                    };
                    threadPoolInfoListener.exposeInfo(poolInfo,pool.getThreadPoolProperties());
                }
            },0,delayTime, TimeUnit.MILLISECONDS);
        });


    }

    public ThreadPoolWrapper getThreadPool(String key) throws ThreadPoolException {
        ThreadPoolWrapper threadPool = keyThreadPoolMap.get(key);
        if (threadPool == null) {
            throw new ThreadPoolException("申请没有配置的线程池！ key:" + key);
        }
        return threadPool;
    }

    public void updateThreadPoolProperties(ThreadPoolProperties threadPoolProperties){
        if (threadPoolProperties != null){
            String key = threadPoolProperties.getThreadKey();
            getThreadPool(key).updatePoolProperties(threadPoolProperties);
        }
    }
}
