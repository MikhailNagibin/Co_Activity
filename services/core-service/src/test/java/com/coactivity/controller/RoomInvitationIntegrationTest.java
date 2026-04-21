package com.coactivity.controller;

import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coactivity.TestcontainersConfiguration;
import com.coactivity.persistence.entity.UserEntity;
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
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Tag("docker")
@DisplayName("Room invitation integration tests")
class RoomInvitationIntegrationTest extends AbstractSessionWebIntegrationTest {

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
  void invitePublishesKafkaPayloadWithRoomOwnerAndLink() throws Exception {
    UserEntity ownerEntity = createActiveUser("invite-owner@example.com", "inviteOwner",
        "Password123");
    UserEntity invitedEntity = createActiveUser("invite-target@example.com", "inviteTarget",
        "Password123");

    SessionContext owner = loginWithSessionCsrf(ownerEntity.getEmail(), "Password123");
    Integer roomId = createRoom(owner.session(), owner.csrf(), false, "Invite Room");

    mockMvc.perform(post("/api/rooms/" + roomId + "/invites")
            .cookie(owner.csrf().cookie(), owner.session())
            .header("X-XSRF-TOKEN", owner.csrf().token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "userId": %d
                }
                """.formatted(invitedEntity.getId())))
        .andExpect(status().isNoContent());

    ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
    verify(kafkaTemplate).send(eq("notifications.email.v1"), eq("invite-target@example.com"),
        payloadCaptor.capture());

    assertTrue(containsAll(payloadCaptor.getValue(),
        "Invite Room",
        "inviteOwner",
        "http://localhost:5173/rooms/" + roomId));
  }

  @Test
  void inviteReturnsServiceUnavailableWhenKafkaPublishFails() throws Exception {
    UserEntity ownerEntity = createActiveUser("invite-failed-owner@example.com", "inviteFailedOwner",
        "Password123");
    UserEntity invitedEntity = createActiveUser("invite-failed-target@example.com", "inviteFailedTarget",
        "Password123");

    SessionContext owner = loginWithSessionCsrf(ownerEntity.getEmail(), "Password123");
    SessionContext invited = loginWithSessionCsrf(invitedEntity.getEmail(), "Password123");

    Integer roomId = createRoom(owner.session(), owner.csrf(), false, "Kafka Failed Invite Room");

    CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
    failedFuture.completeExceptionally(new RuntimeException("Kafka unavailable"));
    when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(failedFuture);

    mockMvc.perform(post("/api/rooms/" + roomId + "/invites")
            .cookie(owner.csrf().cookie(), owner.session())
            .header("X-XSRF-TOKEN", owner.csrf().token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "userId": %d
                }
                """.formatted(invitedEntity.getId())))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("NOTIFICATION_DELIVERY_FAILED"));

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(invited.csrf().cookie(), invited.session())
            .header("X-XSRF-TOKEN", invited.csrf().token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/users/rooms/" + roomId + "/membership")
            .cookie(invited.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(false));

    mockMvc.perform(get("/api/users/rooms/" + roomId + "/requests/pending")
            .cookie(owner.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void inviteReturnsConflictWhenInvitedUserIsBanned() throws Exception {
    UserEntity ownerEntity = createActiveUser("invite-ban-owner@example.com", "inviteBanOwner",
        "Password123");
    UserEntity invitedEntity = createActiveUser("invite-ban-target@example.com", "inviteBanTarget",
        "Password123");

    SessionContext owner = loginWithSessionCsrf(ownerEntity.getEmail(), "Password123");
    Integer roomId = createRoom(owner.session(), owner.csrf(), true, "Banned Invite Room");

    mockMvc.perform(post("/api/rooms/" + roomId + "/bans/" + invitedEntity.getId())
            .cookie(owner.csrf().cookie(), owner.session())
            .header("X-XSRF-TOKEN", owner.csrf().token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/rooms/" + roomId + "/invites")
            .cookie(owner.csrf().cookie(), owner.session())
            .header("X-XSRF-TOKEN", owner.csrf().token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "userId": %d
                }
                """.formatted(invitedEntity.getId())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("USER_BANNED"));
  }

  @Test
  void inviteReturnsConflictWhenInvitedUserAlreadyMember() throws Exception {
    UserEntity ownerEntity = createActiveUser("invite-member-owner@example.com", "inviteMemberOwner",
        "Password123");
    UserEntity invitedEntity = createActiveUser("invite-member-target@example.com", "inviteMemberTarget",
        "Password123");

    SessionContext owner = loginWithSessionCsrf(ownerEntity.getEmail(), "Password123");
    SessionContext invited = loginWithSessionCsrf(invitedEntity.getEmail(), "Password123");

    Integer roomId = createRoom(owner.session(), owner.csrf(), true, "Already Member Invite Room");

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(invited.csrf().cookie(), invited.session())
            .header("X-XSRF-TOKEN", invited.csrf().token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/rooms/" + roomId + "/invites")
            .cookie(owner.csrf().cookie(), owner.session())
            .header("X-XSRF-TOKEN", owner.csrf().token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "userId": %d
                }
                """.formatted(invitedEntity.getId())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ALREADY_MEMBER"));
  }

  @Test
  void privateRoomInviteAllowsDirectJoinWithoutPendingRequest() throws Exception {
    UserEntity ownerEntity = createActiveUser("invite-private-owner@example.com", "invitePrivateOwner",
        "Password123");
    UserEntity invitedEntity = createActiveUser("invite-private-target@example.com", "invitePrivateTarget",
        "Password123");

    SessionContext owner = loginWithSessionCsrf(ownerEntity.getEmail(), "Password123");
    SessionContext invited = loginWithSessionCsrf(invitedEntity.getEmail(), "Password123");

    Integer roomId = createRoom(owner.session(), owner.csrf(), false, "Private Invite Room");

    mockMvc.perform(post("/api/rooms/" + roomId + "/invites")
            .cookie(owner.csrf().cookie(), owner.session())
            .header("X-XSRF-TOKEN", owner.csrf().token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "userId": %d
                }
                """.formatted(invitedEntity.getId())))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(invited.csrf().cookie(), invited.session())
            .header("X-XSRF-TOKEN", invited.csrf().token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/users/rooms/" + roomId + "/membership")
            .cookie(invited.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(true));

    mockMvc.perform(get("/api/users/rooms/" + roomId + "/requests/pending")
            .cookie(owner.session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", empty()));
  }

  private Integer createRoom(Cookie ownerSession, CsrfContext csrf, boolean isPublic, String name)
      throws Exception {
    var result = mockMvc.perform(post("/api/rooms/createRoom")
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "isPublic": %s,
                  "category": "SPORT",
                  "name": "%s",
                  "description": "Invitation integration room",
                  "maximumNumberOfPeople": 10,
                  "chatLink": "https://chat.example.com/%s",
                  "dateOfStartEvent": "2030-01-02T10:00:00Z",
                  "dateOfEndEvent": "2030-01-02T12:00:00Z",
                  "ageRating": 18
                }
                """.formatted(isPublic, name, name.toLowerCase().replace(" ", "-"))))
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
