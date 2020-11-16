package com.wangc.base.lib.bus;

public interface EventListener {

    void listen(EventMsg eventMsg);

    String name();
}
