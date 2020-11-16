package com.wangc.base.lib.bus;

import java.util.concurrent.Executor;

public interface EventBusPlus {

    /**
     * 注册同步事件处理器
     * @param listener
     * @return
     */
    <T > boolean register(EventListener listener);

    /**
     * 注册异步事件处理器
     * @param listener
     * @return
     */
    boolean registerAsync(EventListener listener, Executor executor);


    /**
     * 发送事件
     * @param eventMsg
     */
    void postEvent(EventMsg eventMsg);



}
