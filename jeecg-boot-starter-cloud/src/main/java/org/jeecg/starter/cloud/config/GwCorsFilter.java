package org.jeecg.starter.cloud.config;

/**
 * @Description: 跨域设置 (升级SpringBoot2.6.6)
 * @author: zyf
 * @date: 2022/02/21
 * @version: V1.0
 */
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Arrays;

@Configuration
public class GwCorsFilter {

    /**
     * Allowed cross-origin patterns, comma separated (e.g. https://admin.example.com).
     * Must be configured explicitly when credentials are allowed. A wildcard "*"
     * combined with allowCredentials=true lets any website issue credentialed
     * requests and read the responses, so it is rejected here.
     */
    @Value("${jeecg.cors.allowed-origin-patterns:}")
    private String allowedOriginPatterns;

    @Bean
    public CorsWebFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(18000L);

        boolean hasExplicitOrigins = StringUtils.hasText(allowedOriginPatterns)
                && !"*".equals(allowedOriginPatterns.trim());
        if (hasExplicitOrigins) {
            // Explicit origin whitelist -> safe to allow credentials.
            Arrays.stream(allowedOriginPatterns.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(config::addAllowedOriginPattern);
            config.setAllowCredentials(true);
        } else {
            // No whitelist configured: allow any origin but WITHOUT credentials.
            // Reflecting an arbitrary Origin together with Access-Control-Allow-Credentials
            // would expose authenticated responses to any site (CWE-942).
            config.addAllowedOriginPattern("*");
            config.setAllowCredentials(false);
        }

        org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource source =
                new org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource(new PathPatternParser());
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
