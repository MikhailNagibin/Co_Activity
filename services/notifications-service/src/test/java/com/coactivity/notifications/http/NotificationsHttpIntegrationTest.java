package com.coactivity.notifications.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coactivity.notifications.advice.GlobalExceptionHandler;
import com.coactivity.notifications.controller.NotificationsController;
import com.coactivity.notifications.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = NotificationsController.class)
@Import(GlobalExceptionHandler.class)
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("Notifications HTTP integration tests")
class NotificationsHttpIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private EmailService emailService;

  @Test
  @DisplayName("POST /api/notifications/email should return 204")
  void sendEmailReturnsNoContent() throws Exception {
    mockMvc.perform(post("/api/notifications/email")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "to": "student@example.com",
                  "subject": "Smoke test",
                  "body": "Notifications service works"
                }
                """))
        .andExpect(status().isNoContent());

    verify(emailService).sendEmail(any());
  }

  @Test
  @DisplayName("Unexpected exceptions should be logged and returned as 500")
  void unexpectedExceptionsAreLogged(CapturedOutput output) throws Exception {
    doThrow(new IllegalStateException("boom"))
        .when(emailService)
        .sendEmail(any());

    mockMvc.perform(post("/api/notifications/email")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "to": "student@example.com",
                  "subject": "Smoke test",
                  "body": "Notifications service works"
                }
                """))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message").value("Internal server error"));

    org.assertj.core.api.Assertions.assertThat(output)
        .contains("Unexpected error while processing POST /api/notifications/email")
        .contains("boom");
  }
}
