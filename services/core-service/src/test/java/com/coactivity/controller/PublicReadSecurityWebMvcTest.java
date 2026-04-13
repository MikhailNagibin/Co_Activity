package com.coactivity.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coactivity.config.SecurityConfig;
import com.coactivity.controller.dto.request.RoomFilter;
import com.coactivity.controller.dto.response.QuestionResponse;
import com.coactivity.controller.dto.response.QuestionWithAnswersResponse;
import com.coactivity.controller.dto.response.RoomDetailedResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.controller.impl.QAControllerImpl;
import com.coactivity.controller.impl.RoomControllerImpl;
import com.coactivity.domain.Category;
import com.coactivity.security.CurrentUserDetailsService;
import com.coactivity.security.RestAccessDeniedHandler;
import com.coactivity.security.RestAuthenticationEntryPoint;
import com.coactivity.service.BulletinBoardService;
import com.coactivity.service.QaService;
import com.coactivity.service.RoomImageService;
import com.coactivity.service.RoomMembershipService;
import com.coactivity.service.RoomService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {RoomControllerImpl.class, QAControllerImpl.class})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableMethodSecurity
@DisplayName("Public read security web tests")
class PublicReadSecurityWebMvcTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private RoomService roomService;

  @MockitoBean
  private RoomMembershipService roomMembershipService;

  @MockitoBean
  private BulletinBoardService bulletinBoardService;

  @MockitoBean
  private RoomImageService roomImageService;

  @MockitoBean
  private QaService qaService;

  @MockitoBean
  private CurrentUserDetailsService currentUserDetailsService;

  @Test
  void anonymousUserCanViewPublicRoomsCatalog() throws Exception {
    when(roomService.getRooms(isNull(), any(RoomFilter.class), isNull()))
        .thenReturn(List.of(new RoomSummaryResponse(
            42,
            true,
            true,
            Category.ART,
            "Open room",
            "Public description",
            Instant.parse("2026-04-07T10:00:00Z"),
            Instant.parse("2026-04-07T12:00:00Z"),
            12,
            null,
            3,
            20,
            new UserSummaryResponse(7, "owner", null, null, null, null, null, null),
            false,
            List.of())));

    mockMvc.perform(get("/api/rooms").with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(42))
        .andExpect(jsonPath("$[0].name").value("Open room"));

    verify(roomService).getRooms(isNull(), any(RoomFilter.class), isNull());
  }

  @Test
  void anonymousUserCanViewRoomDetailsAndControllerPassesNullUserId() throws Exception {
    RoomDetailedResponse room = new RoomDetailedResponse();
    room.setId(42);
    room.setActive(true);
    room.setIsPublic(true);
    room.setCategory(Category.ART);
    room.setName("Open room");
    room.setDescription("Public description");
    room.setDateOfStartEvent(Instant.parse("2026-04-07T10:00:00Z"));
    room.setDateOfEndEvent(Instant.parse("2026-04-07T12:00:00Z"));
    room.setAgeRating(12);
    room.setParticipantCount(3);
    room.setMaximumParticipants(20);
    room.setCreator(new UserSummaryResponse(7, "owner", null, null, null, null, null, null));
    room.setIsCurrentUserParticipant(false);
    room.setImageIds(List.of());
    room.setImages(List.of());
    room.setHasProtectedAccess(false);

    when(roomService.getRoomById(42, null))
        .thenReturn(room);

    mockMvc.perform(get("/api/rooms/42").with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(42))
        .andExpect(jsonPath("$.name").value("Open room"));

    verify(roomService).getRoomById(42, null);
  }

  @Test
  void anonymousUserCanViewAllQuestions() throws Exception {
    when(qaService.getQuestions(null))
        .thenReturn(List.of(new QuestionResponse(5, Category.ART, "What is this?",
            new UserSummaryResponse(7, "author", null, null, null, null, null, null))));

    mockMvc.perform(get("/api/qa/questions").with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(5))
        .andExpect(jsonPath("$[0].question").value("What is this?"));

    verify(qaService).getQuestions(null);
  }

  @Test
  void anonymousUserCanViewQuestionsByCategory() throws Exception {
    when(qaService.getQuestions(3)).thenReturn(List.of());

    mockMvc.perform(get("/api/qa/questions/category").with(anonymous()).param("categoryId", "3"))
        .andExpect(status().isOk());

    verify(qaService).getQuestions(3);
  }

  @Test
  void anonymousUserCanViewQuestionThread() throws Exception {
    when(qaService.getQuestionWithAnswers(9))
        .thenReturn(new QuestionWithAnswersResponse(
            new QuestionResponse(9, Category.ART, "Question",
                new UserSummaryResponse(7, "author", null, null, null, null, null, null)),
            List.of()));

    mockMvc.perform(get("/api/qa/questions/9").with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.question.id").value(9));

    verify(qaService).getQuestionWithAnswers(9);
  }

  @Test
  void anonymousUserCannotViewOwnRoomsEndpoint() throws Exception {
    mockMvc.perform(get("/api/rooms/me").with(anonymous()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"))
        .andExpect(jsonPath("$.detail").value("Authentication is required"))
        .andExpect(jsonPath("$.instance").value("/api/rooms/me"))
        .andExpect(jsonPath("$.type").value("urn:coactivity:error:AUTH_REQUIRED"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.title").value("Unauthorized"))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.traceId").exists())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

    verifyNoInteractions(roomMembershipService);
  }

  @Test
  void authenticatedUserWithoutCsrfGetsProblemDetailForbidden() throws Exception {
    mockMvc.perform(post("/api/rooms/createRoom")
            .with(user("csrf-user"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "isPublic": true,
                  "category": "ART",
                  "name": "Open room",
                  "description": "Public description",
                  "maximumNumberOfPeople": 20
                }
                """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
        .andExpect(jsonPath("$.detail").value("Access is denied"))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

    verifyNoInteractions(roomService);
  }

  @Test
  void anonymousUserCannotCreateRoom() throws Exception {
    mockMvc.perform(post("/api/rooms/createRoom")
            .with(anonymous())
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "isPublic": true,
                  "category": "ART",
                  "name": "Open room",
                  "description": "Public description",
                  "maximumNumberOfPeople": 20
                }
                """))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(roomService);
  }

  @Test
  void anonymousUserCannotJoinRoom() throws Exception {
    mockMvc.perform(post("/api/rooms/42/join").with(anonymous()).with(csrf()))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(roomMembershipService);
  }

  @Test
  void anonymousUserCannotManageParticipants() throws Exception {
    mockMvc.perform(get("/api/rooms/42/participants").with(anonymous()))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(roomMembershipService);
  }

  @Test
  void anonymousUserCannotAskQuestion() throws Exception {
    mockMvc.perform(post("/api/qa/questions")
            .with(anonymous())
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "category": "ART",
                  "question": "Can I ask anonymously?"
                }
                """))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(qaService);
  }
}
