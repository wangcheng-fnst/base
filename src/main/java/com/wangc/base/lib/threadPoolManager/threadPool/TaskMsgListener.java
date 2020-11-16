package com.wangc.base.lib.threadPoolManager.threadPool;

import com.wcking.base.threadPoolManager.TaskMsg;

public interface TaskMsgListener {

    void recordTaskMsg(TaskMsg taskMsg);
}
