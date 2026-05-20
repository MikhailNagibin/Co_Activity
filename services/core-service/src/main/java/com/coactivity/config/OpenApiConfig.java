package com.coactivity.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI openAPI(
      @Value("${spring.application.name:core-service}") String applicationName,
      @Value("${app.openapi.title:CoActivity API}") String apiTitle,
      @Value("${app.openapi.version:1.0.0}") String apiVersion,
      @Value("${server.servlet.session.cookie.name:COACTIVITY_SESSION}") String sessionCookieName) {
    return new OpenAPI()
        .info(new Info()
            .title(apiTitle + " (" + applicationName + ")")
            .version(apiVersion))
        .components(new Components()
            .addSecuritySchemes("sessionCookie", new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name(sessionCookieName)
                .description("HTTP session cookie. Issued on login; sent automatically by the browser."))
            .addSecuritySchemes("csrfToken", new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-XSRF-TOKEN")
                .description("CSRF header value for state-changing requests. Token is exposed via cookie XSRF-TOKEN and must be echoed in header X-XSRF-TOKEN.")));
  }
}
