package org.jeecg.boot.starter.job.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.boot.starter.job.prop.XxlJobProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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

    //@Bean(initMethod = "start", destroyMethod = "destroy")
    @Bean
    @ConditionalOnClass()
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info(">>>>>>>>>>> xxl-job config init.");
        //log.info(">>>> ip="+xxlJobProperties.getIp()+"，Port="+xxlJobProperties.getPort()+"，address="+xxlJobProperties.getAdminAddresses());
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
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
        }
        //update-end--Author:scott -- Date:20251230 -- for：支持手动配置IP和Port解决跨网络问题#9189、兼容system服务和demo服务无法同时使用xxl-job #2313---
        xxlJobSpringExecutor.setAccessToken(xxlJobProperties.getAccessToken());
        xxlJobSpringExecutor.setLogPath(xxlJobProperties.getLogPath());
        xxlJobSpringExecutor.setLogRetentionDays(xxlJobProperties.getLogRetentionDays());
        return xxlJobSpringExecutor;
    }


}
