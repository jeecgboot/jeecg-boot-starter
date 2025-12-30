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
    
    /**
     * 是否启用Redis消息监听器（pub/sub）
     * 某些云Redis服务或受限环境不支持SUBSCRIBE命令，此时应设置为false
     * 默认值：true
     */
    private boolean listenerEnabled = true;
    
    public Map<String, Long> getCacheTtls() { return cacheTtls; }
    public void setCacheTtls(Map<String, Long> cacheTtls) { this.cacheTtls = cacheTtls; }
    public boolean isListenerEnabled() { return listenerEnabled; }
    public void setListenerEnabled(boolean listenerEnabled) { this.listenerEnabled = listenerEnabled; }
}