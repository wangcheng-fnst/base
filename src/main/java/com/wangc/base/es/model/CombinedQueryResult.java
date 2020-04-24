package com.wangc.base.es.model;

import java.io.Serializable;
import java.util.List;

public class CombinedQueryResult<T> implements Serializable {

    /**
     * 符合查询条件的总个数
     */
    private int total;
    /**
     * 当前批次结果
     */
    List<T> resList;
    /**
     * 用于scroll查询
     */
    String scrollId;

    /**
     *  ES请求返回的错误日志
     */
    private String msg;

    public List<T> getResList() {
        return resList;
    }

    public void setResList(List<T> resList) {
        this.resList = resList;
    }

    public String getScrollId() {
        return scrollId;
    }

    public void setScrollId(String scrollId) {
        this.scrollId = scrollId;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
