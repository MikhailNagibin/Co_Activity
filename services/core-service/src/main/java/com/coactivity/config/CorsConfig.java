package com.coactivity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Configures CORS for browser clients during local frontend development.
 * Uses origin patterns (not only exact origins) so {@code http://localhost:*} matches any Vite dev port.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

  private final String[] allowedOriginPatterns;

  public CorsConfig(@Value("${app.cors.allowed-origins:"
      + "http://localhost:*,"
      + "http://127.0.0.1:*}") String allowedOriginPatterns) {
    this.allowedOriginPatterns = Arrays.stream(allowedOriginPatterns.split(","))
        .map(String::trim)
        .filter(origin -> !origin.isEmpty())
        .toArray(String[]::new);
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOriginPatterns(allowedOriginPatterns)
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .maxAge(3600);
  }
}
