package com.wangc.base.es;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wangc.base.es.exception.EsException;
import com.wangc.base.es.exception.ExceptionMessage;
import com.wangc.base.es.model.CombinedQueryDto;
import com.wangc.base.es.model.CombinedQueryResult;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.*;
import io.searchbox.params.Parameters;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.threadpool.ExecutorBuilder;
import org.elasticsearch.threadpool.ThreadPool;
import org.nlpcn.es4sql.SearchDao;
import org.nlpcn.es4sql.query.QueryAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;


/**
 * 操作es集群的客户端
 *
 * @Auth:wcking
 */
public class ESClient implements InitializingBean {


    private Logger logger = LoggerFactory.getLogger(ESClient.class);
    /**
     * 成功状态码
     */
    private static final String RESPONSE_SUCCESS_CODE = "200";
    /**
     * 失败状态码
     */
    private static final String RESPONSE_FAIL_CODE = "-1";
    /**
     * 异常状态码
     */
    private static final String RESPONSE_EXCEPTION_CODE = "500";
    //ES集群地址，必须配置
    private String esServerUrls;
    private String defaultMaxTotalConnectionPerRoute;
    private String maxTotalConnection;
    private String readTimeout;

    Gson gson = new Gson();


    /**
     * 查询使用的api客户端
     */
    private JestClient jestClient;

    /**
     * 插入使用的客户端
     */
    private RestClient restClient;


    /**
     * 搜索上下文的时间,用来支持该批次，必须设置
     */
    private static final String SCROLL_ALIVE_TIME = "5m";
    /**
     * 命中集 key
     */
    private static final String QUERY_HITS_KEY = "hits";

    private static final String TOTAL_SIZE = "total";

    /**
     * 数据源 key
     */
    private static final String SOURCE_KEY = "_source";
    /**
     * 滚动ID key
     */
    private static final String SCORLL_ID_KEY = "_scroll_id";


