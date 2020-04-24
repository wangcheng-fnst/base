package com.wangc.base.es.model;

import java.io.Serializable;

/**
 * ES 存储实体
 *
 * @author 18089751
 */
public class ESDocModel<T> implements Serializable {

    private static final long serialVersionUID = -3610025957433734054L;
    /**
     * ES 索引
     */
    String index;
    /**
     * ES 类型
     */
    String type;

    /**
     * ES 主键
     */
    String pkId;
    /**
     * 数据
     */
    T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPkId() {
        return pkId;
    }

    public void setPkId(String pkId) {
        this.pkId = pkId;
    }
}
