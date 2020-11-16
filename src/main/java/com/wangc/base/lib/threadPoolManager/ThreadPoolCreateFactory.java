package com.wangc.base.lib.threadPoolManager;

import com.wcking.base.threadPoolManager.exception.ThreadPoolException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ThreadPoolCreateFactory {

    public static ThreadPoolWrapper getInstance(ThreadPoolProperties poolProperties) throws ThreadPoolException {
        try {
            Class clazz = Class.forName(poolProperties.getaClass());
            Constructor constructor = clazz.getConstructor(ThreadPoolProperties.class);
            ThreadPoolWrapper poolWrapper = (ThreadPoolWrapper) constructor.newInstance(poolProperties);
            return poolWrapper;
        } catch (ClassNotFoundException e) {
            throw new ThreadPoolException("创建不存在的线程池类型，class=" + poolProperties.getaClass());
        } catch (NoSuchMethodException e) {
            throw new ThreadPoolException("自定义ThreadPoolWrapper需要正确的构造方法，class=" + poolProperties.getaClass());
        } catch (IllegalAccessException e) {
            throw new ThreadPoolException("自定义ThreadPoolWrapper需要正确的构造方法，class=" + poolProperties.getaClass());
        } catch (InstantiationException e) {
            throw new ThreadPoolException("自定义ThreadPoolWrapper需要正确的构造方法，class=" + poolProperties.getaClass());
        } catch (InvocationTargetException e) {
            throw new ThreadPoolException("自定义ThreadPoolWrapper需要正确的构造方法，class=" + poolProperties.getaClass());
        }
    }
}
