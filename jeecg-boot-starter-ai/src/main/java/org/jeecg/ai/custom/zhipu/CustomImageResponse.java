package org.jeecg.ai.custom.zhipu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * @Description: 自定义image返回类
 * @author: wangshuai
 * @date: 2026/1/26 16:50
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class CustomImageResponse {
    private Long created;
    private List<CustomImageData> data;
    private String id;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    public static class CustomImageData {
        private String url;
        private String b64Json;
    }
}
