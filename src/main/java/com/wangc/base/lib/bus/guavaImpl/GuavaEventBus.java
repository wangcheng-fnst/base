package com.wangc.base.lib.bus.guavaImpl;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.wangc.base.lib.bus.EventBusPlus;
import com.wangc.base.lib.bus.EventListener;
import com.wangc.base.lib.bus.EventMsg;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.Executor;

public class GuavaEventBus implements EventBusPlus, InitializingBean {


    private EventBus bus ;

    private AsyncEventBus asyncBus ;
    private  Executor executor;

    public Executor getExecutor() {
        return executor;
    }

    public GuavaEventBus() {
    }

    public GuavaEventBus(Executor executor) {
        this.executor = executor;
        init();
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public void init(){
        bus = new EventBus("syncBus");
        asyncBus = new AsyncEventBus("asyncBus",executor);
    }

    @Override
    public <T> boolean register(EventListener listener) {
        bus.register(listener);
        return true;
    }

    @Override
    public boolean registerAsync(EventListener listener, Executor executor) {
        asyncBus.register(listener);
        return true;
    }

    @Override
    public void postEvent(EventMsg eventMsg) {
        if (eventMsg.isSyncFlag()) {
            bus.post(eventMsg);
        }else {
            asyncBus.post(eventMsg);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }
}
