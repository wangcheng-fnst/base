package com.wangc.base.lib.bus;

import java.io.Serializable;

public class EventMsg<T> implements Serializable {

    /**
     * 是否是同步事件
     * true : 同步事件
     * false： 异步事件
     */
    boolean syncFlag = true;

    /**
     *  事件类型
     */
    String type;

    /**
     * 事件数据
     */
    T data;

    Class aClass;

    public EventMsg(String type, T data) {
        this.type = type;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isSyncFlag() {
        return syncFlag;
    }

    public void setSyncFlag(boolean syncFlag) {
        this.syncFlag = syncFlag;
    }

    public Class getaClass() {
        return aClass;
    }

    public void setaClass(Class aClass) {
        this.aClass = aClass;
    }
}
