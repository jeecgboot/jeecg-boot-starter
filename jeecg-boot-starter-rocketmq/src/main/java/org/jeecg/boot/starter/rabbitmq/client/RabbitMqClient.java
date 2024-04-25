package org.jeecg.boot.starter.rabbitmq.client;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.jeecg.boot.starter.rabbitmq.event.EventObj;
import org.jeecg.boot.starter.rabbitmq.event.JeecgRemoteApplicationEvent;
import org.jeecg.common.base.BaseMap;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author eightmonth@qq.com
 * @date 2024/3/29 10:40
 */
@Slf4j
@Configuration
@AllArgsConstructor
public class RabbitMqClient {

    private RocketMQTemplate rocketMQTemplate;
    private ApplicationEventPublisher publisher;
    private BusProperties busProperties;

    /**
     * 同步发送
     * @param topic
     * @param payload
     */
    public void sendMessage(String topic, Object payload) {
        rocketMQTemplate.convertAndSend(topic, payload);
    }

    /**
     * 同步批量发送
     * @param topic
     * @param payload
     * @return
     * @param <T>
     */
    public <T> SendResult sendMessage(String topic, Collection<T> payload) {
        return rocketMQTemplate.syncSend(topic, convert(payload));
    }


    /**
     * 异步发送
     * @param topic
     * @param payload
     * @param callback
     */
    public void sendAsyncMessage(String topic, Object payload, SendCallback callback) {
        rocketMQTemplate.asyncSend(topic, payload, callback);
    }

    /**
     * 异步批量发送
     * @param topic
     * @param payload
     * @param sendCallback
     * @param <T>
     */
    public <T> void sendAsyncMessage(String topic, Collection<T> payload, SendCallback sendCallback) {
        rocketMQTemplate.asyncSend(topic, convert(payload), sendCallback);
    }

    /**
     * 同步发送顺序消息
     * @param topic
     * @param payload
     * @param hashKey
     */
    public void sendOrderlyMessage(String topic, Object payload, String hashKey) {
        rocketMQTemplate.syncSendOrderly(topic, payload, hashKey);
    }

    /**
     * 异步发送顺序消息
     * @param topic
     * @param payload
     * @param hashKey
     * @param callback
     */
    public void sendAsyncOrderlyMessage(String topic, Object payload, String hashKey, SendCallback callback) {
        rocketMQTemplate.asyncSendOrderly(topic, payload, hashKey, callback);
    }

    /**
     * Oneway消息
     * @param topic
     * @param payload
     */
    public void sendOneway(String topic, Object payload) {
        rocketMQTemplate.sendOneWay(topic, payload);
    }

    /**
     * 顺序Oneway消息
     * @param topic
     * @param payload
     * @param hashKey
     */
    public void sendOnewayOrderly(String topic, Object payload, String hashKey) {
        rocketMQTemplate.sendOneWayOrderly(topic, payload, hashKey);
    }

    /**
     * 事务消息
     * @param topic
     * @param payload
     * @param arg
     */
    public void sendMessageInTransaction(String topic, Object payload, Object arg) {
        rocketMQTemplate.sendMessageInTransaction(topic, MessageBuilder.withPayload(payload).build(), arg);
    }

    /**
     * pull模式
     * 拉取消息
     * @param clazz
     * @return
     * @param <T>
     */
    public <T> List<T> receive(Class<T> clazz) {
        return rocketMQTemplate.receive(clazz);
    }

    /**
     * 延迟消息， delayLevel取如下列表中的值，等后续rocket-spring升到2.3.0时，可以设置延迟多少秒，目前适配版本只能设置延迟等级
     * Level 1: 1s（1秒）
     * Level 2: 5s（5秒）
     * Level 3: 10s（10秒）
     * Level 4: 30s（30秒）
     * Level 5: 1m（1分钟）
     * Level 6: 2m（2分钟）
     * Level 7: 3m（3分钟）
     * Level 8: 4m（4分钟）
     * Level 9: 5m（5分钟）
     * Level 10: 6m（6分钟）
     * Level 11: 7m（7分钟）
     * Level 12: 8m（8分钟）
     * Level 13: 9m（9分钟）
     * Level 14: 10m（10分钟）
     * Level 15: 20m（20分钟）
     * Level 16: 30m（30分钟）
     * Level 17: 1h（1小时）
     * Level 18: 2h（2小时）
     * @param topic
     * @param payload
     * @param delayLevel
     * @return
     */
    public SendResult sendMessage(String topic, Object payload, Integer delayLevel) {
        return rocketMQTemplate.syncSend(topic, MessageBuilder.withPayload(payload).build(), rocketMQTemplate.getProducer().getSendMsgTimeout(), delayLevel);
    }

    /**
     * 发送远程事件
     *
     * @param handlerName
     * @param baseMap
     */
    public void publishEvent(String handlerName, BaseMap baseMap) {
        EventObj eventObj = new EventObj();
        eventObj.setHandlerName(handlerName);
        eventObj.setBaseMap(baseMap);
        publisher.publishEvent(new JeecgRemoteApplicationEvent(eventObj, busProperties.getId()));
    }



    public <T> List<Message<T>> convert(Collection<T> payload) {
        return payload.stream().map(p -> MessageBuilder.withPayload(p).build()).collect(Collectors.toList());
    }

}
