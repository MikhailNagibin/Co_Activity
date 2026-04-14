package com.coactivity.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coactivity.TestcontainersConfiguration;
import com.coactivity.auth.model.UserStatus;
import com.coactivity.domain.RequestStatus;
import com.coactivity.persistence.entity.UserEntity;
import com.coactivity.repository.RoomRepository;
import com.coactivity.repository.RoomsRequestRepository;
import com.coactivity.service.NotificationService;
import com.coactivity.support.AbstractSessionWebIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Tag("docker")
@DisplayName("Complex user scenario E2E integration tests")
class ComplexUserScenarioIntegrationTest extends AbstractSessionWebIntegrationTest {

  private static final String PASSWORD = "Password123";

  @Autowired
  private RoomRepository roomRepository;

  @Autowired
  private RoomsRequestRepository roomsRequestRepository;

  @MockitoBean
  private NotificationService notificationService;

  @BeforeEach
  void setUp() throws Exception {
    resetState();
    reset(notificationService);
    when(notificationService.sendRegistrationVerificationCode(anyString(), anyString()))
        .thenReturn(true);
    when(notificationService.sendPasswordResetCode(anyString(), anyString()))
        .thenReturn(true);
  }

  @Test
  void massRegistrationVerificationLoginAndMeKeepIndependentSessions() throws Exception {
    Map<String, String> verificationCodesByEmail = new HashMap<>();
    doAnswer(invocation -> {
      String email = invocation.getArgument(0, String.class);
      String code = invocation.getArgument(1, String.class);
      verificationCodesByEmail.put(email, code);
      return true;
    }).when(notificationService).sendRegistrationVerificationCode(anyString(), anyString());

    List<NewUser> newUsers = List.of(
        new NewUser("mass-user-1@example.com", "massUser1"),
        new NewUser("mass-user-2@example.com", "massUser2"),
        new NewUser("mass-user-3@example.com", "massUser3"),
        new NewUser("mass-user-4@example.com", "massUser4"),
        new NewUser("mass-user-5@example.com", "massUser5")
    );

    for (NewUser user : newUsers) {
      CsrfContext csrf = fetchCsrf();

      mockMvc.perform(post("/api/auth/register")
              .cookie(csrf.cookie())
              .header("X-XSRF-TOKEN", csrf.token())
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                    "email": "%s",
                    "userName": "%s",
                    "password": "%s",
                    "dateOfBirth": "2000-01-01T00:00:00Z"
                  }
                  """.formatted(user.email(), user.username(), PASSWORD)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.email").value(user.email()))
          .andExpect(jsonPath("$.userName").value(user.username()))
          .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"));

      String verificationCode = verificationCodesByEmail.get(user.email());
      assertNotNull(verificationCode);

      mockMvc.perform(post("/api/auth/register/verify")
              .cookie(csrf.cookie())
              .header("X-XSRF-TOKEN", csrf.token())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(
                  Map.of("email", user.email(), "code", verificationCode))))
          .andExpect(status().isNoContent());
    }

    List<SessionUser> sessionUsers = newUsers.stream()
        .map(this::loginRegisteredUser)
        .toList();

    Set<String> sessionCookieValues = new HashSet<>();
    Set<String> csrfCookieValues = new HashSet<>();
    for (SessionUser user : sessionUsers) {
      assertTrue(sessionCookieValues.add(user.session().getValue()));
      assertTrue(csrfCookieValues.add(user.csrf().cookie().getValue()));

      mockMvc.perform(get("/api/auth/me").cookie(user.session()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(user.id()))
          .andExpect(jsonPath("$.email").value(user.email()))
          .andExpect(jsonPath("$.username").value(user.username()));

      UserEntity storedUser = userJpaRepository.findByEmailNormalized(
              user.email().toLowerCase(Locale.ROOT))
          .orElseThrow();
      assertEquals(UserStatus.ACTIVE, storedUser.getStatus());
      assertNotNull(storedUser.getEmailVerifiedAt());
    }

    assertEquals(newUsers.size(), userJpaRepository.count());
    assertEquals(newUsers.size(), sessionCookieValues.size());
    assertEquals(newUsers.size(), csrfCookieValues.size());
  }

  @Test
  void usersJoinPublicRoomAndAcceptedPrivateJoinRequestUpdatesMembershipState() throws Exception {
    SessionUser owner = createActiveUserAndLogin("room-owner@example.com", "roomOwner");
    SessionUser publicJoinerOne = createActiveUserAndLogin("room-public-b@example.com",
        "roomPublicB");
    SessionUser publicJoinerTwo = createActiveUserAndLogin("room-public-c@example.com",
        "roomPublicC");
    SessionUser privateRequester = createActiveUserAndLogin("room-private-d@example.com",
        "roomPrivateD");

    Integer publicRoomId = createRoom(owner, true, "Cross user public room",
        "https://chat.example.com/cross-public");
    Integer privateRoomId = createRoom(owner, false, "Cross user private room",
        "https://chat.example.com/cross-private");

    joinRoom(publicJoinerOne, publicRoomId);
    joinRoom(publicJoinerTwo, publicRoomId);

    mockMvc.perform(get("/api/rooms/" + publicRoomId + "/participants").cookie(owner.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[*].id",
            containsInAnyOrder(owner.id(), publicJoinerOne.id(), publicJoinerTwo.id())))
        .andExpect(jsonPath("$[?(@.id == %d)].role".formatted(owner.id()))
            .value(hasItem("OWNER")))
        .andExpect(jsonPath("$[?(@.id == %d)].role".formatted(publicJoinerOne.id()))
            .value(hasItem("PARTICIPANT")))
        .andExpect(jsonPath("$[?(@.id == %d)].role".formatted(publicJoinerTwo.id()))
            .value(hasItem("PARTICIPANT")));

    expectMembership(owner, publicRoomId, "PARTICIPANT", "OWNER", false);
    expectMembership(publicJoinerOne, publicRoomId, "PARTICIPANT", "PARTICIPANT", false);
    expectMembership(publicJoinerTwo, publicRoomId, "PARTICIPANT", "PARTICIPANT", false);

    joinRoom(privateRequester, privateRoomId);

    MvcResult pendingResult = mockMvc.perform(get(
            "/api/users/rooms/" + privateRoomId + "/requests/pending")
            .cookie(owner.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].userId").value(privateRequester.id()))
        .andExpect(jsonPath("$[0].username").value(privateRequester.username()))
        .andExpect(jsonPath("$[0].status").value("CONSIDERATION"))
        .andReturn();

    Integer requestId = objectMapper.readTree(pendingResult.getResponse().getContentAsString())
        .get(0)
        .get("requestId")
        .asInt();

    mockMvc.perform(get("/api/rooms/" + privateRoomId + "/membership/status")
            .cookie(privateRequester.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.role").value(nullValue()))
        .andExpect(jsonPath("$.pendingRequestId").value(requestId))
        .andExpect(jsonPath("$.canJoin").value(false));

    mockMvc.perform(post("/api/users/requests/" + requestId)
            .cookie(owner.csrf().cookie(), owner.session())
            .header("X-XSRF-TOKEN", owner.csrf().token())
            .param("action", "ACCEPTED"))
        .andExpect(status().isNoContent());

    assertTrue(roomRepository.isUserInMembers(privateRoomId, privateRequester.id()));
    assertEquals(RequestStatus.ACCEPTED, roomsRequestRepository
        .getRequestByUserAndRoom(privateRequester.id(), privateRoomId)
        .getStatus());

    mockMvc.perform(get("/api/users/rooms/" + privateRoomId + "/requests/pending")
            .cookie(owner.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", empty()));

    mockMvc.perform(get("/api/rooms/" + privateRoomId + "/participants").cookie(owner.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[*].id",
            containsInAnyOrder(owner.id(), privateRequester.id())))
        .andExpect(jsonPath("$[?(@.id == %d)].role".formatted(privateRequester.id()))
            .value(hasItem("PARTICIPANT")));

    expectMembership(owner, privateRoomId, "PARTICIPANT", "OWNER", false);
    expectMembership(privateRequester, privateRoomId, "PARTICIPANT", "PARTICIPANT", false);
  }

  @Test
  void qaQuestionsAndAnswersFromDifferentUsersRespectOwnershipAndSearch() throws Exception {
    SessionUser authorA = createActiveUserAndLogin("qa-author-a@example.com", "qaAuthorA");
    SessionUser authorB = createActiveUserAndLogin("qa-author-b@example.com", "qaAuthorB");
    SessionUser authorC = createActiveUserAndLogin("qa-author-c@example.com", "qaAuthorC");

    Integer questionAId = createQuestion(authorA, "SPORT",
        "How should public speaking practice work?");
    Integer answerFromBId = createAnswer(authorB, questionAId,
        "Practice with short rounds and specific feedback.");

    Integer questionCId = createQuestion(authorC, "ART",
        "Which watercolor brush keeps a sharp tip?");
    Integer answerFromAId = createAnswer(authorA, questionCId,
        "Use a good round brush and test the snap.");

    mockMvc.perform(get("/api/qa/questions/" + questionAId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.question.id").value(questionAId))
        .andExpect(jsonPath("$.question.author.userName").value(authorA.username()))
        .andExpect(jsonPath("$.answers.length()").value(1))
        .andExpect(jsonPath("$.answers[0].id").value(answerFromBId))
        .andExpect(jsonPath("$.answers[0].author.userName").value(authorB.username()));

    mockMvc.perform(get("/api/qa/questions/" + questionCId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.question.id").value(questionCId))
        .andExpect(jsonPath("$.answers[0].id").value(answerFromAId))
        .andExpect(jsonPath("$.answers[0].author.userName").value(authorA.username()));

    mockMvc.perform(put("/api/qa/questions/" + questionAId)
            .cookie(authorB.csrf().cookie(), authorB.session())
            .header("X-XSRF-TOKEN", authorB.csrf().token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "category": "SPORT",
                  "question": "Hijacked question?"
                }
                """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.detail").value("Cannot update question created by another user"));

    mockMvc.perform(put("/api/qa/answers/" + answerFromBId)
            .cookie(authorC.csrf().cookie(), authorC.session())
            .header("X-XSRF-TOKEN", authorC.csrf().token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "answer": "Hijacked answer"
                }
                """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.detail").value("Cannot update answer created by another user"));

    mockMvc.perform(put("/api/qa/answers/" + answerFromBId)
            .cookie(authorB.csrf().cookie(), authorB.session())
            .header("X-XSRF-TOKEN", authorB.csrf().token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "answer": "Practice with a timer and feedback."
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.author.userName").value(authorB.username()))
        .andExpect(jsonPath("$.answer").value("Practice with a timer and feedback."));

    mockMvc.perform(put("/api/qa/questions/" + questionAId)
            .cookie(authorA.csrf().cookie(), authorA.session())
            .header("X-XSRF-TOKEN", authorA.csrf().token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "category": "SPORT",
                  "question": "How should public speaking practice work before a meetup?"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.author.userName").value(authorA.username()))
        .andExpect(jsonPath("$.question")
            .value("How should public speaking practice work before a meetup?"));

    mockMvc.perform(get("/api/qa/questions")
            .param("query", "watercolor"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(questionCId))
        .andExpect(jsonPath("$[0].author.userName").value(authorC.username()));

    mockMvc.perform(get("/api/qa/questions")
            .param("query", "public speaking"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(questionAId));

    mockMvc.perform(delete("/api/qa/questions/" + questionAId)
            .cookie(authorB.csrf().cookie(), authorB.session())
            .header("X-XSRF-TOKEN", authorB.csrf().token()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.detail").value("Cannot delete question created by another user"));

    mockMvc.perform(delete("/api/qa/questions/" + questionAId)
            .cookie(authorA.csrf().cookie(), authorA.session())
            .header("X-XSRF-TOKEN", authorA.csrf().token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/qa/questions/" + questionAId))
        .andExpect(status().isNotFound());

    mockMvc.perform(get("/api/qa/questions")
            .param("query", "public speaking"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", empty()));
  }

  @Test
  void roomQaAndInactiveRoomFlowKeepsMembershipAndBlocksNewJoin() throws Exception {
    SessionUser owner = createActiveUserAndLogin("combined-owner@example.com", "combinedOwner");
    SessionUser participant = createActiveUserAndLogin("combined-member@example.com",
        "combinedMember");
    SessionUser lateJoiner = createActiveUserAndLogin("combined-late@example.com",
        "combinedLate");

    Integer roomId = createRoom(owner, true, "Combined flow room",
        "https://chat.example.com/combined");
    joinRoom(participant, roomId);

    Integer questionId = createQuestion(owner, "SPORT",
        "How should we prepare before the room event?");
    Integer answerId = createAnswer(participant, questionId,
        "Bring a checklist and confirm the meeting link.");

    mockMvc.perform(get("/api/qa/questions/" + questionId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.answers[0].id").value(answerId))
        .andExpect(jsonPath("$.answers[0].author.userName").value(participant.username()));

    mockMvc.perform(put("/api/rooms/" + roomId)
            .cookie(participant.csrf().cookie(), participant.session())
            .header("X-XSRF-TOKEN", participant.csrf().token())
            .contentType(MediaType.APPLICATION_JSON)
            .content(roomUpdatePayload(true, "Combined flow hijack", "ACTIVE",
                "https://chat.example.com/hijack")))
        .andExpect(status().isForbidden());

    mockMvc.perform(put("/api/rooms/" + roomId)
            .cookie(owner.csrf().cookie(), owner.session())
            .header("X-XSRF-TOKEN", owner.csrf().token())
            .contentType(MediaType.APPLICATION_JSON)
            .content(roomUpdatePayload(true, "Combined flow room", "INACTIVE",
                "https://chat.example.com/combined")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(roomId))
        .andExpect(jsonPath("$.status").value("INACTIVE"))
        .andExpect(jsonPath("$.active").value(false));

    mockMvc.perform(get("/api/rooms/" + roomId).cookie(owner.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INACTIVE"))
        .andExpect(jsonPath("$.active").value(false))
        .andExpect(jsonPath("$.participantCount").value(2));

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(lateJoiner.csrf().cookie(), lateJoiner.session())
            .header("X-XSRF-TOKEN", lateJoiner.csrf().token()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Only active rooms can accept new participants"));

    expectMembership(participant, roomId, "PARTICIPANT", "PARTICIPANT", false);

    mockMvc.perform(get("/api/rooms/" + roomId + "/membership/status")
            .cookie(lateJoiner.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("NOT_JOINED"))
        .andExpect(jsonPath("$.role").value(nullValue()))
        .andExpect(jsonPath("$.pendingRequestId").value(nullValue()))
        .andExpect(jsonPath("$.canJoin").value(false));

    mockMvc.perform(get("/api/rooms/999999").cookie(owner.session()))
        .andExpect(status().isNotFound());
  }

  private SessionUser loginRegisteredUser(NewUser user) {
    try {
      CsrfContext initialCsrf = fetchCsrf();
      Cookie session = login(user.email(), PASSWORD, initialCsrf, null);
      CsrfContext sessionCsrf = fetchCsrfForSession(session);
      Integer userId = userJpaRepository.findByEmailNormalized(
              user.email().toLowerCase(Locale.ROOT))
          .orElseThrow()
          .getId();
      return new SessionUser(userId, user.email(), user.username(), session, sessionCsrf);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to login registered user " + user.email(), ex);
    }
  }

  private SessionUser createActiveUserAndLogin(String email, String username) throws Exception {
    UserEntity user = createActiveUser(email, username, PASSWORD);
    CsrfContext initialCsrf = fetchCsrf();
    Cookie session = login(email, PASSWORD, initialCsrf, null);
    CsrfContext sessionCsrf = fetchCsrfForSession(session);
    return new SessionUser(user.getId(), email, username, session, sessionCsrf);
  }

  private CsrfContext fetchCsrfForSession(Cookie session) throws Exception {
    MvcResult result = mockMvc.perform(get("/api/auth/csrf")
            .cookie(session))
        .andExpect(status().isOk())
        .andReturn();

    Cookie csrfCookie = Objects.requireNonNull(result.getResponse().getCookie("XSRF-TOKEN"));
    return csrfContextFromCookie(csrfCookie);
  }

  private Integer createRoom(SessionUser owner, boolean isPublic, String roomName, String chatLink)
      throws Exception {
    MvcResult result = mockMvc.perform(post("/api/rooms/createRoom")
            .cookie(owner.csrf().cookie(), owner.session())
            .header("X-XSRF-TOKEN", owner.csrf().token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "isPublic": %s,
                  "category": "SPORT",
                  "name": "%s",
                  "description": "Complex integration test room",
                  "maximumNumberOfPeople": 10,
                  "chatLink": "%s",
                  "dateOfStartEvent": "%s",
                  "dateOfEndEvent": "%s",
                  "ageRating": 18
                }
                """.formatted(
                Boolean.toString(isPublic),
                roomName,
                chatLink,
                Instant.now().plusSeconds(3600).toString(),
                Instant.now().plusSeconds(7200).toString())))
        .andExpect(status().isCreated())
        .andReturn();

    JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
    return payload.get("roomId").asInt();
  }

