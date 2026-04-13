package com.coactivity.controller.advice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coactivity.controller.dto.request.RoomSort;
import com.coactivity.domain.RequestStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DisplayName("GlobalExceptionHandler web tests")
class GlobalExceptionHandlerWebMvcTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void invalidEnumRequestParamReturnsBadRequestInsteadOfInternalServerError() throws Exception {
    mockMvc.perform(get("/test/sort").param("sortBy", "BROKEN"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Validation failed"))
        .andExpect(jsonPath("$.instance").value("/test/sort"));
  }

  @Test
  void invalidEnumRequestBodyReturnsBadRequestInsteadOfInternalServerError() throws Exception {
    mockMvc.perform(post("/test/action")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "action": "BROKEN"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Validation failed"))
        .andExpect(jsonPath("$.instance").value("/test/action"));
  }

  @RestController
  @Validated
  static class TestController {

    @GetMapping("/test/sort")
    String getBySort(@RequestParam RoomSort sortBy) {
      return sortBy.name();
    }

    @PostMapping("/test/action")
    String processAction(@Valid @RequestBody ActionRequest request) {
      return request.action().name();
    }
  }

  record ActionRequest(@NotNull RequestStatus action) {
  }
}
