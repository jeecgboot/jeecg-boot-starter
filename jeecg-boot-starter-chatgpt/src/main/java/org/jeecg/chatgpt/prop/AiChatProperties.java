package org.jeecg.chatgpt.prop;

import com.unfbx.chatgpt.entity.chat.ChatCompletion;
import lombok.Data;
import lombok.NoArgsConstructor;
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
     * api秘钥
     */
    private String apiKey = "";

    /**
     * 使用的模型,默认gpt3.5turbo
     */
    private String model = ChatCompletion.Model.GPT_3_5_TURBO.getName();

    /**
     * api域名
     */
    private String apiHost = "https://api.openai.com/";

    /**
     * 超时时间
     */
    private int timeout = 60;

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

}
