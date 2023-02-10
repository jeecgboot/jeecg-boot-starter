package org.jeecg.common.modules.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import lombok.extern.slf4j.Slf4j;

import org.jeecg.common.constant.CacheConstant;
import org.jeecg.common.constant.GlobalConstants;

import org.jeecg.common.modules.redis.po.JeecgCache;
import org.jeecg.common.modules.redis.receiver.RedisReceiver;
import org.jeecg.common.modules.redis.writer.JeecgRedisCacheWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.*;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;

/**
* 开启缓存支持
* @author zyf
 * @Return:
*/
@Slf4j
@EnableCaching
@Configuration
public class RedisConfig extends CachingConfigurerSupport {

	@Resource
	private LettuceConnectionFactory lettuceConnectionFactory;
	/**扩展缓存声明这个bean 就可以自动注入*/
	@Resource
	private JeecgCache jeecgCache;

	/**
	 * RedisTemplate配置
	 * @param lettuceConnectionFactory
	 * @return
	 */
	@Bean
	public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory lettuceConnectionFactory) {
		log.info(" --- redis config init --- ");
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = jacksonSerializer();
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
		redisTemplate.setConnectionFactory(lettuceConnectionFactory);
		RedisSerializer<String> stringSerializer = new StringRedisSerializer();

		// key序列化
		redisTemplate.setKeySerializer(stringSerializer);
		// value序列化
		redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
		// Hash key序列化
		redisTemplate.setHashKeySerializer(stringSerializer);
		// Hash value序列化
		redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
		redisTemplate.afterPropertiesSet();
		return redisTemplate;
	}

	/**
	 * 缓存配置管理器
	 *
	 * @param factory
	 * @return
	 */
	@Bean
	public CacheManager cacheManager(LettuceConnectionFactory factory) {
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = jacksonSerializer();
        // 配置序列化（解决乱码的问题）,并且配置缓存默认有效期 6小时
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(6));
        RedisCacheConfiguration redisCacheConfiguration = config.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
															.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer));
															//.disableCachingNullValues();

		// 以锁写入的方式创建RedisCacheWriter对象
		//update-begin-author:taoyan date:20210316 for:注解CacheEvict根据key删除redis支持通配符*
		RedisCacheWriter writer = new JeecgRedisCacheWriter(factory, Duration.ofMillis(50L));
		Map<String, RedisCacheConfiguration> customCache = new HashMap<>();
		if (jeecgCache == null) {
			jeecgCache = new JeecgCache();
		}
		jeecgCache.put(CacheConstant.TEST_DEMO_CACHE, 60*5);
		jeecgCache.put(CacheConstant.PLUGIN_MALL_RANKING, 24*60*60);
		jeecgCache.put(CacheConstant.PLUGIN_MALL_PAGE_LIST, 24*60*60);

		jeecgCache.getCache().forEach((k,v)->{
			log.info("自定义缓存配置，key:{},value:{}",k,v);
			customCache.put(k, redisCacheConfiguration.entryTtl(Duration.ZERO.withSeconds(v)).disableCachingNullValues());
		});
		//RedisCacheWriter.lockingRedisCacheWriter(factory);
		// 创建默认缓存配置对象
		/* 默认配置，设置缓存有效期 1小时*/
		//RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(1));
		// 自定义配置test:demo 的超时时间为 5分钟
		RedisCacheManager cacheManager = RedisCacheManager.builder(writer).cacheDefaults(redisCacheConfiguration)
            .withInitialCacheConfigurations(singletonMap(CacheConstant.SYS_DICT_TABLE_CACHE,
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(10)).disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer))))
				.withInitialCacheConfigurations(customCache)
				.transactionAware().build();
		//update-end-author:taoyan date:20210316 for:注解CacheEvict根据key删除redis支持通配符*
		return cacheManager;
	}

	/**
	 * redis 监听配置
	 *
	 * @param redisConnectionFactory redis 配置
	 * @return
	 */
	@Bean
	public RedisMessageListenerContainer redisContainer(RedisConnectionFactory redisConnectionFactory, RedisReceiver redisReceiver, MessageListenerAdapter commonListenerAdapter) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(redisConnectionFactory);
		container.addMessageListener(commonListenerAdapter, new ChannelTopic(GlobalConstants.REDIS_TOPIC_NAME));
		return container;
	}


	@Bean
	MessageListenerAdapter commonListenerAdapter(RedisReceiver redisReceiver) {
		MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(redisReceiver, "onMessage");
		messageListenerAdapter.setSerializer(jacksonSerializer());
		return messageListenerAdapter;
	}

	private Jackson2JsonRedisSerializer jacksonSerializer() {
		Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
		jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
		return jackson2JsonRedisSerializer;
	}


}
