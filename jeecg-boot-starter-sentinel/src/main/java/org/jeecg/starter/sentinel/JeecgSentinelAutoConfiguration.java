package org.jeecg.starter.sentinel;

import com.alibaba.cloud.sentinel.feign.SentinelFeignAutoConfiguration;
import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.RequestOriginParser;
import feign.Feign;
import org.jeecg.starter.sentinel.feign.JeecgSentinelFeign;
import org.jeecg.starter.sentinel.handler.JeecgUrlBlockHandler;
import org.jeecg.starter.sentinel.parser.JeecgHeaderRequestOriginParser;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * Sentinel 自动配置
 *
 * @author jeecg
 * @date 2026-07-07
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(SentinelFeignAutoConfiguration.class)
@ConditionalOnClass(com.alibaba.csp.sentinel.SphU.class)
public class JeecgSentinelAutoConfiguration {

	@Bean
	@Scope("prototype")
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.cloud.openfeign.sentinel.enabled", matchIfMissing = true)
	@ConditionalOnClass(Feign.class)
	public Feign.Builder feignSentinelBuilder() {
		return JeecgSentinelFeign.builder();
	}

	@Bean
	@ConditionalOnMissingBean
	public BlockExceptionHandler blockExceptionHandler() {
		return new JeecgUrlBlockHandler();
	}

	@Bean
	@ConditionalOnMissingBean
	public RequestOriginParser requestOriginParser() {
		return new JeecgHeaderRequestOriginParser();
	}

}
