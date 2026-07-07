package org.jeecg.common.modules.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.constant.CacheConstant;
import org.jeecg.common.constant.GlobalConstants;
import org.jeecg.common.modules.redis.receiver.RedisReceiver;
import org.jeecg.common.modules.redis.writer.JeecgRedisCacheWriter;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;


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

	// 缓存Jackson序列化器实例，避免重复创建
	private static volatile RedisSerializer<Object> cachedJacksonSerializer;


	/**
	 * RedisTemplate配置
	 * @param lettuceConnectionFactory
	 * @return
	 */
	@Bean
	public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory lettuceConnectionFactory) {
		long startTime = System.currentTimeMillis();
		
        RedisSerializer<Object> jackson2JsonRedisSerializer = getJacksonSerializer();
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
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
		
		long endTime = System.currentTimeMillis();
		log.info(" --- redis config init ---，耗时: {}ms", (endTime - startTime));
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
        RedisSerializer<Object> jackson2JsonRedisSerializer = getJacksonSerializer();
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

		// 设置自定义缓存 - 优化：只在有配置时才处理
		Map<String, Long> cacheTtls = redisCacheProperties.getCacheTtls();
		if (cacheTtls != null && !cacheTtls.isEmpty()) {
			cacheTtls.forEach((cacheName, ttl) -> {
				log.debug("自定义缓存配置，cacheKey:{}, 缓存秒数:{}",cacheName,ttl);
				initialCaches.put(cacheName, RedisCacheConfiguration.defaultCacheConfig()
						.entryTtl(Duration.ofSeconds(ttl))
						.disableCachingNullValues()
						.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer)));
			});
		}
		
		RedisCacheManager cacheManager = new RedisConfigCacheManager(writer, redisCacheConfiguration, initialCaches);
		cacheManager.setTransactionAware(true);
		//update-end-author:taoyan date:20210316 for:注解CacheEvict根据key删除redis支持通配符*
		return cacheManager;
	}


	public static class RedisConfigCacheManager extends RedisCacheManager {


		public RedisConfigCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration, Map<String, RedisCacheConfiguration> initialCaches) {
			super(cacheWriter, defaultCacheConfiguration, initialCaches);
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
	 * 注意：某些云Redis服务或受限环境不支持SUBSCRIBE命令
	 * 如果生产环境Redis不支持pub/sub，请在配置文件中设置：jeecg.redis.listener-enabled=false
	 *
	 * @param redisConnectionFactory redis 配置
	 * @return
	 */
	@Bean
	@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
		prefix = "jeecg.redis",
		name = "listener-enabled",
		havingValue = "true",
		matchIfMissing = true
	)
	public RedisMessageListenerContainer redisContainer(RedisConnectionFactory redisConnectionFactory, RedisReceiver redisReceiver, MessageListenerAdapter commonListenerAdapter) {
		log.info("Redis消息监听器已启用。如果Redis不支持SUBSCRIBE命令，请设置 jeecg.redis.listener-enabled=false");
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(redisConnectionFactory);
		container.addMessageListener(commonListenerAdapter, new ChannelTopic(GlobalConstants.REDIS_TOPIC_NAME));
		return container;
	}


	@Bean
	@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
		prefix = "jeecg.redis",
		name = "listener-enabled",
		havingValue = "true",
		matchIfMissing = true
	)
	MessageListenerAdapter commonListenerAdapter(RedisReceiver redisReceiver) {
		MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(redisReceiver, "onMessage");
		messageListenerAdapter.setSerializer(jacksonSerializer());
		return messageListenerAdapter;
	}

	/**
	 * 获取Jackson序列化器（单例模式，线程安全）
	 */
	private static RedisSerializer<Object> getJacksonSerializer() {
		if (cachedJacksonSerializer == null) {
			synchronized (RedisConfig.class) {
				if (cachedJacksonSerializer == null) {
					cachedJacksonSerializer = jacksonSerializer();
				}
			}
		}
		return cachedJacksonSerializer;
	}

	private static RedisSerializer<Object> jacksonSerializer() {
		// Jackson 3.x 推荐的多态类型校验器，生产环境中建议限制包或类
		PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
				.allowIfBaseType(Object.class) // 放宽限制，实际使用可针对具体包或类限制
				.build();

		ObjectMapper objectMapper = JsonMapper.builder()
				// 启用属性字母排序，方便调试和对比
				.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
				// 启用非ASCII字符转义，保证序列化输出ASCII安全
				.enable(JsonWriteFeature.ESCAPE_NON_ASCII)
				// 反序列化设置 关闭反序列化时Jackson发现无法找到对应的对象字段，便会抛出UnrecognizedPropertyException
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				// 多态类型处理，使用 PROPERTY 格式而不是 WRAPPER_ARRAY
				// 这样类型信息会作为 @class 属性嵌入到 JSON 对象中
				.activateDefaultTyping(ptv, DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
				// 设置所有字段（包括private）可见
				.changeDefaultVisibility(vc -> vc.withVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY))
				// 允许单引号和非标准控制字符，视具体需求启用
				.enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
				.enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
				.build();
		
		// 注：Jackson 3.x 默认已经包含对 Java 8 日期时间类型的支持，无需额外注册 JavaTimeModule
		
		return new JacksonJsonRedisSerializer(objectMapper, Object.class);
	}


}
