package com.coactivity.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coactivity.auth.model.UserStatus;
import com.coactivity.persistence.entity.UserEntity;
import com.coactivity.persistence.repository.UserJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public abstract class AbstractSessionWebIntegrationTest {

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  protected UserJpaRepository userJpaRepository;

  @Autowired
  protected PasswordEncoder passwordEncoder;

  @Autowired
  protected DataSource dataSource;

  @Autowired
  protected StringRedisTemplate stringRedisTemplate;

  @Value("${app.storage.local.root}")
  private String storageRoot;

  protected record CsrfContext(String token, Cookie cookie) {
  }

  protected void resetState() throws SQLException {
    SecurityContextHolder.clearContext();
    TestSecurityContextHolder.clearContext();
    cleanupTables();
    flushRedis();
    cleanupStorage();
  }

  protected UserEntity createActiveUser(String email, String username, String rawPassword) {
    UserEntity user = new UserEntity();
    user.setEmail(email);
    user.setEmailNormalized(email.trim().toLowerCase(java.util.Locale.ROOT));
    user.setUserName(username);
    user.setPasswordHash(passwordEncoder.encode(rawPassword));
    user.setStatus(UserStatus.ACTIVE);
    user.setEmailVerifiedAt(Instant.now());
    return userJpaRepository.saveAndFlush(user);
  }

  protected CsrfContext fetchCsrf() throws Exception {
    MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
        .andExpect(status().isOk())
        .andReturn();

    Cookie csrfCookie = Objects.requireNonNull(result.getResponse().getCookie("XSRF-TOKEN"));
    JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
    Objects.requireNonNull(payload.get("token"));
    return new CsrfContext(csrfCookie.getValue(), csrfCookie);
  }

  protected CsrfContext csrfContextFromCookie(Cookie csrfCookie) {
    return new CsrfContext(csrfCookie.getValue(), csrfCookie);
  }

  protected Cookie login(String email, String password, CsrfContext csrf,
      Cookie existingSessionCookie) throws Exception {
    MockHttpServletRequestBuilder request = post("/api/auth/login")
        .contentType("application/json")
        .header("X-XSRF-TOKEN", csrf.token())
        .content(objectMapper.writeValueAsString(
            java.util.Map.of("email", email, "password", password)));

    List<Cookie> cookies = new ArrayList<>();
    cookies.add(csrf.cookie());
    if (existingSessionCookie != null) {
      cookies.add(existingSessionCookie);
    }
    request.cookie(cookies.toArray(Cookie[]::new));

    MvcResult result = mockMvc.perform(request)
        .andExpect(status().isOk())
        .andReturn();

    return Objects.requireNonNull(result.getResponse().getCookie("COACTIVITY_SESSION"));
  }

  protected Path resolveStoragePath(String storageKey) {
    return Path.of(storageRoot)
        .toAbsolutePath()
        .normalize()
        .resolve(storageKey)
        .normalize();
  }

  private void cleanupTables() throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             TRUNCATE TABLE
               bulletin_board,
               user_notifications,
               answers,
               questions,
               bans,
               room_requests,
               room_members,
               pictures,
               user_avatars,
               rooms,
               users
             RESTART IDENTITY CASCADE
             """)) {
      statement.executeUpdate();
    }
  }

  private void flushRedis() {
    try (RedisConnection connection =
             Objects.requireNonNull(stringRedisTemplate.getConnectionFactory()).getConnection()) {
      connection.serverCommands().flushDb();
    }
  }

  private void cleanupStorage() {
    try {
      Path rootPath = Path.of(storageRoot).toAbsolutePath().normalize();
      if (!Files.exists(rootPath)) {
        return;
      }
      Files.walk(rootPath)
          .sorted((left, right) -> right.getNameCount() - left.getNameCount())
          .forEach(path -> {
            if (path.equals(rootPath)) {
              return;
            }
            try {
              Files.deleteIfExists(path);
            } catch (IOException ex) {
              throw new IllegalStateException("Failed to cleanup test storage", ex);
            }
          });
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to cleanup test storage", ex);
    }
  }
}
