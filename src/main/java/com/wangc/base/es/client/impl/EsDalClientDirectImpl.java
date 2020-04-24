package com.wangc.base.es.client.impl;



import com.wangc.base.es.ESClient;
import com.wangc.base.es.client.ESDalClient;
import com.wangc.base.es.exception.EsException;
import com.wangc.base.es.model.CombinedQueryDto;
import com.wangc.base.es.model.CombinedQueryResult;
import com.wangc.base.es.model.ESDocModel;

import java.io.IOException;
import java.util.Map;

/**
 *  直接连接ES集群，需要jdk>1.8
 *
 * @Auth wcking
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class EsDalClientDirectImpl implements ESDalClient {

    private ESClient esClient;

    public ESClient getEsClient() {
        return esClient;
    }

    public void setEsClient(ESClient esClient) {
        this.esClient = esClient;
    }


    /**
     *  异步保存<br>
     *
     * @Auth wcking
     * @see [相关类/方法]（可选）
     * @since [产品/模块版本] （可选）
     */
    @Override
    public boolean syncSave(ESDocModel esModel) throws IOException {
        return esClient.insertOrUpdateDoc(esModel.getData(),esModel.getPkId(),esModel.getIndex(),esModel.getType());
    }

    /**
     * 查询 <br>
     *
     * @author 18089751
     * @see [相关类/方法]（可选）
     * @since [产品/模块版本] （可选）
     */
    @Override
    public <T> CombinedQueryResult<T> searchForList(String esSql, Class<T> tClass) throws EsException {
        return esClient.searchBySql(esSql,tClass);
    }

    /**
     * 根据id删除 <br>
     *
     * @author 18089751
     * @see [相关类/方法]（可选）
     * @since [产品/模块版本] （可选）
     */
    @Override
    public boolean deleteById(String id, String index, String type) {
        return esClient.deleteDocById(id,index,type);
    }

    /**
     * 根据scrollId查询
     *
     * @param esSqlId
     * @param scrollId
     * @return
     * @throws EsException
     */
    @Override
    public  CombinedQueryResult<Map> searchByScrollId(String esSqlId, String scrollId) {
        CombinedQueryDto combinedQueryDto = esClient.parseSqlToSearch(esSqlId);
        if (combinedQueryDto == null){
            CombinedQueryResult combinedQueryResult = new CombinedQueryResult();
            combinedQueryResult.setMsg("SQL parse Exception,Check your Sql!");
            return combinedQueryResult;
        }
        combinedQueryDto.setScrollId(scrollId);
        return esClient.queryByScroll(combinedQueryDto, Map.class);
    }

}
