package org.jeecg.common.modules.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.constant.CacheConstant;
import org.jeecg.common.constant.GlobalConstants;
import org.jeecg.common.modules.redis.receiver.RedisReceiver;
import org.jeecg.common.modules.redis.writer.JeecgRedisCacheWriter;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


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

	@Resource
	private JeecgRedisCacheTtls redisCacheProperties;

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
		RedisCacheWriter writer = new JeecgRedisCacheWriter(factory, Duration.ofMillis(50L));
		// 创建默认缓存配置对象
		Map<String, RedisCacheConfiguration> initialCaches = new HashMap<>();
		initialCaches.put(CacheConstant.SYS_DICT_TABLE_CACHE, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(10)).disableCachingNullValues()
				.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer)));
		initialCaches.put(CacheConstant.TEST_DEMO_CACHE, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5)).disableCachingNullValues());
		initialCaches.put(CacheConstant.PLUGIN_MALL_RANKING, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(24)).disableCachingNullValues());
		initialCaches.put(CacheConstant.PLUGIN_MALL_PAGE_LIST, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(24)).disableCachingNullValues());
		// 流程运行时数据，缓存有效期1年
		initialCaches.put(CacheConstant.FLOW_RUNTIME_DATA_PREFIX, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(365)).disableCachingNullValues());

		// 设置自定义缓存
		redisCacheProperties.getCacheTtls().forEach((cacheName, ttl) -> {
			log.info("自定义缓存配置，cacheKey:{}, 缓存秒数:{}",cacheName,ttl);
			initialCaches.put(cacheName, RedisCacheConfiguration.defaultCacheConfig()
					.entryTtl(Duration.ofSeconds(ttl))
					.disableCachingNullValues()
					.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer)));
		});
		
		RedisCacheManager cacheManager = new RedisConfigCacheManager(writer, redisCacheConfiguration, initialCaches);
		cacheManager.setTransactionAware(true);
		//update-end-author:taoyan date:20210316 for:注解CacheEvict根据key删除redis支持通配符*
		return cacheManager;
	}


	public static class RedisConfigCacheManager extends RedisCacheManager {


		public RedisConfigCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration, Map<String, RedisCacheConfiguration> initialCaches) {
			super(cacheWriter, defaultCacheConfiguration, initialCaches, true);
		}

		private static final RedisSerializationContext.SerializationPair<Object> DEFAULT_PAIR = RedisSerializationContext.SerializationPair
				.fromSerializer(jacksonSerializer());

		private static final CacheKeyPrefix DEFAULT_CACHE_KEY_PREFIX = cacheName -> cacheName + "::";

		@Override
		protected RedisCache createRedisCache(String name, RedisCacheConfiguration cacheConfig) {
			final int lastIndexOf = name.lastIndexOf( '#');
			if (lastIndexOf > -1) {
				final String ttl = name.substring(lastIndexOf + 1);
				final Duration duration = Duration.ofSeconds(Long.parseLong(ttl));
				cacheConfig = cacheConfig.entryTtl(duration);
				//修改缓存key和value值的序列化方式
				cacheConfig = cacheConfig.computePrefixWith(DEFAULT_CACHE_KEY_PREFIX)
						.serializeValuesWith(DEFAULT_PAIR);
				final String cacheName = name.substring(0, lastIndexOf);
				return super.createRedisCache(cacheName, cacheConfig);
			}else{
				//修改缓存key和value值的序列化方式
				cacheConfig = cacheConfig.computePrefixWith(DEFAULT_CACHE_KEY_PREFIX)
						.serializeValuesWith(DEFAULT_PAIR);
				return super.createRedisCache(name, cacheConfig);
			}
		}


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

	private static Jackson2JsonRedisSerializer jacksonSerializer() {
		Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
		objectMapper.registerModule(new JavaTimeModule());
		// 反序列化设置 关闭反序列化时Jackson发现无法找到对应的对象字段，便会抛出UnrecognizedPropertyException
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
		return jackson2JsonRedisSerializer;
	}


}
