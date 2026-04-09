package com.coactivity.auth;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coactivity.TestcontainersConfiguration;
import com.coactivity.service.NotificationService;
import com.coactivity.support.AbstractSessionWebIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "app.auth.challenge.resend-cooldown=1s"
})
@Tag("docker")
@DisplayName("Auth session integration tests")
class AuthSessionIntegrationTest extends AbstractSessionWebIntegrationTest {

  @MockitoBean
  private NotificationService notificationService;

  @BeforeEach
  void setUp() throws Exception {
    resetState();
    when(notificationService.sendRegistrationVerificationCode(anyString(), anyString()))
        .thenReturn(true);
    when(notificationService.sendPasswordResetCode(anyString(), anyString()))
        .thenReturn(true);
  }

  @Test
  void registerVerifyLoginMeAndLogoutFlowWorks() throws Exception {
    CsrfContext csrf = fetchCsrf();
    String email = "register-flow@example.com";
    String password = "Password123";

    mockMvc.perform(post("/api/auth/register")
            .cookie(csrf.cookie())
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType("application/json")
            .content("""
                {
                  "email": "register-flow@example.com",
                  "userName": "registerFlow",
                  "password": "Password123",
                  "dateOfBirth": "2000-01-01T00:00:00Z"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"));

    ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
    verify(notificationService).sendRegistrationVerificationCode(anyString(), codeCaptor.capture());

    mockMvc.perform(post("/api/auth/register/verify")
            .cookie(csrf.cookie())
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(
                java.util.Map.of("email", email, "code", codeCaptor.getValue()))))
        .andExpect(status().isNoContent());

    Cookie sessionCookie = login(email, password, csrf, null);

    mockMvc.perform(get("/api/auth/me").cookie(sessionCookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.username").value("registerFlow"));

    mockMvc.perform(post("/api/auth/logout")
            .cookie(csrf.cookie(), sessionCookie)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/auth/me").cookie(sessionCookie))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void loginWorksWithoutPreexistingSession() throws Exception {
    createActiveUser("fresh-login@example.com", "freshLogin", "Password123");
    CsrfContext csrf = fetchCsrf();

    Cookie sessionCookie = login("fresh-login@example.com", "Password123", csrf, null);

    mockMvc.perform(get("/api/auth/me").cookie(sessionCookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("fresh-login@example.com"));
  }

  @Test
  void resendRegistrationCodeRejectsImmediateRepeatRequests() throws Exception {
    CsrfContext csrf = fetchCsrf();

    mockMvc.perform(post("/api/auth/register")
            .cookie(csrf.cookie())
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType("application/json")
            .content("""
                {
                  "email": "resend-cooldown@example.com",
                  "userName": "resendCooldown",
                  "password": "Password123",
                  "dateOfBirth": "2000-01-01T00:00:00Z"
                }
                """))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/auth/register/resend")
            .cookie(csrf.cookie())
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType("application/json")
            .content("""
                {
                  "email": "resend-cooldown@example.com"
                }
                """))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("REGISTRATION_CODE_RESEND_COOLDOWN"));
  }

  @Test
  void resendRegistrationCodeInvalidatesPreviousCodeAfterCooldownExpires() throws Exception {
    CsrfContext csrf = fetchCsrf();
    String email = "resend-flow@example.com";

    mockMvc.perform(post("/api/auth/register")
            .cookie(csrf.cookie())
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType("application/json")
            .content("""
                {
                  "email": "resend-flow@example.com",
                  "userName": "resendFlow",
                  "password": "Password123",
                  "dateOfBirth": "2000-01-01T00:00:00Z"
                }
                """))
        .andExpect(status().isCreated());

    ArgumentCaptor<String> firstCodeCaptor = ArgumentCaptor.forClass(String.class);
    verify(notificationService).sendRegistrationVerificationCode(anyString(), firstCodeCaptor.capture());

    reset(notificationService);
    when(notificationService.sendRegistrationVerificationCode(anyString(), anyString()))
        .thenReturn(true);

    Thread.sleep(1100L);

    mockMvc.perform(post("/api/auth/register/resend")
            .cookie(csrf.cookie())
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType("application/json")
            .content("""
                {
                  "email": "resend-flow@example.com"
                }
                """))
        .andExpect(status().isNoContent());

    ArgumentCaptor<String> secondCodeCaptor = ArgumentCaptor.forClass(String.class);
    verify(notificationService).sendRegistrationVerificationCode(anyString(), secondCodeCaptor.capture());
    assertNotEquals(firstCodeCaptor.getValue(), secondCodeCaptor.getValue());

    mockMvc.perform(post("/api/auth/register/verify")
            .cookie(csrf.cookie())
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(
                java.util.Map.of("email", email, "code", firstCodeCaptor.getValue()))))
        .andExpect(status().isForbidden());

    mockMvc.perform(post("/api/auth/register/verify")
            .cookie(csrf.cookie())
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(
                java.util.Map.of("email", email, "code", secondCodeCaptor.getValue()))))
        .andExpect(status().isNoContent());
  }

  @Test
  void resendRegistrationCodeRejectsAlreadyVerifiedAccount() throws Exception {
    CsrfContext csrf = fetchCsrf();
    String email = "verified-resend@example.com";

    mockMvc.perform(post("/api/auth/register")
            .cookie(csrf.cookie())
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType("application/json")
            .content("""
                {
                  "email": "verified-resend@example.com",
                  "userName": "verifiedResend",
                  "password": "Password123",
                  "dateOfBirth": "2000-01-01T00:00:00Z"
                }
                """))
        .andExpect(status().isCreated());

    ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
    verify(notificationService).sendRegistrationVerificationCode(anyString(), codeCaptor.capture());

    mockMvc.perform(post("/api/auth/register/verify")
            .cookie(csrf.cookie())
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(
                java.util.Map.of("email", email, "code", codeCaptor.getValue()))))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/auth/register/resend")
            .cookie(csrf.cookie())
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType("application/json")
            .content("""
                {
                  "email": "verified-resend@example.com"
                }
                """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_VERIFIED"));
  }

  @Test
  void loginReplacesExistingAuthenticatedSession() throws Exception {
    createActiveUser("session-replace@example.com", "sessionReplace", "Password123");
    CsrfContext csrf = fetchCsrf();

    Cookie firstSession = login("session-replace@example.com", "Password123", csrf, null);
    Cookie secondSession = login("session-replace@example.com", "Password123", csrf, firstSession);

    assertNotEquals(firstSession.getValue(), secondSession.getValue());

    mockMvc.perform(get("/api/auth/me").cookie(firstSession))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(get("/api/auth/me").cookie(secondSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("sessionReplace"));
  }

  @Test
  void passwordChangeInvalidatesAllActiveSessions() throws Exception {
    createActiveUser("password-change@example.com", "passwordChange", "Password123");
    CsrfContext csrf = fetchCsrf();

    Cookie firstSession = login("password-change@example.com", "Password123", csrf, null);
    Cookie secondSession = login("password-change@example.com", "Password123", csrf, null);

    mockMvc.perform(post("/api/auth/password/change")
            .cookie(csrf.cookie(), firstSession)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType("application/json")
            .content("""
                {
                  "currentPassword": "Password123",
                  "newPassword": "Password456"
                }
                """))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/auth/me").cookie(firstSession))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(get("/api/auth/me").cookie(secondSession))
        .andExpect(status().isUnauthorized());

    Cookie newSession = login("password-change@example.com", "Password456", csrf, null);
    mockMvc.perform(get("/api/auth/me").cookie(newSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("passwordChange"));
  }

  @Test
  void passwordResetFlowInvalidatesExistingSessionsAndAllowsNewPassword() throws Exception {
    createActiveUser("password-reset@example.com", "passwordReset", "Password123");
    CsrfContext csrf = fetchCsrf();

    Cookie firstSession = login("password-reset@example.com", "Password123", csrf, null);
    Cookie secondSession = login("password-reset@example.com", "Password123", csrf, null);

    mockMvc.perform(post("/api/auth/password/reset/request")
            .cookie(csrf.cookie())
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType("application/json")
            .content("""
                {
                  "email": "password-reset@example.com"
                }
                """))
        .andExpect(status().isNoContent());

    ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
    verify(notificationService).sendPasswordResetCode(anyString(), codeCaptor.capture());

    mockMvc.perform(post("/api/auth/password/reset/verify")
            .cookie(csrf.cookie())
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(
                java.util.Map.of(
                    "email", "password-reset@example.com",
                    "code", codeCaptor.getValue()))))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/auth/password/reset/confirm")
            .cookie(csrf.cookie())
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(
                java.util.Map.of(
                    "email", "password-reset@example.com",
                    "code", codeCaptor.getValue(),
                    "newPassword", "Password456"))))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/auth/me").cookie(firstSession))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(get("/api/auth/me").cookie(secondSession))
        .andExpect(status().isUnauthorized());

    Cookie newSession = login("password-reset@example.com", "Password456", csrf, null);
    mockMvc.perform(get("/api/auth/me").cookie(newSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("passwordReset"));
  }
}
