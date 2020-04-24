package com.wangc.base.es.client;

import com.suning.framework.dal.parsing.FreeMakerParser;
import com.suning.framework.dal.parsing.ParsingException;
import com.suning.framework.dal.parsing.XNode;
import com.suning.framework.dal.parsing.XPathParser;
import com.suning.framework.dal.parsing.xml.XmlSqlMapEntityResolver;
import com.suning.union.lib.es.ESClient;
import com.suning.union.lib.es.exception.EsException;
import com.suning.union.lib.es.exception.ExceptionMessage;
import com.suning.union.lib.es.model.CombinedQueryResult;
import com.suning.union.lib.es.model.ESDocModel;
import io.searchbox.client.JestResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataRetrievalFailureException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Deprecated
public class ESDalOld implements InitializingBean {

    private static Logger logger = LoggerFactory.getLogger(ESDalOld.class.getName());

    protected Resource[] esSqlMapConfigLocation;

    private ESClient esClient;
    private ConcurrentMap<String,String> esSqlContainer =new ConcurrentHashMap<String,String>();
    Properties variables = new Properties();

    private static ConversionService conversionService = DefaultConversionService.getSharedInstance();

    public Resource[] getEsSqlMapConfigLocation() {
        return esSqlMapConfigLocation;
    }

    public void setEsSqlMapConfigLocation(Resource[] esSqlMapConfigLocation) {
        this.esSqlMapConfigLocation = esSqlMapConfigLocation;
    }

    public ConcurrentMap<String, String> getEsSqlContainer() {
        return esSqlContainer;
    }

    public void setEsSqlContainer(ConcurrentMap<String, String> esSqlContainer) {
        this.esSqlContainer = esSqlContainer;
    }

    public ESClient getEsClient() {
        return esClient;
    }

    public void setEsClient(ESClient esClient) {
        this.esClient = esClient;
    }



    @Override
    public void afterPropertiesSet() throws Exception {
        if (esSqlMapConfigLocation != null) {
            for (Resource resource : esSqlMapConfigLocation) {
                //解析esSqlMap配置文件
                XPathParser xParser = new XPathParser(resource.getInputStream(), false, variables, new XmlSqlMapEntityResolver());
                XNode context = xParser.evalNode("/esSqlMap");
                String namespace = context.getStringAttribute("namespace", "");
                if ("".equals(namespace)) {
                    throw new ParsingException("sqlMap's namespace cannot be empty");
                }

                for (XNode statementsNode : context.evalNodes("sql|select|insert|update|delete")) {
                    StringBuilder sqlBuilder = new StringBuilder();
                    String id = statementsNode.getStringAttribute("id");
                    NodeList children = statementsNode.getNode().getChildNodes();
                    for (int i = 0; i < children.getLength(); i++) {
                        XNode child = statementsNode.newXNode(children.item(i));
                        String nodeName = child.getNode().getNodeName();
                        if (child.getNode().getNodeType() == Node.CDATA_SECTION_NODE
                                || child.getNode().getNodeType() == Node.TEXT_NODE) {
                            String data = child.getStringBody("");
                            sqlBuilder.append(data);
                        } else if (child.getNode().getNodeType() == Node.ELEMENT_NODE) {
                            throw new ParsingException("Unknown element <" + nodeName + "> in SQL statement.");
                        }
                    }
                    String sqlSource = sqlBuilder.toString();
                    sqlSource = sqlSource.trim().replace('\n', ' ');
                    if (id == null || "".equals(id)) {
                        throw new ParsingException(" element " + statementsNode.getName() +
                                "'s id cannot be empty");
                    }
                    if (sqlSource == null || "".equals(sqlSource)) {
                        throw new ParsingException(" sql sql statment['id'=" + id + "] is an empty sql.");
                    }
                    esSqlContainer.putIfAbsent(namespace+"_"+id,sqlSource);

                }
            }
        }
    }

    /**
     * 同步插入，使用rsf接口进行通信
     * @param doc
     * @return
     */
    public  boolean  syncSave(ESDocModel doc) throws IOException {
        Map<String,Object> reqMap = new HashMap<>();
        reqMap.put("index",doc.getIndex());
        reqMap.put("type",doc.getType());
        reqMap.put("pkId",doc.getPkId());
        reqMap.put("data",doc.getData());

        return esClient.insertOrUpdateDoc(doc.getData(),doc.getPkId(),doc.getIndex(),doc.getType());
    }



