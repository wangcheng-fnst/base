package com.wangc.base.lib.threadPoolManager;


public class ThreadLocalUtil {

    private static ThreadLocal<TaskMsg> createContextThreadLocal = new ThreadLocal<>();

    public static void set(TaskMsg msg){
        createContextThreadLocal.set(msg);
    }

    public static TaskMsg get(){
        return createContextThreadLocal.get();
    }

    public static void remove(){
        createContextThreadLocal.remove();
    }
}