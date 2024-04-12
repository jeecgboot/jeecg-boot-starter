package org.jeecg.boot.starter.rabbitmq.config;

import com.alibaba.cloud.stream.binder.rocketmq.convert.RocketMQMessageConverter;
import org.jeecg.common.config.mqtoken.TransmitUserTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.*;

import java.util.HashSet;
import java.util.Set;

/**
 * @author eightmonth@qq.com
 * @date 2024/3/29 10:47
 */
@Configuration
public class RocketMqConfig {
    /**
     * 替换默认covert，处理spring cloud bus rocketmq 发送消息其它app无法接收问题
     * @return
     */
    @Bean(RocketMQMessageConverter.DEFAULT_NAME)
    public CompositeMessageConverter rocketMqMessageConvert() {
        Set<MessageConverter> messageConverters = new HashSet();
        ByteArrayMessageConverter byteArrayMessageConverter = new ByteArrayMessageConverter();
        byteArrayMessageConverter.setContentTypeResolver((ContentTypeResolver)null);
        messageConverters.add(byteArrayMessageConverter);
        messageConverters.add(new StringMessageConverter());
        return new CompositeMessageConverter(messageConverters);
    }

    /**
     * 注入获取token过滤器
     * @return
     */
    @Bean
    public TransmitUserTokenFilter transmitUserInfoFromHttpHeader(){
        return new TransmitUserTokenFilter();
    }
}
