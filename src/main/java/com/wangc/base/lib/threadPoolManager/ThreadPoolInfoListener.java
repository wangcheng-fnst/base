package com.wangc.base.lib.threadPoolManager;

import java.util.Map;

public interface ThreadPoolInfoListener {

    void exposeInfo(Map<String, String> poolInfo, ThreadPoolProperties poolProperties);
}
