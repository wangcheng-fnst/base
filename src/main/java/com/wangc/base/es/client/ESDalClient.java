package com.wangc.base.es.client;


import com.wangc.base.es.exception.EsException;
import com.wangc.base.es.model.CombinedQueryResult;
import com.wangc.base.es.model.ESDocModel;

import java.io.IOException;
import java.util.Map;

/**
 * 客户端接口
 *
 * @Auth 18089751
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public interface ESDalClient {
    /**
     * 异步存数据
     *
     * @param esModel
     * @return
     * @Auth 18089751
     * @see [相关类/方法]（可选）
     * @since [产品/模块版本] （可选）
     */
    boolean syncSave(ESDocModel esModel) throws IOException;

    /**
     * 查询
     *
     * @param esSqlId
     * @param tClass
     * @param <T>
     * @return
     * @throws EsException
     * @Auth 18089751
     * @see [相关类/方法]（可选）
     * @since [产品/模块版本] （可选）
     */
    <T> CombinedQueryResult<T> searchForList(String esSqlId, Class<T> tClass) throws EsException;

    /**
     * 删除
     *
     * @param id
     * @param index
     * @param type
     * @return
     * @Auth 18089751
     * @see [相关类/方法]（可选）
     * @since [产品/模块版本] （可选）
     */
    boolean deleteById(String id, String index, String type);


    /**
     * 根据scrollId查询ES
     *
     * @param esSqlId 查询sql
     * @return 查询结果
     * @throws EsException
     */
    CombinedQueryResult<Map> searchByScrollId(String esSqlId, String scrollId) throws EsException;
}
