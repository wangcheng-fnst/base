package com.wangc.base.es.client;


import com.wangc.base.es.SqlHelper;
import com.wangc.base.es.exception.EsException;
import com.wangc.base.es.exception.ExceptionMessage;
import com.wangc.base.es.model.CombinedQueryResult;
import com.wangc.base.es.model.ESDocModel;
import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.builder.xml.XMLMapperEntityResolver;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.parsing.ParsingException;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  查询ES客户端
 *  使用方法如下（使用xml管理bean的方式，注解方式参照即可）
 *   1. 配置ESSqlClient
 *      如果是1.8 版本
 *        需要先配置ESClient
 *        <bean id="esClient" class="com.suning.union.lib.es.ESClient">
 * 		    <property name="esServerUrls" value="es地址"></property>
 * 		    <property name="defaultMaxTotalConnectionPerRoute" value="每个节点最大连接数"></property>
 * 		    <property name="maxTotalConnection" value="总的最大连接数"></property>
 * 		    <property name="readTimeout" value="超时时间"></property>
 * 	     </bean>
 * 	     然后配置
 *       <bean id="esSqlClient" class="com.suning.union.lib.es.client.impl.EsDalClientDirectImpl">
 * 		    <property name="esClient" ref="esClient"></property>
 * 	     </bean>
 *
 *
 *
 *   2. 配置 DefaultESSqlClient
 *          <property name="esSqlMapConfigLocation">
 *             <list>
 *                 //存放es sql的xml路径
 *                 <value>classpath*:conf/esSqlMap/esSqlMap_*.xml</value>
 *             </list>
 *         </property>
 *         <property name="esDalClient" ref="esDalClient"/>
 *
 *
 *   3. 在需要使用es操作的地方进行注入即可
 *
 */
public class ESSqlDao implements InitializingBean {

    private static Logger logger = LoggerFactory.getLogger(ESSqlDao.class.getName());

    protected Resource[] esSqlMapConfigLocation;

    private ESSqlClient ESSQLClient;
    private ConcurrentHashMap<String,String> esSqlContainer =new ConcurrentHashMap<String,String>();
    Properties variables = new Properties();
    Configuration configuration = new Configuration();
    SqlSourceBuilder sqlSourceBuilder = new SqlSourceBuilder(configuration);

    private static ConversionService conversionService = DefaultConversionService.getSharedInstance();

    public Resource[] getEsSqlMapConfigLocation() {
        return esSqlMapConfigLocation;
    }

    public void setEsSqlMapConfigLocation(Resource[] esSqlMapConfigLocation) {
        this.esSqlMapConfigLocation = esSqlMapConfigLocation;
    }

    public ConcurrentHashMap<String, String> getEsSqlContainer() {
        return esSqlContainer;
    }

    public void setEsSqlContainer(ConcurrentHashMap<String, String> esSqlContainer) {
        this.esSqlContainer = esSqlContainer;
    }



    public ESSqlClient getESSQLClient() {
        return ESSQLClient;
    }

