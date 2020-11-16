package com.wangc.base.lib.bus.guavaImpl;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.wangc.base.lib.bus.EventListener;
import com.wangc.base.lib.bus.EventMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractConcurrentGuavaEventListener implements EventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGuavaEventListener.class);

    @Override
    @Subscribe
    @AllowConcurrentEvents
    public void listen(EventMsg eventMsg) {
        process(eventMsg);
    }
    public abstract void process(EventMsg eventMsg);
}
