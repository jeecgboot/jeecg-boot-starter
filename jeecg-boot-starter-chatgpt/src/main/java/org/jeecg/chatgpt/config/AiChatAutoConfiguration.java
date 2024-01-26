package org.jeecg.chatgpt.config;

import com.unfbx.chatgpt.OpenAiClient;
import com.unfbx.chatgpt.OpenAiStreamClient;
import com.unfbx.chatgpt.function.KeyRandomStrategy;
import com.unfbx.chatgpt.interceptor.OpenAILogger;
import com.unfbx.chatgpt.interceptor.OpenAiResponseInterceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.commons.lang.StringUtils;
import org.jeecg.chatgpt.prop.AiChatProperties;
import org.jeecg.chatgpt.service.AiChatService;
import org.jeecg.chatgpt.service.impl.ChatGptService;
import org.jeecg.chatgpt.service.impl.DefaultAiChatService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * AI聊天工具自动装配
 *
 * @author chenrui
 * @date 2024/1/12 15:45
 */
@Configuration
@EnableConfigurationProperties(AiChatProperties.class)
public class AiChatAutoConfiguration {

    //update-begin---author:chenrui ---date:20240126  for：新增streamClientBean------------

    /**
     * openAI客户端
     *
     * @param aiChatProperties
     * @return
     * @author chenrui
     * @date 2024/1/12 17:09
     */
    @Bean
    @ConditionalOnProperty(prefix = AiChatProperties.PREFIX, name = "enabled", havingValue = "true")
    public OpenAiClient openAiClient(AiChatProperties aiChatProperties) {
        OkHttpClient okHttpClient = buildHttpClient(aiChatProperties);
        // 构造openAiClient
        return OpenAiClient.builder()
                //支持多key传入，请求时候随机选择
                .apiKey(Collections.singletonList(aiChatProperties.getApiKey()))
                //自定义key的获取策略(实现KeyStrategyFunction)：默认KeyRandomStrategy
                .keyStrategy(new KeyRandomStrategy())
                .okHttpClient(okHttpClient)
                //自己做了代理就传代理地址，没有可不不传
                .apiHost(aiChatProperties.getApiHost())
                .build();
    }


    /**
     * openAIStream客户端
     *
     * @param aiChatProperties
     * @return
     * @author chenrui
     * @date 2024/1/25 10:50
     */
    @Bean
    @ConditionalOnProperty(prefix = AiChatProperties.PREFIX, name = "enabled", havingValue = "true")
    public OpenAiStreamClient openAiStreamClient(AiChatProperties aiChatProperties) {
        OkHttpClient okHttpClient = buildHttpClient(aiChatProperties);
        return OpenAiStreamClient.builder()
                .apiKey(Collections.singletonList(aiChatProperties.getApiKey()))
                //自定义key的获取策略：默认KeyRandomStrategy
                .keyStrategy(new KeyRandomStrategy())
                .okHttpClient(okHttpClient)
                .apiHost(aiChatProperties.getApiHost())
                .build();
    }

    /**
     * 构建http客户端
     *
     * @param aiChatProperties
     * @return
     * @author chenrui
     * @date 2024/1/25 10:49
     */
    private static OkHttpClient buildHttpClient(AiChatProperties aiChatProperties) {
        //网络代理
        AiChatProperties.Proxy proxyProp = aiChatProperties.getProxy();
        Proxy proxy = null;
        if (null != proxyProp && StringUtils.isNotEmpty(proxyProp.getHost())) {
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyProp.getHost(), proxyProp.getPort()));
        }
        // http日志设置
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor(new OpenAILogger());
        //生产或者测试环境建议设置为这三种级别：NONE,BASIC,HEADERS
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        OkHttpClient okHttpClient = new OkHttpClient
                .Builder()
                .proxy(proxy)
                .addInterceptor(httpLoggingInterceptor)
                .addInterceptor(new OpenAiResponseInterceptor())
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(aiChatProperties.getTimeout(), TimeUnit.SECONDS)
                .readTimeout(aiChatProperties.getTimeout(), TimeUnit.SECONDS)
                .build();
        return okHttpClient;
    }

    //update-end---author:chenrui ---date:20240126  for：新增streamClientBean------------

    /**
     * ChatGpt聊天Service
     *
     * @param openAiClient
     * @return
     * @author chenrui
     * @date 2024/1/12 17:09
     */
    @Bean
    @ConditionalOnBean(OpenAiClient.class)
    public AiChatService chatGptAiChatService(OpenAiClient openAiClient) {
        return new ChatGptService(openAiClient);
    }

    /**
     * 默认的AI聊天Service
     *
     * @return
     * @author chenrui
     * @date 2024/1/12 17:08
     */
    @Bean
    @ConditionalOnMissingBean(AiChatService.class)
    public AiChatService defaultAiChatService() {
        return new DefaultAiChatService();
    }

}
