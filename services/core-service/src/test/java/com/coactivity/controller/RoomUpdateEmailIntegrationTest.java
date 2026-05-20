package com.coactivity.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coactivity.TestcontainersConfiguration;
import com.coactivity.support.AbstractSessionWebIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Tag("docker")
@DisplayName("Room update email integration tests")
class RoomUpdateEmailIntegrationTest extends AbstractSessionWebIntegrationTest {

  private record SessionContext(Cookie session, CsrfContext csrf) {
  }

  @MockitoBean
  private KafkaTemplate<String, String> kafkaTemplate;

  @BeforeEach
  void setUp() throws Exception {
    resetState();
    reset(kafkaTemplate);
    when(kafkaTemplate.send(anyString(), anyString(), anyString()))
        .thenReturn(CompletableFuture.<SendResult<String, String>>completedFuture(null));
  }

  @Test
  void statusChangeToInactivePublishesImportantRoomUpdateEmail() throws Exception {
    var ownerEntity = createActiveUser("room-email-owner@example.com", "roomEmailOwner", "Password123");
    var participantEntity = createActiveUser("room-email-participant@example.com", "roomEmailParticipant",
        "Password123");

    SessionContext owner = loginWithSessionCsrf(ownerEntity.getEmail(), "Password123");
    SessionContext participant = loginWithSessionCsrf(participantEntity.getEmail(), "Password123");

    enableImportantRoomUpdates(owner.session(), owner.csrf());
    enableImportantRoomUpdates(participant.session(), participant.csrf());

    Integer roomId = createRoom(owner.session(), owner.csrf(), true, "Important Status Room",
        "https://chat.example.com/status-old");

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(participant.csrf().cookie(), participant.session())
            .header("X-XSRF-TOKEN", participant.csrf().token()))
        .andExpect(status().isNoContent());

    reset(kafkaTemplate);
    when(kafkaTemplate.send(anyString(), anyString(), anyString()))
        .thenReturn(CompletableFuture.<SendResult<String, String>>completedFuture(null));