    //for sql parse
    SearchDao searchDao;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (esServerUrls == null || "".equals(esServerUrls)) {
            Exception exception = new EsException(ExceptionMessage.ES_NOT_INIT);
            logger.error("not esServerUrls!", exception);
            throw exception;
        }
        jestClient = initJestClient();
        restClient = initClient();
        if (searchDao == null) {
            Settings settings = Settings.builder().build();
            ThreadPool threadPool = new ThreadPool(settings, new ExecutorBuilder[0]);
            Client client = new NodeClient(settings, threadPool);
            searchDao = new SearchDao(client);
        }
        logger.info("esClient init success");
    }


    private JestClient initJestClient() {
        JestClientFactory factory = new JestClientFactory();
        List<String> urls = Arrays.asList(esServerUrls.split(","));
        factory.setHttpClientConfig(new HttpClientConfig
                .Builder(urls)
                .multiThreaded(true)
                .defaultMaxTotalConnectionPerRoute(Integer.valueOf(defaultMaxTotalConnectionPerRoute))
                .maxTotalConnection(Integer.valueOf(maxTotalConnection))
                .readTimeout(Integer.valueOf(readTimeout))
                .build());
        JestClient client = factory.getObject();
        return client;
    }

    private RestClient initClient() {
        List<String> urls = Arrays.asList(esServerUrls.split(","));
        HttpHost[] httpHosts = new HttpHost[urls.size()];
        int i = 0;
        for (String url : urls) {
            HttpHost httpHost = HttpHost.create(url);
            httpHosts[i] = httpHost;
            i++;
        }
        RestClient client = RestClient.builder(httpHosts).build();
        return client;
    }

    /**
     * 指定 id 更新
     *
     * @param t
     * @param uniqueId doc id
     * @param index
     * @param type
     * @return
     * @throws IOException
     */
    public boolean updateField(Object t, String uniqueId, String index, String type) throws IOException {
        //是否更新成功标识
        boolean flag = false;
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("doc", t);
        Update.Builder builder = new Update.Builder(updateMap);
        if (!StringUtils.isEmpty(uniqueId)) {
            builder.id(uniqueId);
        }
        builder.refresh(true);
        Update updateDoc = builder.index(index).type(type).build();
        String s = updateDoc.getData(new Gson());
        HttpEntity entity = new NStringEntity(s, ContentType.APPLICATION_JSON);
        try {
            Response response = restClient.performRequest(updateDoc.getRestMethodName(),
                    updateDoc.getURI(), Collections.emptyMap(), entity);
            if (response == null) {
                flag = false;
            } else {
                int code = response.getStatusLine().getStatusCode();
                if ((code / 100) == 2) {
                    flag = true;
                }
            }
            if (!flag) {
                logger.info("error update pkId =" + uniqueId + ",reason:" + response.getStatusLine().getReasonPhrase());
            }
        } catch (IOException e) {
            logger.error("single update error pkId=" + uniqueId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("single update error pkId=" + uniqueId, e.getMessage(), e);
        }

        return flag;
    }

    //插入或者更新doc
    public boolean insertOrUpdateDoc(Object t, String uniqueId, String index, String type) throws IOException {
        //是否插入成功标识
        boolean flag = false;
        Index.Builder builder = new Index.Builder(t);
        if (!StringUtils.isEmpty(uniqueId)) {
            builder.id(uniqueId);
        }
        builder.refresh(true);
        Index indexDoc = builder.index(index).type(type).build();
        String s = indexDoc.getData(new Gson());
        HttpEntity entity = new NStringEntity(s, ContentType.APPLICATION_JSON);
        try {
            Response response = restClient.performRequest(indexDoc.getRestMethodName(),
                    indexDoc.getURI(), Collections.emptyMap(), entity);
            if (response == null) {
                flag = false;
            } else {
                int code = response.getStatusLine().getStatusCode();
                if ((code / 100) == 2) {
                    flag = true;
                }
            }
            if (!flag) {
                logger.info("error insert pkId =" + uniqueId + ",reason:" + response.getStatusLine().getReasonPhrase());
            }
        } catch (IOException e) {
            logger.error("single insert error pkId=" + uniqueId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("single insert error pkId=" + uniqueId, e.getMessage(), e);
        }

        return flag;
    }

    /**
     * 批量操作
     *
     * @param index
     * @param type
     * @param requestMapList
     * @return
     */
    public Map<String, Object> batchOperation(String index, String type, List<Map> requestMapList) {
        Map<String,Object> responseMap = new HashMap<>();
        //组装HttpEntity所需参数
        StringBuilder stringBuilder = new StringBuilder();
        for (Map map : requestMapList) {//NOSONAR
            //拼装ES Bulk Api 中的索引部分 例如:{ "create" : { "_index" : "test", "_id" : "3" } }
            String indexStr = JSONObject.toJSONString(map.get("requestTitle"));
            stringBuilder.append(indexStr + "\n");
            if (map.get("requestBody") != null) {
                //拼装ES Bulk Api 中的文档部分 例如:{ "field1" : "value1", "field2" : "value2"  }
                String data = JSONObject.toJSONString(map.get("requestBody"));
                stringBuilder.append(data + "\n");
            }
        }
        //组装完成的结果:{ "create" : { "_index" : "test", "_id" : "3" } }这里\n换行{ "field1" : "value1", "field2" : "value2"  }
        String postJson = stringBuilder.toString();

        Bulk.Builder bulkBuilder = new Bulk.Builder();
        bulkBuilder.defaultIndex(index);
        bulkBuilder.defaultType(type);
        Bulk build = bulkBuilder.refresh(true).build();

        NStringEntity entity = new NStringEntity(postJson, ContentType.APPLICATION_JSON);

        Response response;
        try {
            response = restClient.performRequest(build.getRestMethodName(), build.getURI(), Collections.emptyMap(), entity);
            if (response == null) {
                //失败
                responseMap.put("message","批量操作失败!");
                responseMap.put("code",RESPONSE_FAIL_CODE);
                logger.error("批量操作失败:index:"+index+",type:"+type+",data:"+JSONObject.toJSONString(requestMapList));
            } else {
                int code = response.getStatusLine().getStatusCode();
                if ((code / 100) == 2) {
                    //成功
                    responseMap.put("message","批量操作成功!");
                    responseMap.put("code",RESPONSE_SUCCESS_CODE);
                }else {
                    //失败
                    responseMap.put("message","批量操作失败!");
                    responseMap.put("code",RESPONSE_FAIL_CODE);
                    logger.error("批量操作失败:"+response.toString());
                }
            }
        } catch (IOException e) {
            logger.error("批量操作异常:"+e.getMessage(), e);
            responseMap.put("message","批量操作IO异常:"+e.getMessage());
            responseMap.put("code",RESPONSE_EXCEPTION_CODE);
        } catch (Exception e) {
            logger.error("批量操作异常:"+e.getMessage(), e);
            responseMap.put("message","批量操作异常:"+e.getMessage());
            responseMap.put("code",RESPONSE_EXCEPTION_CODE);
        }finally {
            //释放资源
            entity.close();
        }
        return responseMap;
    }

    public <T> CombinedQueryResult<T> queryByScroll(CombinedQueryDto combinedQueryDto, Class<T> clz) {
        return buildResponse(scrollSearch(combinedQueryDto), clz);
    }

    // < 1w 的分页
    private JestResult search(CombinedQueryDto combinedQueryDto) {
        JestResult result = null;
        if (combinedQueryDto == null){
            return result;
        }
        SearchSourceBuilder searchSourceBuilder = combinedQueryDto.getSearchSourceBuilder();
        searchSourceBuilder.size(combinedQueryDto.getSize());
        searchSourceBuilder.from(combinedQueryDto.getFrom());

        //构造查询条件,设置索引及类型
        Search.Builder builder = new Search.Builder(searchSourceBuilder.toString())
                .addIndices(combinedQueryDto.getIndices())
                .addTypes(combinedQueryDto.getTypes());
        if (searchSourceBuilder.fetchSource() != null
                && searchSourceBuilder.fetchSource().includes() != null
                && searchSourceBuilder.fetchSource().includes().length > 0) {
            for (String source : searchSourceBuilder.fetchSource().includes()) {
                builder.addSourceIncludePattern(source);
            }
        }


        Search search = builder.build();
        String s = search.getData(gson);
        HttpEntity entity = new NStringEntity(s, ContentType.APPLICATION_JSON);

        try {
            Response response = restClient.performRequest(search.getRestMethodName(),search.getURI(),Collections.emptyMap(), entity);
            if (response != null){
                StatusLine statusLine = response.getStatusLine();
                result = search.createNewElasticSearchResult(response.getEntity() == null ? null : EntityUtils.toString(response.getEntity()),
                        statusLine.getStatusCode(), statusLine.getReasonPhrase(), this.gson);
            }

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }


    /**
     * 深度分页时的可选方案：快照查询
     * 使用建议：
     * 1.对于实时分页查询建议查询条件带上pkId > xxx 的条件使用 searchBySql
     * 2.当scrollId有值时，只能查询下一页数据，对于上一页数据，需要调用方自己保存
     * 3. 本方法适合导出报表业务
     *
     * @param combinedQueryDto
     * @return
     */
    private JestResult scrollSearch(CombinedQueryDto combinedQueryDto) {
        String scrollId = combinedQueryDto.getScrollId();
        JestResult result = null;
        try {
            //首次查询或滚动时间超时,则重新查询
            if (StringUtils.isEmpty(scrollId)) {
                //构造查询条件
                SearchSourceBuilder searchSourceBuilder = combinedQueryDto.getSearchSourceBuilder();
                searchSourceBuilder.size(combinedQueryDto.getSize());

                //构造查询条件,设置索引及类型
                Search.Builder builder = new Search.Builder(searchSourceBuilder.toString())
                        .addIndices(combinedQueryDto.getIndices())
                        .addTypes(combinedQueryDto.getTypes())
                        .setParameter(Parameters.SCROLL, SCROLL_ALIVE_TIME); //超时时间统一设置

                Search search = builder.build();
                //第一次检索,拍下快照
                logger.info("search sql:" + searchSourceBuilder.toString());
                String s = search.getData(gson);
                HttpEntity entity = new NStringEntity(s, ContentType.APPLICATION_JSON);//NOSONAR
                Response response = restClient.performRequest(search.getRestMethodName(),search.getURI(),Collections.emptyMap(), entity);
                if (response != null){
                    StatusLine statusLine = response.getStatusLine();
                    result = search.createNewElasticSearchResult(response.getEntity() == null ? null : EntityUtils.toString(response.getEntity()),
                            statusLine.getStatusCode(), statusLine.getReasonPhrase(), this.gson);
                }

            } else {
                // 基于scrollId的快照查询
                SearchScroll scroll = new SearchScroll.Builder(scrollId, SCROLL_ALIVE_TIME).build();
                Response response = restClient.performRequest(scroll.getRestMethodName(),scroll.getURI(),Collections.emptyMap());
                if (response != null){
                    StatusLine statusLine = response.getStatusLine();
                    result = scroll.createNewElasticSearchResult(response.getEntity() == null ? null : EntityUtils.toString(response.getEntity()),
                            statusLine.getStatusCode(), statusLine.getReasonPhrase(), this.gson);
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }


    /**
     * 清除滚动ID
     */
    public void clearScrollIds(String scrollId) {
        ClearScroll clearScroll = new ClearScroll.Builder().addScrollId(scrollId)
                .build();

        try {
            jestClient.execute(clearScroll);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public <T> CombinedQueryResult<T> buildResponse(JestResult result, Class<T> clz) {

        CombinedQueryResult res = new CombinedQueryResult();
        if (result == null) {
            res.setMsg("result is null");
            return res;
        }
        if (!result.isSucceeded()) {
            res.setMsg(result.getErrorMessage());
            return res;
        }
        JsonObject jsonObject = result.getJsonObject();
        int total = jsonObject.getAsJsonObject(QUERY_HITS_KEY).get(TOTAL_SIZE).getAsInt();
        res.setTotal(total);
        if (jsonObject.has(SCORLL_ID_KEY)) {
            res.setScrollId(jsonObject.getAsJsonPrimitive(SCORLL_ID_KEY).getAsString());
        }
        JsonArray jsonElements = jsonObject.getAsJsonObject(QUERY_HITS_KEY).getAsJsonArray(QUERY_HITS_KEY);
        List<T> list = new ArrayList<>();
        for (JsonElement jsonElement : jsonElements) {
            if (String.class.equals(clz)) {
                list.add((T) gson.toJson(jsonElement.getAsJsonObject().get(SOURCE_KEY)));
            } else {
                list.add(gson.fromJson(
                        gson.toJson(jsonElement.getAsJsonObject().get(SOURCE_KEY)),
                        clz));
            }
        }
        //不为空,才算文档查询成功
        if (!CollectionUtils.isEmpty(list)) {
            res.setResList(list);
        }
        return res;
    }


    @Deprecated
    public CombinedQueryResult<String> buildResponse(JestResult result) {

        CombinedQueryResult res = new CombinedQueryResult();
        if (result == null) {
            res.setMsg("query result is null");
            return res;
        }
        List<Map> list = new ArrayList<>();
        if (!result.isSucceeded()) {
            //返回错误消息信息
            res.setMsg(result.getErrorMessage());
            res.setTotal(0);
            res.setResList(list);
            return res;
        }
        JsonObject jsonObject = result.getJsonObject();
        int total = jsonObject.getAsJsonObject(QUERY_HITS_KEY).get(TOTAL_SIZE).getAsInt();
        res.setTotal(total);
        JsonArray jsonElements = jsonObject.getAsJsonObject(QUERY_HITS_KEY).getAsJsonArray(QUERY_HITS_KEY);

        for (JsonElement jsonElement : jsonElements) {

            list.add((Map) gson.fromJson(jsonElement, Map.class)
                    .get(SOURCE_KEY));
        }
        if (jsonObject.has(SCORLL_ID_KEY)) {
            String scrollId = jsonObject.getAsJsonPrimitive(SCORLL_ID_KEY).getAsString();
            res.setScrollId(scrollId);
        }
        //不为空,才算文档查询成功
        if (!CollectionUtils.isEmpty(list)) {
            res.setResList(list);
        }
        return res;
    }


    private static final String LIMIT_STR = "limit";


    public <T> CombinedQueryResult<T> searchBySql(String sql, Class<T> tClass) {
        return buildResponse(doSqlSearch(sql), tClass);
    }

    public JestResult searchBySql(String sql) {
        return doSqlSearch(sql);
    }

    private JestResult doSqlSearch(String sql) {
        CombinedQueryDto combinedQueryDto = parseSqlToSearch(sql);
        return search(combinedQueryDto);
    }

    /**
     * 根据主键删除
     *
     * @param uniqueId
     * @param index
     * @param type
     * @return
     */
    public boolean deleteDocById(String uniqueId, String index, String type) {
        boolean flag = false;
        Delete delete = new Delete.Builder(uniqueId)
                .index(index)
                .type(type)
                .build();
        try {
            JestResult result = jestClient.execute(delete);
            if (result.getResponseCode() / 100 == 2) {
                flag = true;
            }
            if (!flag) {
                logger.error("delete error _id=" + uniqueId + ",reason:" + result.getErrorMessage());
            }
        } catch (IOException e) {
            logger.error("delete error _id=" + uniqueId + ",reason:" + e.getMessage(), e);
            flag = false;
        }

        return flag;
    }


    /**
     * 根据主键获取文档
     *
     * @param clz   返回对象
     * @param index 待操作的库
     * @param type  待操作的表
     * @param id    待操作的主键id
     * @return
     */
    public <T> T getDocumentById(Class<T> clz, String index, String type, String id) {
        Get get = new Get.Builder(index, id).type(type).build();
        JestResult result = null;
        T o = null;
        try {
            result = jestClient.execute(get);
            o = result.getSourceAsObject(clz);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return o;
    }

    public String getEsServerUrls() {
        return esServerUrls;
    }

    public void setEsServerUrls(String esServerUrls) {
        this.esServerUrls = esServerUrls;
    }

    public String getDefaultMaxTotalConnectionPerRoute() {
        return defaultMaxTotalConnectionPerRoute;
    }

    public void setDefaultMaxTotalConnectionPerRoute(String defaultMaxTotalConnectionPerRoute) {
        this.defaultMaxTotalConnectionPerRoute = defaultMaxTotalConnectionPerRoute;
    }

    public String getMaxTotalConnection() {
        return maxTotalConnection;
    }

    public void setMaxTotalConnection(String maxTotalConnection) {
        this.maxTotalConnection = maxTotalConnection;
    }

    public String getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(String readTimeout) {
        this.readTimeout = readTimeout;
    }

    //offset 必须是size的整数倍，否则数据会有重叠
    public CombinedQueryDto parseSqlToSearch(String sql) {
        CombinedQueryDto combinedQueryDto = new CombinedQueryDto();
        try {
            if (sql.contains(LIMIT_STR)) {
                String[] ranges = sql.split("limit")[1].trim().split(",");
                if (ranges.length == 2) {
                    int offset = Integer.parseInt(ranges[0]);
                    int size = Integer.parseInt(ranges[1]);
                    int scrollTimes = offset / size;
                    combinedQueryDto.setFrom(offset);
                    combinedQueryDto.setSize(size);
                    combinedQueryDto.setScrollTimes(scrollTimes);

                } else if (ranges.length == 1) {
                    int size = Integer.parseInt(ranges[0]);
                    combinedQueryDto.setSize(size);
                    combinedQueryDto.setScrollTimes(0);
                    combinedQueryDto.setFrom(0);
                }
                sql = sql.split("limit")[0];
            } else {
                combinedQueryDto.setFrom(0);
                combinedQueryDto.setSize(20);
            }
            int total = combinedQueryDto.getFrom() + combinedQueryDto.getSize();
            if (total > 10000) {
                throw new EsException(ExceptionMessage.ES_TOO_DEEP_PAGE);
            }
            QueryAction queryAction = searchDao.explain(sql);
            //for scroll query
            SearchRequest searchRequest = (SearchRequest) queryAction.explain().request();
            combinedQueryDto.setSearchSourceBuilder(searchRequest.source());
            String[] indices = searchRequest.indices();
            String[] types = searchRequest.types();
            Set<String> indexSet = new HashSet<>();
            Set<String> typeSet = new HashSet<>();
            indexSet.addAll(Arrays.asList(indices));
            typeSet.addAll(Arrays.asList(types));
            combinedQueryDto.setIndices(indexSet);
            combinedQueryDto.setTypes(typeSet);
        } catch (Exception e) {
            logger.error("parseSqlToSearch 执行异常", e);
            combinedQueryDto = null;
        }
        return combinedQueryDto;
    }

}
