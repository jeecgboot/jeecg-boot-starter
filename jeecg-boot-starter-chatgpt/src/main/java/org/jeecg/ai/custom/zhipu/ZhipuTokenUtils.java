package org.jeecg.ai.custom.zhipu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 获取智普token
 * @author: wangshuai
 * @date: 2026/1/26 16:24
 */
public class ZhipuTokenUtils {
    private static final long EXPIRATION_SECONDS = 300;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String generateToken(String apiKey) {
        String[] parts = apiKey.split("\\.");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid Zhipu API Key");
        }
        String id = parts[0];
        String secret = parts[1];

        try {
            // 1. Create Header
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("sign_type", "SIGN");
            String headerJson = objectMapper.writeValueAsString(header);
            String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));

            // 2. Create Payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("api_key", id);
            payload.put("exp", System.currentTimeMillis() + EXPIRATION_SECONDS * 1000);
            payload.put("timestamp", System.currentTimeMillis());
            String payloadJson = objectMapper.writeValueAsString(payload);
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            // 3. Generate Signature
            String dataToSign = encodedHeader + "." + encodedPayload;
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] signatureBytes = sha256_HMAC.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            String encodedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

            // 4. Assemble Token
            return dataToSign + "." + encodedSignature;

        } catch (JsonProcessingException | java.security.NoSuchAlgorithmException |
                 java.security.InvalidKeyException e) {
            throw new RuntimeException("Failed to generate Zhipu AI token", e);
        }
    }
}
