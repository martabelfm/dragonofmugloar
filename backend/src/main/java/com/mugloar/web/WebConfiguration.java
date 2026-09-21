package com.mugloar.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The documented run modes (Angular dev-server proxy, nginx in Docker) both reach the API
 * same-origin, so this mapping is not exercised by them. It exists for ad-hoc use: opening
 * Swagger UI or calling the API directly from a page served on the Angular dev port.
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("GET", "POST", "PUT", "OPTIONS")
                .maxAge(3600);
    }
}