    mockMvc.perform(put("/api/rooms/" + roomId)
            .cookie(owner.csrf().cookie(), owner.session())
            .header("X-XSRF-TOKEN", owner.csrf().token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "isPublic": true,
                  "category": "SPORT",
                  "name": "Important Status Room",
                  "description": "Status changed",
                  "maximumNumberOfPeople": 10,
                  "chatLink": "https://chat.example.com/status-old",
                  "dateOfStartEvent": "2030-01-02T10:00:00Z",
                  "dateOfEndEvent": "2030-01-02T12:00:00Z",
                  "status": "INACTIVE",
                  "ageRating": 18
                }
                """))
        .andExpect(status().isOk());

    ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
    verify(kafkaTemplate, timeout(3000).times(2))
        .send(eq("notifications.email.v1"), anyString(), payloadCaptor.capture());

    assertTrue(payloadCaptor.getAllValues().stream().anyMatch(payload -> containsAll(payload,
        "Important Status Room",
        "Old status: ACTIVE",
        "New status: INACTIVE",
        "What it means for you")));
  }

  @Test
  void disabledImportantRoomUpdatesSuppressesEmailPublish() throws Exception {
    var ownerEntity = createActiveUser("suppressed-owner@example.com", "suppOwner", "Password123");
    var participantEntity = createActiveUser("suppressed-participant@example.com", "suppParticipant",
        "Password123");

    SessionContext owner = loginWithSessionCsrf(ownerEntity.getEmail(), "Password123");
    SessionContext participant = loginWithSessionCsrf(participantEntity.getEmail(), "Password123");

    Integer roomId = createRoom(owner.session(), owner.csrf(), true, "Suppressed Room",
        "https://chat.example.com/suppressed");

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(participant.csrf().cookie(), participant.session())
            .header("X-XSRF-TOKEN", participant.csrf().token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(put("/api/rooms/" + roomId)
            .cookie(owner.csrf().cookie(), owner.session())
            .header("X-XSRF-TOKEN", owner.csrf().token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "isPublic": true,
                  "category": "SPORT",
                  "name": "Suppressed Room",
                  "description": "Status changed",
                  "maximumNumberOfPeople": 10,
                  "chatLink": "https://chat.example.com/suppressed",
                  "dateOfStartEvent": "2030-01-02T10:00:00Z",
                  "dateOfEndEvent": "2030-01-02T12:00:00Z",
                  "status": "COMPLETED",
                  "ageRating": 18
                }
                """))
        .andExpect(status().isOk());

    verify(kafkaTemplate, after(1000).never()).send(anyString(), anyString(), anyString());
  }

  @Test
  void becomingPublicPublishesPendingRequesterEmailWhenApprovalNoLongerNeeded() throws Exception {
    var ownerEntity = createActiveUser("request-owner@example.com", "requestOwner", "Password123");
    var requesterEntity = createActiveUser("requester@example.com", "requester", "Password123");

    SessionContext owner = loginWithSessionCsrf(ownerEntity.getEmail(), "Password123");
    SessionContext requester = loginWithSessionCsrf(requesterEntity.getEmail(), "Password123");

    enableImportantRoomUpdates(requester.session(), requester.csrf());

    Integer roomId = createRoom(owner.session(), owner.csrf(), false, "Pending Approval Room",
        "https://chat.example.com/private");

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(requester.csrf().cookie(), requester.session())
            .header("X-XSRF-TOKEN", requester.csrf().token()))
        .andExpect(status().isNoContent());

    reset(kafkaTemplate);
    when(kafkaTemplate.send(anyString(), anyString(), anyString()))
        .thenReturn(CompletableFuture.<SendResult<String, String>>completedFuture(null));

    mockMvc.perform(put("/api/rooms/" + roomId)
            .cookie(owner.csrf().cookie(), owner.session())
            .header("X-XSRF-TOKEN", owner.csrf().token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "isPublic": true,
                  "category": "SPORT",
                  "name": "Pending Approval Room",
                  "description": "Now public",
                  "maximumNumberOfPeople": 10,
                  "chatLink": "https://chat.example.com/private",
                  "dateOfStartEvent": "2030-01-02T10:00:00Z",
                  "dateOfEndEvent": "2030-01-02T12:00:00Z",
                  "status": "ACTIVE",
                  "ageRating": 18
                }
                """))
        .andExpect(status().isOk());

    ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
    verify(kafkaTemplate, timeout(3000).times(1))
        .send(eq("notifications.email.v1"), eq("requester@example.com"), payloadCaptor.capture());

    assertTrue(containsAll(payloadCaptor.getValue(),
        "Pending Approval Room",
        "manual approval is no longer required",
        "you can now join directly"));
  }

  @Test
  void roomCreationPublishesEmailForFollowersOfAuthor() throws Exception {
    var ownerEntity = createActiveUser("follow-owner@example.com", "followOwner", "Password123");
    var followerEntity = createActiveUser("follow-subscriber@example.com", "followSubscriber",
        "Password123");

    SessionContext owner = loginWithSessionCsrf(ownerEntity.getEmail(), "Password123");
    SessionContext follower = loginWithSessionCsrf(followerEntity.getEmail(), "Password123");

    mockMvc.perform(post("/api/users/" + ownerEntity.getId() + "/follow")
            .cookie(follower.csrf().cookie(), follower.session())
            .header("X-XSRF-TOKEN", follower.csrf().token()))
        .andExpect(status().isNoContent());

    reset(kafkaTemplate);
    when(kafkaTemplate.send(anyString(), anyString(), anyString()))
        .thenReturn(CompletableFuture.<SendResult<String, String>>completedFuture(null));

    Integer roomId = createRoom(owner.session(), owner.csrf(), true, "Followed Author Room",
        "https://chat.example.com/followed-room");

    ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
    verify(kafkaTemplate, timeout(3000).times(1))
        .send(eq("notifications.email.v1"), eq("follow-subscriber@example.com"),
            payloadCaptor.capture());

    assertTrue(containsAll(payloadCaptor.getValue(),
        "followOwner",
        "Followed Author Room",
        "http://localhost:5173/rooms/" + roomId));
  }

  private void enableImportantRoomUpdates(Cookie session, CsrfContext csrf) throws Exception {
    SecurityContextHolder.clearContext();
    mockMvc.perform(put("/api/users/me/notifications")
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "importantRoomUpdates": true
                }
                """))
        .andExpect(status().isOk());
  }

  private Integer createRoom(Cookie ownerSession, CsrfContext csrf, boolean isPublic, String name,
      String chatLink) throws Exception {
    var result = mockMvc.perform(post("/api/rooms/createRoom")
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "isPublic": %s,
                  "category": "SPORT",
                  "name": "%s",
                  "description": "Integration room",
                  "maximumNumberOfPeople": 10,
                  "chatLink": "%s",
                  "dateOfStartEvent": "2030-01-02T10:00:00Z",
                  "dateOfEndEvent": "2030-01-02T12:00:00Z",
                  "ageRating": 18
                }
                """.formatted(isPublic, name, chatLink)))
        .andExpect(status().isCreated())
        .andReturn();

    JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
    return payload.get("roomId").asInt();
  }

  private boolean containsAll(String payload, String... parts) {
    for (String part : parts) {
      if (!payload.contains(part)) {
        return false;
      }
    }
    return true;
  }

  private SessionContext loginWithSessionCsrf(String email, String password) throws Exception {
    CsrfContext initialCsrf = fetchCsrf();
    Cookie session = login(email, password, initialCsrf, null);
    CsrfContext sessionCsrf = fetchCsrfForSession(session);
    return new SessionContext(session, sessionCsrf);
  }

  private CsrfContext fetchCsrfForSession(Cookie session) throws Exception {
    var result = mockMvc.perform(get("/api/auth/csrf")
            .cookie(session))
        .andExpect(status().isOk())
        .andReturn();

    Cookie csrfCookie = Objects.requireNonNull(result.getResponse().getCookie("XSRF-TOKEN"));
    return csrfContextFromCookie(csrfCookie);
  }
}
