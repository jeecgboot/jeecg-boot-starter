package org.jeecg.boot.starter.seata;

import feign.Client;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author eightmonth
 * @date 2023/10/16 17:35
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Client.class)
public class SeataFeignClientAutoConfiguration {
    @Bean
    public SeataFeignRequestInterceptor seataFeignRequestInterceptor() {
        return new SeataFeignRequestInterceptor();
    }
}
