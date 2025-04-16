package org.jeecg.boot.starter.rabbitmq.config;


import org.jeecg.boot.starter.rabbitmq.event.JeecgRemoteApplicationEvent;
import org.jeecg.common.config.mqtoken.TransmitUserTokenFilter;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.support.ConsumerTagStrategy;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

import java.util.UUID;

/**
 * 消息队列配置类
 *
 * @author zyf
 */
@Configuration
@RemoteApplicationEventScan(basePackageClasses = JeecgRemoteApplicationEvent.class)
public class RabbitMqConfig implements RabbitListenerConfigurer {


    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        //设置忽略声明异常
        rabbitAdmin.setIgnoreDeclarationExceptions(true);
        return rabbitAdmin;
    }

    /**
     * 注入获取token过滤器
     * @return
     */
    @Bean
    public TransmitUserTokenFilter transmitUserInfoFromHttpHeader(){
        return new TransmitUserTokenFilter();
    }

    @Bean
    public SimpleMessageListenerContainer messageListenerContainer(ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        //手动确认
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        //当前的消费者数量
        container.setConcurrentConsumers(1);
        //最大的消费者数量
        container.setMaxConcurrentConsumers(1);
        //是否重回队列
        container.setDefaultRequeueRejected(true);

        //消费端的标签策略
        container.setConsumerTagStrategy(new ConsumerTagStrategy() {
            @Override
            public String createConsumerTag(String queue) {
                return queue + "_" + UUID.randomUUID().toString();
            }
        });
        return container;
    }

    
    //-----------------------------------------------------------------------------
    //update-begin---author:scott ---date:2023-03-22  for：[QQYUN-4114]简流数据事务，并行和串行规则实现-------------
    @Bean
    public RabbitListenerErrorHandler rabbitListenerErrorHandler() {
        return (amqpMessage, channel,message, exception) -> {
            exception.printStackTrace();
            throw exception;
        };
    }
    
    @Bean
    public MessageHandlerMethodFactory messageHandlerMethodFactory() {
        DefaultMessageHandlerMethodFactory messageHandlerMethodFactory = new DefaultMessageHandlerMethodFactory();
        messageHandlerMethodFactory.setMessageConverter(consumerJackson2MessageConverter());
        return messageHandlerMethodFactory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        //手动确认
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(1);
        //update-begin---author:chenrui ---date:20240110  for：[issue/5778]springAmpq默认mc会验证反序列类可信导致接收消息报错------------
        factory.setMessageConverter(messageConverter());
        //update-end---author:chenrui ---date:20240110  for：[issue/5778]springAmpq默认mc会验证反序列类可信导致接收消息报错------------
        return factory;
    }

    @Bean
    public MappingJackson2MessageConverter consumerJackson2MessageConverter() {
        return new MappingJackson2MessageConverter();
    }
    
    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
        registrar.setMessageHandlerMethodFactory(messageHandlerMethodFactory());
    }
    //update-end---author:scott ---date::2023-03-22  for：[QQYUN-4114]简流数据事务，并行和串行规则实现--------------
    //-----------------------------------------------------------------------------

    //update-begin---author:chenrui ---date:20240110  for：[issue/5778]springAmpq默认mc会验证反序列类可信导致接收消息报错------------
    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }
    //update-end---author:chenrui ---date:20240110  for：[issue/5778]springAmpq默认mc会验证反序列类可信导致接收消息报错------------
}
