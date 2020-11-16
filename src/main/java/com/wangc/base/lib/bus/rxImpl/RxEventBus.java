package com.wangc.base.lib.bus.rxImpl;

import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import com.wangc.base.lib.bus.EventBusPlus;
import com.wangc.base.lib.bus.EventListener;
import com.wangc.base.lib.bus.EventMsg;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

public class RxEventBus implements EventBusPlus {
    private static final Logger LOGGER = LoggerFactory.getLogger(RxEventBus.class);

    private final Relay<Object> mBus;

    private RxEventBus() {
        // toSerialized method made bus thread safe
        mBus = PublishRelay.create().toSerialized();
    }

    public static RxEventBus get() {
        return Holder.BUS;
    }

    public void post(Object obj) {
        mBus.accept(obj);
    }

    public <T > Observable<T> toObservable(Class<T> tClass) {
        return mBus.ofType(tClass);
    }

    public Observable<Object> toObservable() {
        return mBus;
    }

    public boolean hasObservers() {
        return mBus.hasObservers();
    }

    @Override
    public <T> boolean register(EventListener listener) {
        toObservable(EventMsg.class).subscribe(new Consumer<EventMsg>() {
            @Override
            public void accept(EventMsg eventMsg) throws Exception {
                try {
                    if (listener.name().equals(eventMsg.getType())){
                        listener.listen(eventMsg);
                    }
                } catch (Exception e) {
                    LOGGER.error("handler event msg error! ",e);
                }
            }
        });
        return true;
    }

    @Override
    public boolean registerAsync(EventListener listener, Executor executor) {
        toObservable(EventMsg.class)
                .observeOn(Schedulers.from(executor))
                .subscribe((eventMsg) ->{
                    try {
                        if (listener.name().equals(eventMsg.getType())){
                            listener.listen(eventMsg);
                        }
                    } catch (Exception e) {
                        LOGGER.error("handler event msg error! ",e);
                    }
                });
        return true;
    }

    @Override
    public void postEvent(EventMsg eventMsg) {
        this.post(eventMsg);
    }


    private static class Holder {
        private static final RxEventBus BUS = new RxEventBus();
    }
}
