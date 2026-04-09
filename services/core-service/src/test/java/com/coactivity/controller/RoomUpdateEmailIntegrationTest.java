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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coactivity.TestcontainersConfiguration;
import com.coactivity.support.AbstractSessionWebIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Tag("docker")
@DisplayName("Room update email integration tests")
class RoomUpdateEmailIntegrationTest extends AbstractSessionWebIntegrationTest {

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
    var owner = createActiveUser("room-email-owner@example.com", "roomEmailOwner", "Password123");
    var participant = createActiveUser("room-email-participant@example.com", "roomEmailParticipant",
        "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);
    Cookie participantSession = login(participant.getEmail(), "Password123", csrf, null);

    enableImportantRoomUpdates(ownerSession, csrf);
    enableImportantRoomUpdates(participantSession, csrf);

    Integer roomId = createRoom(ownerSession, csrf, true, "Important Status Room",
        "https://chat.example.com/status-old");

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), participantSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    reset(kafkaTemplate);
    when(kafkaTemplate.send(anyString(), anyString(), anyString()))
        .thenReturn(CompletableFuture.<SendResult<String, String>>completedFuture(null));

    mockMvc.perform(put("/api/rooms/" + roomId)
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token())
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
    var owner = createActiveUser("suppressed-owner@example.com", "suppOwner", "Password123");
    var participant = createActiveUser("suppressed-participant@example.com", "suppParticipant",
        "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);
    Cookie participantSession = login(participant.getEmail(), "Password123", csrf, null);

    Integer roomId = createRoom(ownerSession, csrf, true, "Suppressed Room",
        "https://chat.example.com/suppressed");

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), participantSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(put("/api/rooms/" + roomId)
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token())
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
    var owner = createActiveUser("request-owner@example.com", "requestOwner", "Password123");
    var requester = createActiveUser("requester@example.com", "requester", "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);
    Cookie requesterSession = login(requester.getEmail(), "Password123", csrf, null);

    enableImportantRoomUpdates(requesterSession, csrf);

    Integer roomId = createRoom(ownerSession, csrf, false, "Pending Approval Room",
        "https://chat.example.com/private");

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), requesterSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    reset(kafkaTemplate);
    when(kafkaTemplate.send(anyString(), anyString(), anyString()))
        .thenReturn(CompletableFuture.<SendResult<String, String>>completedFuture(null));

    mockMvc.perform(put("/api/rooms/" + roomId)
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token())
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

  private void enableImportantRoomUpdates(Cookie session, CsrfContext csrf) throws Exception {
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
}
