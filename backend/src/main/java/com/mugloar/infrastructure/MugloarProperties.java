package com.mugloar.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties("mugloar")
public record MugloarProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {
    public MugloarProperties {
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://dragonsofmugloar.com";
        if (connectTimeout == null) connectTimeout = Duration.ofSeconds(3);
        if (readTimeout == null) readTimeout = Duration.ofSeconds(10);
    }
}
