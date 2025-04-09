package org.jeecg.boot.starter.lock.test;

import org.jeecg.boot.starter.lock.core.RedissonManager;
import org.jeecg.boot.starter.lock.prop.RedissonProperties;
import org.redisson.api.RedissonClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication(scanBasePackages = "org.jeecg")
@EnableAspectJAutoProxy
public class LockTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(LockTestApplication.class, args);
    }

    @Bean
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient(RedissonProperties redissonProperties) {
        RedissonManager redissonManager = new RedissonManager(redissonProperties);
        return redissonManager.getRedisson();
    }

}