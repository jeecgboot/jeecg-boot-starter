package org.jeecg.ai.custom.zhipu;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 智普图像自定义model
 * @author: wangshuai
 * @date: 2026/1/26 15:36
 */
public class CustomZhipuAiImageModel implements ImageModel {

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final Boolean watermarkEnabled;
    private final String size;

    public CustomZhipuAiImageModel(String apiKey, String baseUrl, String model, Duration timeout, Duration connectTimeout, Boolean watermarkEnabled, String size) {
        this.apiKey = apiKey;
        if (baseUrl == null || baseUrl.isEmpty()) {
            this.baseUrl = "https://open.bigmodel.cn/api/paas/v4/";
        } else {
            // 自动修正智谱AI的baseUrl，如果用户只配置了域名，自动补充路径
            if (baseUrl.contains("open.bigmodel.cn") && !baseUrl.contains("/api/paas/v4")) {
                this.baseUrl = baseUrl.endsWith("/") ? baseUrl + "api/paas/v4/" : baseUrl + "/api/paas/v4/";
            } else {
                this.baseUrl = baseUrl;
            }
        }
        this.model = model;
        this.watermarkEnabled = watermarkEnabled;
        this.size = normalizeSize(size);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (timeout != null) {
            builder.readTimeout(timeout);
        }
        if (connectTimeout != null) {
            builder.connectTimeout(connectTimeout);
        }

        this.client = builder.build();
        this.objectMapper = new ObjectMapper();
    }

    public static CustomZhipuAiImageModelBuilder builder() {
        return new CustomZhipuAiImageModelBuilder();
    }

    @Override
    public Response<Image> generate(String prompt) {
        String url = this.baseUrl + (this.baseUrl.endsWith("/") ? "" : "/") + "images/generations";

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", this.model);
        requestMap.put("prompt", prompt);
        if (watermarkEnabled != null) {
            requestMap.put("watermark_enabled", watermarkEnabled);
        }
        if (size != null) {
            requestMap.put("size", size);
        }

        try {
            String json = objectMapper.writeValueAsString(requestMap);
            String token = ZhipuTokenUtils.generateToken(apiKey);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + token)
                    .post(RequestBody.create(MediaType.parse("application/json"), json))
                    .build();

            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    throw new RuntimeException("Zhipu AI image generation failed: " + response.code() + " " + errorBody);
                }
                if (response.body() == null) {
                    throw new RuntimeException("Empty response body");
                }
                CustomImageResponse imageResponse = objectMapper.readValue(response.body().string(), CustomImageResponse.class);

                if (imageResponse.getData() == null || imageResponse.getData().isEmpty()) {
                    throw new RuntimeException("No image data returned from Zhipu AI");
                }

                CustomImageResponse.CustomImageData firstImage = imageResponse.getData().get(0);
                Image image;
                if (firstImage.getUrl() != null) {
                    image = Image.builder().url(URI.create(firstImage.getUrl())).build();
                } else if (firstImage.getB64Json() != null) {
                    image = Image.builder().base64Data(firstImage.getB64Json()).build();
                } else {
                    throw new RuntimeException("Unknown image format in response");
                }

                return Response.from(image);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error communicating with Zhipu AI", e);
        }
    }

    /**
     * 验证图片大小
     * <p>
     * 智普的size的长宽均需满足512px-2880px之间,且为32整数倍
     *
     * @param size
     * @return
     */
    private String normalizeSize(String size) {
        if (size == null) {
            return null;
        }
        String s = size.trim().toLowerCase();
        if (s.isEmpty()) {
            return null;
        }
        s = s.replace("×", "x");
        String[] parts;
        if (s.contains("x")) {
            parts = s.split("x");
        } else if (s.contains("*")) {
            parts = s.split("\\*");
        } else {
            throw new IllegalArgumentException("智谱图片size格式错误，应为'宽x高'，例如 1024x1024");
        }
        if (parts.length != 2) {
            throw new IllegalArgumentException("智谱图片size格式错误，应为'宽x高'，例如 1024x1024");
        }
        int width;
        int height;
        try {
            width = Integer.parseInt(parts[0]);
            height = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("智谱图片size必须是数字，例如 1024x1024", e);
        }
        width = normalizeSide(width);
        height = normalizeSide(height);
        return width + "x" + height;
    }

    /**
     * 处理非正常图片的大小
     *
     * @param value
     * @return
     */
    private int normalizeSide(int value) {
        int v = value;
        if (v < 512) {
            v = 512;
        } else if (v > 2880) {
            v = 2880;
        }
        int mod = v % 32;
        if (mod != 0) {
            v = v - mod;
            if (v < 512) {
                v = 512;
            }
        }
        return v;
    }

    /**
     * 智普自定义image
     */
    public static class CustomZhipuAiImageModelBuilder {
        private String apiKey;
        private String baseUrl;
        private String model;
        private String size;
        private Duration readTimeout;
        private Duration connectTimeout;
        private Integer maxRetries;
        private Boolean logRequests;
        private Boolean logResponses;
        private Boolean watermarkEnabled;

        public CustomZhipuAiImageModelBuilder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public CustomZhipuAiImageModelBuilder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public CustomZhipuAiImageModelBuilder model(String model) {
            this.model = model;
            return this;
        }

        public CustomZhipuAiImageModelBuilder readTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public CustomZhipuAiImageModelBuilder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public CustomZhipuAiImageModelBuilder maxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public CustomZhipuAiImageModelBuilder logRequests(Boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        public CustomZhipuAiImageModelBuilder logResponses(Boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        public CustomZhipuAiImageModelBuilder watermarkEnabled(Boolean watermarkEnabled) {
            this.watermarkEnabled = watermarkEnabled;
            return this;
        }

        public CustomZhipuAiImageModelBuilder size(String size) {
            this.size = size;
            return this;
        }

        public CustomZhipuAiImageModel build() {
            return new CustomZhipuAiImageModel(apiKey, baseUrl, model, readTimeout, connectTimeout, watermarkEnabled, size);
        }
    }
}
