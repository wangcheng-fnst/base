package com.wangc.base.lib.bus.guavaImpl;

import com.google.common.eventbus.Subscribe;
import com.wangc.base.lib.bus.EventListener;
import com.wangc.base.lib.bus.EventMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGuavaEventListener implements EventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGuavaEventListener.class);

    @Override
    @Subscribe
    public void listen(EventMsg eventMsg) {
        try {
            process(eventMsg);
        }catch (Exception e){
            LOGGER.error("process sync event error!",e);
        }

    }


    public abstract void process(EventMsg eventMsg);
}
