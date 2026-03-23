package com.coactivity.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coactivity.controller.advice.GlobalExceptionHandler;
import com.coactivity.controller.dto.request.RoomFilter;
import com.coactivity.controller.dto.request.RoomSort;
import com.coactivity.controller.dto.response.QuestionResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.controller.impl.QAControllerImpl;
import com.coactivity.controller.impl.RoomControllerImpl;
import com.coactivity.controller.impl.UserControllerImpl;
import com.coactivity.domain.Category;
import com.coactivity.service.AuthService;
import com.coactivity.service.BulletinBoardService;
import com.coactivity.service.JoinRequestService;
import com.coactivity.service.QaGatewayService;
import com.coactivity.service.RoomMembershipService;
import com.coactivity.service.RoomService;
import com.coactivity.service.TokenService;
import com.coactivity.service.UserProfileService;
import com.coactivity.service.dto.TokenPayload;
import com.coactivity.service.exception.NotificationDeliveryException;
import com.coactivity.service.exception.QaServiceUnavailableException;
import com.coactivity.service.exception.ValidationException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
    RoomControllerImpl.class,
    QAControllerImpl.class,
    UserControllerImpl.class
})
@Import(GlobalExceptionHandler.class)
@DisplayName("Core HTTP smoke tests")
class CoreHttpSmokeTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private RoomService roomService;

  @MockBean
  private TokenService tokenService;

  @MockBean
  private RoomMembershipService roomMembershipService;

  @MockBean
  private BulletinBoardService bulletinBoardService;

  @MockBean
  private QaGatewayService qaGatewayService;

  @MockBean
  private UserProfileService userProfileService;

  @MockBean
  private AuthService authService;

  @MockBean
  private JoinRequestService joinRequestService;

  @Test
  @DisplayName("GET /api/rooms should return 200 and empty list")
  void getRoomsReturnsOkAndEmptyList() throws Exception {
    when(roomService.getRooms(nullable(Integer.class), any(RoomFilter.class),
        nullable(RoomSort.class))).thenReturn(List.of());

    mockMvc.perform(get("/api/rooms"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  @DisplayName("GET /api/qa/questions should return proxied question list")
  void getQuestionsReturnsQaProxyResponse() throws Exception {
    UserSummaryResponse author = new UserSummaryResponse(
        7,
        "Alice",
        Instant.parse("2000-01-01T00:00:00Z"),
        "Moscow",
        "Russia",
        "Music teacher",
        1);
    QuestionResponse question = new QuestionResponse(
        1,
        Category.MUSIC,
        "What guitar should I buy?",
        author);

    when(qaGatewayService.getAllQuestions()).thenReturn(List.of(question));

    mockMvc.perform(get("/api/qa/questions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].question").value("What guitar should I buy?"))
        .andExpect(jsonPath("$[0].category").value("MUSIC"))
        .andExpect(jsonPath("$[0].author.userName").value("Alice"));
  }

  @Test
  @DisplayName("GET /api/users/me without token should return 401 instead of 500")
  void getUserProfileWithoutTokenReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Authorization token is required"));

    verifyNoInteractions(userProfileService);
  }

  @Test
  @DisplayName("POST /api/rooms/createRoom without token should return 401 instead of 500")
  void createRoomWithoutTokenReturnsUnauthorized() throws Exception {
    mockMvc.perform(post("/api/rooms/createRoom")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "isPublic": true,
                  "category": "Sport",
                  "name": "Morning Run",
                  "description": "HTTP smoke test room",
                  "maximumNumberOfPeople": 5,
                  "chatLink": "https://example.com/chat",
                  "dateOfStartEvent": "2030-01-01T10:00:00Z",
                  "dateOfEndEvent": "2030-01-01T12:00:00Z",
                  "frequency": null,
                  "ageRating": 18
                }
                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Authorization token is required"));

    verifyNoInteractions(roomService);
  }

  @Test
  @DisplayName("POST /api/rooms/createRoom should return 400 when category is invalid")
  void createRoomReturnsBadRequestWhenCategoryIsInvalid() throws Exception {
    when(tokenService.isTokenActive("valid-token")).thenReturn(true);
    when(tokenService.decodeToken("valid-token"))
        .thenReturn(new TokenPayload(1, Instant.parse("2030-01-01T00:00:00Z")));
    doThrow(new ValidationException("Category not found: UnknownCategory"))
        .when(roomService)
        .createRoom(any(), any());

    mockMvc.perform(post("/api/rooms/createRoom")
            .header("Authorization", "Bearer valid-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "isPublic": true,
                  "category": "UnknownCategory",
                  "name": "Morning Run",
                  "description": "HTTP smoke test room",
                  "maximumNumberOfPeople": 5,
                  "chatLink": "https://example.com/chat",
                  "dateOfStartEvent": "2030-01-01T10:00:00Z",
                  "dateOfEndEvent": "2030-01-01T12:00:00Z",
                  "frequency": null,
                  "ageRating": 18
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Category not found: UnknownCategory"));
  }

  @Test
  @DisplayName("POST /api/users/login should return 202 when verification flow is accepted")
  void loginReturnsAcceptedWhenVerificationFlowStarts() throws Exception {
    mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "login": "student@example.com",
                  "password": "Password123"
                }
                """))
        .andExpect(status().isAccepted());

    verify(authService).loginUser(any());
  }

  @Test
  @DisplayName("POST /api/users/login should return 503 when verification code cannot be delivered")
  void loginReturnsServiceUnavailableWhenVerificationCodeDeliveryFails() throws Exception {
    doThrow(new NotificationDeliveryException("Unable to deliver verification code"))
        .when(authService)
        .loginUser(any());

    mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "login": "student@example.com",
                  "password": "Password123"
                }
                """))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message").value("Unable to deliver verification code"));
  }

  @Test
  @DisplayName("GET /api/qa/questions should return 503 when qa-service is unavailable")
  void getQuestionsReturnsServiceUnavailableWhenQaServiceIsDown() throws Exception {
    when(qaGatewayService.getAllQuestions())
        .thenThrow(new QaServiceUnavailableException("QA service is unavailable"));

    mockMvc.perform(get("/api/qa/questions"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message").value("QA service is unavailable"));
  }
}
