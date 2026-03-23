package com.coactivity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

  private final String[] allowedOrigins;

  public CorsConfig(@Value("${app.cors.allowed-origins:http://localhost:*}") String allowedOrigins) {
    this.allowedOrigins = allowedOrigins
        .trim()
        .isEmpty()
        ? new String[0]
        : allowedOrigins.split("\\s*,\\s*");
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    String[] patterns =
        allowedOrigins.length == 0 ? new String[] {"http://localhost:*"} : allowedOrigins;
    registry.addMapping("/api/**")
        .allowedOriginPatterns(patterns)
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .exposedHeaders("Authorization")
        .allowCredentials(false)
        .maxAge(3600);
    // Health checks from browser / tooling (not under /api)
    registry.addMapping("/actuator/**")
        .allowedOriginPatterns(patterns)
        .allowedMethods("GET", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false)
        .maxAge(3600);
  }
}
