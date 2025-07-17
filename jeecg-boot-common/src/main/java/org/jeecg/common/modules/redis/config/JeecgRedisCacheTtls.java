package org.jeecg.common.modules.redis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "jeecg.redis")
public class JeecgRedisCacheTtls {
    /**
     * 自定义缓存配置
     * key: 缓存名称
     * value: 缓存过期时间，单位秒
     */
    private Map<String, Long> cacheTtls = new HashMap<>();
    public Map<String, Long> getCacheTtls() { return cacheTtls; }
    public void setCacheTtls(Map<String, Long> cacheTtls) { this.cacheTtls = cacheTtls; }
}