  private void joinRoom(SessionUser user, Integer roomId) throws Exception {
    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(user.csrf().cookie(), user.session())
            .header("X-XSRF-TOKEN", user.csrf().token()))
        .andExpect(status().isNoContent());
  }

  private void expectMembership(SessionUser user, Integer roomId, String statusValue, String role,
      boolean canJoin) throws Exception {
    mockMvc.perform(get("/api/rooms/" + roomId + "/membership/status")
            .cookie(user.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roomId").value(roomId))
        .andExpect(jsonPath("$.userId").value(user.id()))
        .andExpect(jsonPath("$.status").value(statusValue))
        .andExpect(jsonPath("$.role").value(role))
        .andExpect(jsonPath("$.pendingRequestId").value(nullValue()))
        .andExpect(jsonPath("$.canJoin").value(canJoin));
  }

  private Integer createQuestion(SessionUser user, String category, String question)
      throws Exception {
    MvcResult result = mockMvc.perform(post("/api/qa/questions")
            .cookie(user.csrf().cookie(), user.session())
            .header("X-XSRF-TOKEN", user.csrf().token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "category": "%s",
                  "question": "%s"
                }
                """.formatted(category, question)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.author.userName").value(user.username()))
        .andReturn();

    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asInt();
  }

  private Integer createAnswer(SessionUser user, Integer questionId, String answer)
      throws Exception {
    MvcResult result = mockMvc.perform(post("/api/qa/answers")
            .cookie(user.csrf().cookie(), user.session())
            .header("X-XSRF-TOKEN", user.csrf().token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "questionId": %d,
                  "previousAnswerId": null,
                  "answer": "%s"
                }
                """.formatted(questionId, answer)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.author.userName").value(user.username()))
        .andReturn();

    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asInt();
  }

  private String roomUpdatePayload(boolean isPublic, String roomName, String status,
      String chatLink) {
    return """
        {
          "isPublic": %s,
          "category": "SPORT",
          "name": "%s",
          "description": "Updated complex integration test room",
          "maximumNumberOfPeople": 10,
          "chatLink": "%s",
          "dateOfStartEvent": "%s",
          "dateOfEndEvent": "%s",
          "status": "%s",
          "ageRating": 18
        }
        """.formatted(
        Boolean.toString(isPublic),
        roomName,
        chatLink,
        Instant.now().plusSeconds(3600).toString(),
        Instant.now().plusSeconds(7200).toString(),
        status);
  }

  private record NewUser(String email, String username) {
  }

  private record SessionUser(
      Integer id,
      String email,
      String username,
      Cookie session,
      CsrfContext csrf) {
  }
}
