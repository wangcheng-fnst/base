package com.wangc.base.es;

import com.wangc.base.es.client.ESSqlDao;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;

import java.util.HashMap;
import java.util.Map;

public class TestClass {

    public static void main(String[] args) throws Exception {
        ESSqlDao esSqlDao = new ESSqlDao();
        esSqlDao.setEsSqlMapConfigLocation(new Resource[]{ new FileUrlResource(TestClass.class.getResource("/esSqlMap_contract.xml"))});
        esSqlDao.afterPropertiesSet();
        Map<String,Object> param = new HashMap<>();
        param.put("status",1);
        param.put("offset",1);
        param.put("size",1);

        esSqlDao.searchForList("esContractSql_findByPage",param,Object.class);
    }
}
