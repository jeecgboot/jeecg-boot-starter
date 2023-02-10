package org.jeecg.common.modules.redis.po;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
@Data
public class JeecgCache {
    private Map<String,Integer> cache= new HashMap<>();
    public void put(String key,Integer expire){
        cache.put(key,expire);
    }
}
