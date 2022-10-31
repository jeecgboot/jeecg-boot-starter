package org.jeecg.config.mongodb.common;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;

/**
 * 参考：https://blog.csdn.net/qq_38736877/article/details/121612871
 */
@Repository
public class CommonMongoDataImpl {

    @Resource
    private MongoTemplate template;

    public List getData(int pageNum, int pageSize, String tableName, HashMap<String, String> conditions) {
        //创建查询对象
        Query query = new Query();
        //设置起始数
        query.skip((pageNum - 1) * pageSize);
        //设置查询条数
        query.limit(pageSize);
        if (conditions != null) {
            for (String field : conditions.keySet()) {
                query.addCriteria(new Criteria(field).is(conditions.get(field)));
            }
        }
        return template.find(query, Object.class, tableName);
    }
}
