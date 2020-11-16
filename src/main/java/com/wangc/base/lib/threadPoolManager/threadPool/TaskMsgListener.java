package com.wangc.base.lib.threadPoolManager.threadPool;


import com.wangc.base.lib.threadPoolManager.TaskMsg;

public interface TaskMsgListener {

    void recordTaskMsg(TaskMsg taskMsg);
}
