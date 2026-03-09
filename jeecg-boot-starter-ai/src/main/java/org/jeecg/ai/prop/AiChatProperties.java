package org.jeecg.ai.prop;

import dev.langchain4j.model.openai.OpenAiChatModelName;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jeecg.ai.factory.AiModelFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

@NoArgsConstructor
@Data
@ConfigurationProperties(prefix = AiChatProperties.PREFIX)
public class AiChatProperties {
    
    public static final String PREFIX = "jeecg.ai-chat";

    /**
     * 是否启用
     */
    private boolean enabled = false;


    /**
     * 供应商
     */
    private String provider = AiModelFactory.AIMODEL_TYPE_OPENAI;


    /**
     * 秘钥信息
     */
    private Credential credential = new Credential();

    /**
     * api秘钥
     *
     * @deprecated 废弃，使用credential.apiKey
     */
    private String apiKey = "";


    public String getApiKey() {
        return credential.getApiKey();
    }

    public void setApiKey(String apiKey) {
        this.credential.setApiKey(apiKey);
    }

    /**
     * 使用的模型,默认gpt3.5turbo
     */
    private String model = OpenAiChatModelName.GPT_3_5_TURBO.toString();

    /**
     * api域名
     */
    private String apiHost = "https://api.openai.com/v1/";

    /**
     * 其他参数设置
     */
    private Metadata metadata = new Metadata();

    /**
     * 超时时间
     *
     * @deprecated 废弃，使用Metadata.timeout
     */
    private int timeout = metadata.getTimeout();

    public int getTimeout() {
        return metadata.getTimeout();
    }

    public void setTimeout(int timeout) {
        this.metadata.setTimeout(timeout);
    }

    /**
     * 网络代理
     */
    private Proxy proxy;

    @Data
    public static class Proxy {

        /**
         * 网络代理域名
         */
        String host;

        /**
         * 网络代理端口
         */
        Integer port;

    }


    @Data
    public static class Credential {

        /**
         * api秘钥
         */
        private String apiKey = "";

        /**
         * secretKey
         */
        private String secretKey;
    }

    @Data
    public static class Metadata {

        /**
         * 超时时间
         */
        private int timeout = 60;


    }

}