    public void setESSQLClient(ESSqlClient ESSQLClient) {
        this.ESSQLClient = ESSQLClient;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (esSqlMapConfigLocation != null) {
            for (Resource resource : esSqlMapConfigLocation) {
                //解析esSqlMap配置文件
                XPathParser xParser = new XPathParser(resource.getInputStream(), false, variables, new XMLMapperEntityResolver());
                XNode context = xParser.evalNode("/mapper");
                String namespace = context.getStringAttribute("namespace", "");
                if ("".equals(namespace)) {
                    throw new ParsingException("sqlMap's namespace cannot be empty",new Exception());
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
     * 同步插入
     * @param doc
     * @return
     */
    public  boolean  syncSave(ESDocModel doc) throws IOException {
        return ESSQLClient.syncSave(doc);
    }


    /**
     * 通过scrollId的方式查询ES
     * @param esSqlId
     * @param param
     * @return
     * @throws Exception
     */
    public <T> CombinedQueryResult<T> searchForList(String esSqlId, Map<String,Object> param, Class<T> tClass) throws EsException {
        if (StringUtils.isEmpty(esSqlId)){
            throw new EsException(ExceptionMessage.ES_SQL_NOT_FOUND);
        }

        String sqlSource = esSqlContainer.get(esSqlId);
        if (StringUtils.isEmpty(sqlSource)){
            throw new EsException(ExceptionMessage.ES_SQL_NOT_FOUND);
        }
        BoundSql boundSql = sqlSourceBuilder
                .parse(sqlSource,Map.class,param)
                .getBoundSql(param);
        param.forEach((k,v)->{
            boundSql.setAdditionalParameter(k,v);
        });
        String finalEsSql = SqlHelper.getBoundSql(boundSql,param,configuration);
        Map<String,Object> reqMap = new HashMap<>();
        reqMap.put("sql",finalEsSql);
        CombinedQueryResult<T> combinedQueryResult = new CombinedQueryResult<>();
        try {
            CombinedQueryResult<Map> result = ESSQLClient.searchForList(finalEsSql,Map.class);
            List<T> tList = new ArrayList<>();
            if (!CollectionUtils.isEmpty(result.getResList())) {
                for (Map map : result.getResList()) {
                    tList.add(mapToClass((Map<String, Object>) map, tClass));
                }
            }
            combinedQueryResult.setMsg(result.getMsg());
            combinedQueryResult.setResList(tList);
            combinedQueryResult.setScrollId(result.getScrollId());
            combinedQueryResult.setTotal(result.getTotal());
            return combinedQueryResult;
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            combinedQueryResult.setMsg(e.getMessage());
        }
        return combinedQueryResult;
    }

    /**
     * 通过scrollId的方式查询ES
     * @param esSqlId
     * @param param
     * @return
     * @throws Exception
     */
    public <T> CombinedQueryResult<T> searchByScrollId(String esSqlId, Map<String,Object> param ,String scrollId,Class<T> tClass) throws EsException {
        if (StringUtils.isEmpty(esSqlId)){
            throw new EsException(ExceptionMessage.ES_SQL_NOT_FOUND);
        }

        String sqlSource = esSqlContainer.get(esSqlId);
        if (StringUtils.isEmpty(sqlSource)){
            throw new EsException(ExceptionMessage.ES_SQL_NOT_FOUND);
        }

        BoundSql boundSql = sqlSourceBuilder
                .parse(sqlSource,Map.class,param)
                .getBoundSql(param);
        param.forEach((k,v)->{
            boundSql.setAdditionalParameter(k,v);
        });
        String finalEsSql = SqlHelper.getBoundSql(boundSql,param,configuration);

        CombinedQueryResult<T> combinedQueryResult = new CombinedQueryResult<>();
        try {
            CombinedQueryResult<Map> result = ESSQLClient.searchByScrollId(finalEsSql,scrollId);
            List<T> tList = new ArrayList<>();
            if (!CollectionUtils.isEmpty(result.getResList())) {
                for (Map map : result.getResList()) {
                    tList.add(mapToClass((Map<String, Object>) map,tClass));
                }
            }
            combinedQueryResult.setMsg(result.getMsg());
            combinedQueryResult.setResList(tList);
            combinedQueryResult.setScrollId(result.getScrollId());
            combinedQueryResult.setTotal(result.getTotal());
            return combinedQueryResult;
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            combinedQueryResult.setMsg(e.getMessage());
        }
        return combinedQueryResult;
    }

    /**
     * 根据pkId对ES进行删除
     * @param id
     * @param index
     * @param type
     * @return
     */
    public boolean deleteById(String id,String index,String type){
        return ESSQLClient.deleteById(id,index,type);
    }


    protected static String lowerCaseName(String name) {
        return name.toLowerCase(Locale.US);
    }

    protected static String underscoreName(String name) {
        if (!StringUtils.hasLength(name)) {
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
    public static <T> T mapToClass(Map<String,Object> data,Class<T> tClass) throws EsException {
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
                    throw new EsException( "ES sql Unable to map column '" + column + "' to property '" + pd.getName() + "\'");
//                    throw new Exception(
//                            "ES sql Unable to map column '" + column + "' to property '" + pd.getName() + "'");
                }
            }
        }
        return mappedObject;
    }
}
