package org.jeecg.boot.starter.job.config;

import com.xxl.tool.http.IPTool;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.boot.starter.job.prop.XxlJobProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 定时任务配置
 *
 * @author jeecg
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(value = XxlJobProperties.class)
@ConditionalOnProperty(value = "jeecg.xxljob.enabled", havingValue = "true", matchIfMissing = true)
public class XxlJobConfiguration {

	@Autowired
	private XxlJobProperties xxlJobProperties;

	@Bean
	public JeecgXxlJobSpringExecutor xxlJobExecutor() {
		log.info(">>>>>>>>>>> xxl-job config init.");
		JeecgXxlJobSpringExecutor xxlJobSpringExecutor = new JeecgXxlJobSpringExecutor();
		xxlJobSpringExecutor.setAdminAddresses(xxlJobProperties.getAdminAddresses());
		xxlJobSpringExecutor.setAppname(xxlJobProperties.getAppname());
		//update-begin--Author:scott -- Date:20251230 -- for：支持手动配置IP和Port解决跨网络问题#9189、兼容system服务和demo服务无法同时使用xxl-job #2313---
		String ip = xxlJobProperties.getIp();
		Integer port = xxlJobProperties.getPort();
		if (StringUtils.hasText(ip)) {
			xxlJobSpringExecutor.setIp(ip);
		}
		if (port != null && port > 0) {
			xxlJobSpringExecutor.setPort(port);
		} else {
			// 自动检测可用端口，起始 10000，避免和网关 9999 冲突
			int availablePort = IPTool.getAvailablePort(10000);
			xxlJobSpringExecutor.setPort(availablePort);
			log.info(">>>>>>>>>>> xxl-job auto-assigned port: {}", availablePort);
		}
		//update-end--Author:scott -- Date:20251230 -- for：支持手动配置IP和Port解决跨网络问题#9189、兼容system服务和demo服务无法同时使用xxl-job #2313---
		// 排除第三方包的Bean扫描，避免 NoClassDefFoundError（如 springdoc 依赖 spring-data-commons 但未引入）
		xxlJobSpringExecutor.setExcludedPackage("org.springframework.,spring.,org.springdoc.");
		xxlJobSpringExecutor.setAccessToken(xxlJobProperties.getAccessToken());
		xxlJobSpringExecutor.setLogPath(xxlJobProperties.getLogPath());
		xxlJobSpringExecutor.setLogRetentionDays(xxlJobProperties.getLogRetentionDays());
		return xxlJobSpringExecutor;
	}

}
