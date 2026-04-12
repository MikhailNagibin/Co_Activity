package com.coactivity.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coactivity.TestcontainersConfiguration;
import com.coactivity.persistence.entity.UserEntity;
import com.coactivity.support.AbstractSessionWebIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Tag("docker")
@DisplayName("Q&A lifecycle integration tests")
class QaLifecycleIntegrationTest extends AbstractSessionWebIntegrationTest {

  @BeforeEach
  void setUp() throws Exception {
    resetState();
  }

  @Test
  void questionsCanBeSearchedByKeywordAndCategory() throws Exception {
    UserEntity author = createActiveUser("qa-search@example.com", "qaSearch", "Password123");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(author.getEmail(), "Password123", csrf, null);

    createQuestion(session, csrf, "SPORT", "How should I choose running shoes?");
    createQuestion(session, csrf, "ART", "Which brushes work best for watercolor?");

    mockMvc.perform(get("/api/qa/questions")
            .param("query", "running")
            .param("categoryId", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].question").value("How should I choose running shoes?"))
        .andExpect(jsonPath("$[0].category").value("SPORT"));

    mockMvc.perform(get("/api/qa/questions")
            .param("query", "running")
            .param("categoryId", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void onlyOwnersCanEditAndDeleteQuestionsAndAnswers() throws Exception {
    UserEntity author = createActiveUser("qa-owner@example.com", "qaOwner", "Password123");
    UserEntity other = createActiveUser("qa-other@example.com", "qaOther", "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie authorSession = login(author.getEmail(), "Password123", csrf, null);
    Cookie otherSession = login(other.getEmail(), "Password123", csrf, null);

    Integer questionId = createQuestion(authorSession, csrf, "SPORT", "Original question?");
    Integer answerId = createAnswer(authorSession, csrf, questionId, "Original answer");

    mockMvc.perform(put("/api/qa/questions/" + questionId)
            .cookie(csrf.cookie(), otherSession)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "category": "SPORT",
                  "question": "Hijacked question?"
                }
                """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Cannot update question created by another user"));

    mockMvc.perform(put("/api/qa/answers/" + answerId)
            .cookie(csrf.cookie(), otherSession)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "answer": "Hijacked answer"
                }
                """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Cannot update answer created by another user"));

    mockMvc.perform(put("/api/qa/questions/" + questionId)
            .cookie(csrf.cookie(), authorSession)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "category": "ART",
                  "question": "Updated question?"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.question").value("Updated question?"))
        .andExpect(jsonPath("$.category").value("ART"));

    mockMvc.perform(put("/api/qa/answers/" + answerId)
            .cookie(csrf.cookie(), authorSession)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "answer": "Updated answer"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.answer").value("Updated answer"));

    mockMvc.perform(delete("/api/qa/answers/" + answerId)
            .cookie(csrf.cookie(), otherSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Cannot delete answer created by another user"));

    mockMvc.perform(delete("/api/qa/answers/" + answerId)
            .cookie(csrf.cookie(), authorSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/qa/questions/" + questionId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.answers").isEmpty());

    mockMvc.perform(delete("/api/qa/questions/" + questionId)
            .cookie(csrf.cookie(), otherSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Cannot delete question created by another user"));

    mockMvc.perform(delete("/api/qa/questions/" + questionId)
            .cookie(csrf.cookie(), authorSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/qa/questions/" + questionId))
        .andExpect(status().isNotFound());
  }

  private Integer createQuestion(Cookie session, CsrfContext csrf, String category, String question)
      throws Exception {
    MvcResult result = mockMvc.perform(post("/api/qa/questions")
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "category": "%s",
                  "question": "%s"
                }
                """.formatted(category, question)))
        .andExpect(status().isCreated())
        .andReturn();

    JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
    return payload.get("id").asInt();
  }

  private Integer createAnswer(Cookie session, CsrfContext csrf, Integer questionId, String answer)
      throws Exception {
    MvcResult result = mockMvc.perform(post("/api/qa/answers")
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "questionId": %d,
                  "answer": "%s"
                }
                """.formatted(questionId, answer)))
        .andExpect(status().isCreated())
        .andReturn();

    JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
    return payload.get("id").asInt();
  }
}
