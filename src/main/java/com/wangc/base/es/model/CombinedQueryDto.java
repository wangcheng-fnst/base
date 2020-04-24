package com.wangc.base.es.model;

import io.searchbox.core.search.sort.Sort;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CombinedQueryDto {
    /**
     * 滚动ID
     */
    private String scrollId;

    /**
     * 滚动次数
     */
    private int scrollTimes;

    /**
     * 分页大小
     */
    private int size;

    /**
     * 从第几条数据开始
     */
    private int from;

    /**
     * 前台查询参数map
     */
    private Map<String, Object> params;

    private SearchSourceBuilder searchSourceBuilder;
    private Set<String> indices;
    private Set<String> types;
    private List<Sort>  sorts;

    public List<Sort> getSorts() {
        return sorts;
    }

    public void setSorts(List<Sort> sorts) {
        this.sorts = sorts;
    }

    public String getScrollId() {
        return scrollId;
    }

    public void setScrollId(String scrollId) {
        this.scrollId = scrollId;
    }

    public int getScrollTimes() {
        return scrollTimes;
    }

    public void setScrollTimes(int scrollTimes) {
        this.scrollTimes = scrollTimes;
    }

    public SearchSourceBuilder getSearchSourceBuilder() {
        return searchSourceBuilder;
    }

    public void setSearchSourceBuilder(SearchSourceBuilder searchSourceBuilder) {
        this.searchSourceBuilder = searchSourceBuilder;
    }

    public Set<String> getIndices() {
        return indices;
    }

    public void setIndices(Set<String> indices) {
        this.indices = indices;
    }

    public Set<String> getTypes() {
        return types;
    }

    public void setTypes(Set<String> types) {
        this.types = types;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }



}
