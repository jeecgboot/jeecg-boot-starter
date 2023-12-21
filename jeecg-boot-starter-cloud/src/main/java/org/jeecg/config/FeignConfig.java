package org.jeecg.config;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.alibaba.fastjson.support.springfox.SwaggerJsonSerializer;
import feign.Feign;
import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.config.TenantContext;
import org.jeecg.common.config.mqtoken.UserTokenContext;
import org.jeecg.common.util.PathMatcherUtil;
import org.jeecg.starter.cloud.util.HttpUtils;
import org.jeecg.starter.cloud.util.SignUtil;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;

/**
 * @Description: FeignConfig
 * @author: JeecgBoot
 */
@ConditionalOnClass(Feign.class)
@AutoConfigureBefore(FeignAutoConfiguration.class)
@Slf4j
@Configuration
public class FeignConfig {
    @Resource
    JeecgCloudBaseConfig jeecgCloudBaseConfig;

    /**===============================================================================================*/
    /**
     * ::非常重要::
     * 注意：这四个常量值如果修改，需要与 jeecg-boot-base-core/org.jeecg.common.constant.CommonConstant 中的值保持一致。
     */
    public static final String X_ACCESS_TOKEN = "X-Access-Token";
    public static final String X_SIGN = "X-Sign";
    public static final String X_TIMESTAMP = "X-TIMESTAMP";
    public static final String TENANT_ID = "X-Tenant-Id";
    /**===============================================================================================*/

    /**
     * 设置feign header参数
     * 【X_ACCESS_TOKEN】【X_SIGN】【X_TIMESTAMP】
     * @return
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (null != attributes) {
                HttpServletRequest request = attributes.getRequest();
                log.debug("Feign request: {}", request.getRequestURI());
                // 将token信息放入header中
                String token = request.getHeader(FeignConfig.X_ACCESS_TOKEN);
                if(token==null || "".equals(token)){
                    token = request.getParameter("token");
                    //【issues/4683】微服务之间调用免Token方案的问题 
                    if (StringUtils.isEmpty(token)) {
                        token = UserTokenContext.getToken();
                    }
                }
                log.info("Feign Login Request token: {}", token);
                requestTemplate.header(FeignConfig.X_ACCESS_TOKEN, token);

                //update-begin-author:taoyan date:2022-6-23 for: issues/I5AO20 多租户微服务之间调用找不到tenant-id（自定义页面）
                // 将tenantId信息放入header中
                String tenantId = request.getHeader(FeignConfig.TENANT_ID);
                if(tenantId==null || "".equals(tenantId)){
                    tenantId = request.getParameter(FeignConfig.TENANT_ID);
                }
                log.info("Feign Login Request tenantId: {}", tenantId);
                requestTemplate.header(FeignConfig.TENANT_ID, tenantId);
                //update-end-author:taoyan date:2022-6-23 for: issues/I5AO20 多租户微服务之间调用找不到tenant-id（自定义页面）

            }else{
                String token = UserTokenContext.getToken();
                log.info("Feign no Login token: {}", token);
                requestTemplate.header(FeignConfig.X_ACCESS_TOKEN, token);

                //update-begin-author:taoyan date:2022-6-23 for: issues/I5AO20 多租户微服务之间调用找不到tenant-id（自定义页面）
                String tenantId = TenantContext.getTenant();
                log.info("Feign no Login tenantId: {}", tenantId);
                requestTemplate.header(FeignConfig.TENANT_ID, tenantId);
                //update-end-author:taoyan date:2022-6-23 for: issues/I5AO20 多租户微服务之间调用找不到tenant-id（自定义页面）
            }

            //================================================================================================================
            //针对特殊接口，进行加签验证 ——根据URL地址过滤请求 【字典表参数签名验证】

            //1.查询需要进行签名拦截的接口 signUrls
            String signUrls = jeecgCloudBaseConfig.getSignUrls();
            List signUrlsArray = null;
            if (StringUtils.isNotBlank(signUrls)) {
                signUrlsArray = Arrays.asList(signUrls.split(","));
            } else {
                signUrlsArray = Arrays.asList(PathMatcherUtil.SIGN_URL_LIST);
            }
            //2.拦截处理，加入签名逻辑
            if (PathMatcherUtil.matches(signUrlsArray,requestTemplate.path())) {
                try {
                    log.info("============================ [begin] fegin starter url ============================");
                    log.info(requestTemplate.path());
                    log.info(requestTemplate.method());
                    String queryLine = requestTemplate.queryLine();
                    if(queryLine!=null && queryLine.startsWith("?")){
                        queryLine = queryLine.substring(1);
                    }
                    log.info(queryLine);
                    if(requestTemplate.body()!=null){
                        log.info(new String(requestTemplate.body()));
                    }
                    SortedMap<String, String> allParams = HttpUtils.getAllParams(requestTemplate.path(),queryLine,requestTemplate.body(),requestTemplate.method());
                    String sign = SignUtil.getParamsSign(allParams);
                    log.info(" Feign request params sign: {}",sign);
                    log.info("============================ [end] fegin starter url ============================");
                    requestTemplate.header(FeignConfig.X_SIGN, sign);
                    //update-begin--author:taoyan---date:20220421--for: VUEN-410【签名改造】 X-TIMESTAMP牵扯
                    requestTemplate.header(FeignConfig.X_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
                    //update-end--author:taoyan---date:20220421--for: VUEN-410【签名改造】 X-TIMESTAMP牵扯
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //================================================================================================================
        };
    }



    /**
     * Feign 客户端的日志记录，默认级别为NONE
     * Logger.Level 的具体级别如下：
     * NONE：不记录任何信息
     * BASIC：仅记录请求方法、URL以及响应状态码和执行时间
     * HEADERS：除了记录 BASIC级别的信息外，还会记录请求和响应的头信息
     * FULL：记录所有请求与响应的明细，包括头信息、请求体、元数据
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * Feign支持文件上传
     * @param messageConverters
     * @return
     */
    @Bean
    @Primary
    @Scope("prototype")
    public Encoder multipartFormEncoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        return new SpringFormEncoder(new SpringEncoder(messageConverters));
    }

    // update-begin--Author:sunjianlei Date:20210604 for： 给 Feign 添加 FastJson 的解析支持 ----------
    @Bean
    public Encoder feignEncoder() {
        return new SpringEncoder(feignHttpMessageConverter());
    }

    @Bean
    public Decoder feignDecoder() {
        return new SpringDecoder(feignHttpMessageConverter());
    }

    /**
     * 设置解码器为fastjson
     *
     * @return
     */
    private ObjectFactory<HttpMessageConverters> feignHttpMessageConverter() {
        final HttpMessageConverters httpMessageConverters = new HttpMessageConverters(this.getFastJsonConverter());
        return () -> httpMessageConverters;
    }

    private FastJsonHttpMessageConverter getFastJsonConverter() {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();

        List<MediaType> supportedMediaTypes = new ArrayList<>();
        MediaType mediaTypeJson = MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE);
        supportedMediaTypes.add(mediaTypeJson);
        converter.setSupportedMediaTypes(supportedMediaTypes);
        FastJsonConfig config = new FastJsonConfig();
        config.getSerializeConfig().put(JSON.class, new SwaggerJsonSerializer());
        config.setSerializerFeatures(SerializerFeature.DisableCircularReferenceDetect);
        converter.setFastJsonConfig(config);

        return converter;
    }
    // update-end--Author:sunjianlei Date:20210604 for： 给 Feign 添加 FastJson 的解析支持 ----------

}