    /**
     * 通过sql的方式查询ES
     * @param esSqlId
     * @param param
     * @return
     * @throws Exception
     */
    public <T> CombinedQueryResult<T> searchForList(String esSqlId, Map param, Class<T> tClass) throws EsException {
        if (StringUtils.isEmpty(esSqlId)){
            throw new EsException(ExceptionMessage.ES_SQL_NOT_FOUND);
        }

        String sqlSource = esSqlContainer.get(esSqlId);
        if (StringUtils.isEmpty(sqlSource)){
            throw new EsException(ExceptionMessage.ES_SQL_NOT_FOUND);
        }

        String finalEsSql = FreeMakerParser.process(sqlSource,param);
        Map<String,Object> reqMap = new HashMap<>();
        reqMap.put("sql",finalEsSql);
        CombinedQueryResult<T> combinedQueryResult = null;
        try {
            combinedQueryResult = esClient.searchBySql(finalEsSql,tClass);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            return new CombinedQueryResult<T>();
        }
        return combinedQueryResult;

    }

    public <T> CombinedQueryResult<T> sqlQuery(String sql,Class<T> clz){
        return esClient.searchBySql(sql,clz);
    }
    public JestResult sqlQuery(String sql){
        return esClient.searchBySql(sql);
    }


    /**
     * 根据pkId对ES进行删除
     * @param id
     * @param index
     * @param type
     * @return
     */
    public boolean deleteById(String id,String index,String type){
        Map<String,Object> param = new HashMap<>();
        param.put("index",index);
        param.put("type",type);
        param.put("pkId",id);
        return esClient.deleteDocById(id,index,type);
    }

    /**
     * 根据主键获取文档
     * @param tClass 返回对象
     * @param index  待操作的库
     * @param type   待操作的表
     * @param id     待操作的主键id
     * @return
     */
    public <T> T getDocumentById(Class<T> tClass , String index, String type, String id) {
        return esClient.getDocumentById(tClass,index,type,id);
    }



    protected static String lowerCaseName(String name) {
        return name.toLowerCase(Locale.US);
    }

    protected static String underscoreName(String name) {
        if (!org.springframework.util.StringUtils.hasLength(name)) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        result.append(lowerCaseName(name.substring(0, 1)));
        for (int i = 1; i < name.length(); i++) {
            String s = name.substring(i, i + 1);
            String slc = lowerCaseName(s);
            if (!s.equals(slc)) {
                result.append("_").append(slc);
            }
            else {
                result.append(s);
            }
        }
        return result.toString();
    }


    /**
     * 根据map构造dto
     * @param data
     * @param tClass
     * @param <T>
     * @return
     */
    public static <T> T mapToClass(Map<String,Object> data,Class<T> tClass){
        PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(tClass);
        HashMap<String, PropertyDescriptor> mappedFields = new HashMap<>();
        Set<String> mappedProperties = new HashSet<>();
        for (PropertyDescriptor pd : pds) {
            if (pd.getWriteMethod() != null) {
                mappedFields.put(lowerCaseName(pd.getName()), pd);
                String underscoredName = underscoreName(pd.getName());
                if (!lowerCaseName(pd.getName()).equals(underscoredName)) {
                    mappedFields.put(underscoredName, pd);
                }
                mappedProperties.add(pd.getName());
            }
        }
        Iterator<Map.Entry<String, Object>> it = data.entrySet().iterator();

        T mappedObject = BeanUtils.instantiateClass(tClass);
        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mappedObject);
        bw.setConversionService(conversionService);
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            String column = entry.getKey();
            String field = lowerCaseName(column.replaceAll(" ", ""));
            PropertyDescriptor pd = mappedFields.get(field);
            if (pd != null) {
                try {
                    Object value = entry.getValue();
                    try {
                        bw.setPropertyValue(pd.getName(), value);
                    } catch (TypeMismatchException ex) {
                        if (value != null) {
                            throw ex;
                        }
                    }
                } catch (NotWritablePropertyException ex) {
                    throw new DataRetrievalFailureException(
                            "ES sql Unable to map column '" + column + "' to property '" + pd.getName() + "'", ex);
                }
            }
        }
        return mappedObject;
    }
}
