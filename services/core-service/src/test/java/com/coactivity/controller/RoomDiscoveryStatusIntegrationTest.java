package com.coactivity.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coactivity.TestcontainersConfiguration;
import com.coactivity.persistence.entity.UserEntity;
import com.coactivity.service.NotificationService;
import com.coactivity.support.AbstractSessionWebIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
@DisplayName("Room discovery and membership status integration tests")
class RoomDiscoveryStatusIntegrationTest extends AbstractSessionWebIntegrationTest {

  @MockitoBean
  private NotificationService notificationService;

  @BeforeEach
  void setUp() throws Exception {
    resetState();
    Mockito.reset(notificationService);
  }

  @Test
  void roomsCanBeFilteredByLocation() throws Exception {
    UserEntity owner = createActiveUser("location-owner@example.com", "locationOwner",
        "Password123");
    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);

    Integer moscowRoomId = createRoom(ownerSession, csrf, true, "Moscow running club",
        "Moscow", "Russia");
    Integer privateMoscowRoomId = createRoom(ownerSession, csrf, false, "Private Moscow club",
        "Moscow", "Russia");
    createRoom(ownerSession, csrf, true, "Berlin running club", "Berlin", "Germany");

    mockMvc.perform(get("/api/rooms")
            .param("city", "Moscow")
            .param("country", "Russia"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value(privateMoscowRoomId))
        .andExpect(jsonPath("$[0].isPublic").value(false))
        .andExpect(jsonPath("$[0].city").value("Moscow"))
        .andExpect(jsonPath("$[0].country").value("Russia"))
        .andExpect(jsonPath("$[1].id").value(moscowRoomId))
        .andExpect(jsonPath("$[1].isPublic").value(true));
  }

  @Test
  void membershipStatusEndpointReportsParticipantPendingBannedAndNotJoined() throws Exception {
    UserEntity owner = createActiveUser("status-owner@example.com", "statusOwner", "Password123");
    UserEntity pendingUser = createActiveUser("status-pending@example.com", "statusPending",
        "Password123");
    UserEntity bannedUser = createActiveUser("status-banned@example.com", "statusBanned",
        "Password123");
    UserEntity outsider = createActiveUser("status-outsider@example.com", "statusOutsider",
        "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);
    Cookie pendingSession = login(pendingUser.getEmail(), "Password123", csrf, null);
    Cookie bannedSession = login(bannedUser.getEmail(), "Password123", csrf, null);
    Cookie outsiderSession = login(outsider.getEmail(), "Password123", csrf, null);

    Integer publicRoomId = createRoom(ownerSession, csrf, true, "Public status room",
        "Moscow", "Russia");
    Integer privateRoomId = createRoom(ownerSession, csrf, false, "Private status room",
        "Moscow", "Russia");

    mockMvc.perform(post("/api/rooms/" + privateRoomId + "/join")
            .cookie(csrf.cookie(), pendingSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/rooms/" + publicRoomId + "/bans/" + bannedUser.getId())
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/rooms/" + publicRoomId + "/membership/status")
            .cookie(ownerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roomId").value(publicRoomId))
        .andExpect(jsonPath("$.userId").value(owner.getId()))
        .andExpect(jsonPath("$.status").value("PARTICIPANT"))
        .andExpect(jsonPath("$.role").value("OWNER"))
        .andExpect(jsonPath("$.canJoin").value(false));

    mockMvc.perform(get("/api/rooms/" + privateRoomId + "/membership/status")
            .cookie(pendingSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.pendingRequestId").isNumber())
        .andExpect(jsonPath("$.canJoin").value(false));

    mockMvc.perform(get("/api/rooms/" + publicRoomId + "/membership/status")
            .cookie(bannedSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("BANNED"))
        .andExpect(jsonPath("$.canJoin").value(false));

    mockMvc.perform(get("/api/rooms/" + publicRoomId + "/membership/status")
            .cookie(outsiderSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("NOT_JOINED"))
        .andExpect(jsonPath("$.canJoin").value(true));
  }

  private Integer createRoom(Cookie session, CsrfContext csrf, boolean isPublic, String roomName,
      String city, String country) throws Exception {
    MvcResult createResult = mockMvc.perform(post("/api/rooms/createRoom")
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "isPublic": %s,
                  "category": "SPORT",
                  "name": "%s",
                  "description": "Integration test room",
                  "city": "%s",
                  "country": "%s",
                  "maximumNumberOfPeople": 10,
                  "chatLink": "https://chat.example.com/%s",
                  "dateOfStartEvent": "%s",
                  "dateOfEndEvent": "%s",
                  "ageRating": 18
                }
                """.formatted(
                Boolean.toString(isPublic),
                roomName,
                city,
                country,
                roomName.toLowerCase().replace(" ", "-"),
                Instant.now().plusSeconds(3600).toString(),
                Instant.now().plusSeconds(7200).toString())))
        .andExpect(status().isCreated())
        .andReturn();

    JsonNode createPayload = objectMapper.readTree(createResult.getResponse().getContentAsString());
    return createPayload.get("roomId").asInt();
  }
}
