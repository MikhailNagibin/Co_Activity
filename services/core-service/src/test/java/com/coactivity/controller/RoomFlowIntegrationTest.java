package com.coactivity.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coactivity.TestcontainersConfiguration;
import com.coactivity.domain.RequestStatus;
import com.coactivity.domain.Role;
import com.coactivity.domain.RoomsRequest;
import com.coactivity.persistence.entity.UserEntity;
import com.coactivity.repository.RoomRepository;
import com.coactivity.repository.RoomsRequestRepository;
import com.coactivity.service.NotificationService;
import com.coactivity.support.AbstractSessionWebIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
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
@DisplayName("Room flow integration tests")
class RoomFlowIntegrationTest extends AbstractSessionWebIntegrationTest {

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
  }

  @Test
  void ownerCanCreateRoomAndSeeProtectedFields() throws Exception {
    UserEntity owner = createActiveUser("room-owner@example.com", "roomOwner", "Password123");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(owner.getEmail(), "Password123", csrf, null);

    Integer roomId = createRoomThroughApi(session, csrf, true, "Owner public room",
        "https://chat.example.com/public");

    mockMvc.perform(get("/api/rooms/" + roomId).cookie(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(roomId))
        .andExpect(jsonPath("$.creator.userName").value("roomOwner"))
        .andExpect(jsonPath("$.participantCount").value(1))
        .andExpect(jsonPath("$.hasProtectedAccess").value(true))
        .andExpect(jsonPath("$.chatLink").value("https://chat.example.com/public"));

    mockMvc.perform(get("/api/rooms").cookie(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(roomId))
        .andExpect(jsonPath("$[0].isCurrentUserParticipant").value(true));
  }

  @Test
  void privateJoinRequestCanBeAcceptedAndRequesterGetsProtectedAccess() throws Exception {
    UserEntity owner = createActiveUser("private-owner@example.com", "privateOwner",
        "Password123");
    UserEntity requester = createActiveUser("private-requester@example.com", "privateRequester",
        "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);
    Cookie requesterSession = login(requester.getEmail(), "Password123", csrf, null);

    Integer roomId = createRoomThroughApi(ownerSession, csrf, false, "Private room",
        "https://chat.example.com/private");

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), requesterSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    verify(notificationService).sendNewJoinRequest(eq(owner.getId()), eq("Private room"),
        eq("privateRequester"));

    List<RoomsRequest> sentRequests = roomsRequestRepository.getRequestsByUser(requester.getId());
    assertEquals(1, sentRequests.size());
    assertEquals(RequestStatus.CONSIDERATION, sentRequests.getFirst().getStatus());

    MvcResult pendingResult = mockMvc.perform(get("/api/users/rooms/" + roomId + "/requests/pending")
            .cookie(ownerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].username").value("privateRequester"))
        .andReturn();

    JsonNode pendingPayload = objectMapper.readTree(
        pendingResult.getResponse().getContentAsString());
    Integer requestId = pendingPayload.get(0).get("requestId").asInt();

    mockMvc.perform(post("/api/users/requests/" + requestId)
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token())
            .param("action", "ACCEPTED"))
        .andExpect(status().isNoContent());

    assertTrue(roomRepository.isUserInMembers(roomId, requester.getId()));

    mockMvc.perform(get("/api/users/rooms/" + roomId + "/membership").cookie(requesterSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(true));

    mockMvc.perform(get("/api/rooms/" + roomId).cookie(requesterSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasProtectedAccess").value(true))
        .andExpect(jsonPath("$.chatLink").value("https://chat.example.com/private"));
  }

  @Test
  void requesterCanSeeAndCancelOwnPendingRequest() throws Exception {
    UserEntity owner = createActiveUser("cancel-owner@example.com", "cancelOwner",
        "Password123");
    UserEntity requester = createActiveUser("cancel-requester@example.com", "cancelRequester",
        "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);
    Cookie requesterSession = login(requester.getEmail(), "Password123", csrf, null);

    Integer roomId = createRoomThroughApi(ownerSession, csrf, false, "Cancelable room",
        "https://chat.example.com/cancel");

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), requesterSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    MvcResult sentRequestsResult = mockMvc.perform(get("/api/users/requests/sent")
            .cookie(requesterSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].roomName").value("Cancelable room"))
        .andReturn();

    JsonNode sentPayload = objectMapper.readTree(
        sentRequestsResult.getResponse().getContentAsString());
    Integer requestId = sentPayload.get(0).get("requestId").asInt();

    mockMvc.perform(delete("/api/users/requests/" + requestId)
            .cookie(csrf.cookie(), requesterSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    assertTrue(roomsRequestRepository.getRequestsByUser(requester.getId()).isEmpty());
  }

  @Test
  void ownerCanAssignAdminAndAdminCanInspectParticipants() throws Exception {
    UserEntity owner = createActiveUser("admin-owner@example.com", "adminOwner", "Password123");
    UserEntity member = createActiveUser("admin-member@example.com", "adminMember", "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);
    Cookie memberSession = login(member.getEmail(), "Password123", csrf, null);

    Integer roomId = createRoomThroughApi(ownerSession, csrf, true, "Admin room",
        "https://chat.example.com/admin");

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), memberSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/users/rooms/" + roomId + "/admins/" + member.getId())
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.newRole").value("ADMIN"))
        .andExpect(jsonPath("$.previousRole").value("PARTICIPANT"));

    mockMvc.perform(get("/api/rooms/" + roomId + "/participants").cookie(memberSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));

    mockMvc.perform(get("/api/rooms/" + roomId + "/participants/" + owner.getId())
            .cookie(memberSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isMember").value(true))
        .andExpect(jsonPath("$.role").value("OWNER"))
        .andExpect(jsonPath("$.roomName").value("Admin room"));
  }

  @Test
  void participantCanLeaveRoomAndLosesProtectedAccess() throws Exception {
    UserEntity owner = createActiveUser("leave-owner@example.com", "leaveOwner", "Password123");
    UserEntity participant = createActiveUser("leave-member@example.com", "leaveMember",
        "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);
    Cookie participantSession = login(participant.getEmail(), "Password123", csrf, null);

    Integer roomId = createRoomThroughApi(ownerSession, csrf, true, "Leave room",
        "https://chat.example.com/leave");

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), participantSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/rooms/" + roomId + "/leave")
            .cookie(csrf.cookie(), participantSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/users/rooms/" + roomId + "/membership").cookie(participantSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(false));

    mockMvc.perform(get("/api/rooms/" + roomId).cookie(participantSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasProtectedAccess").value(false))
        .andExpect(jsonPath("$.chatLink").value(nullValue()));
  }

  @Test
  void refuseWithBanAddsRoomToBannedListAndBlocksFutureJoin() throws Exception {
    UserEntity owner = createActiveUser("ban-owner@example.com", "banOwner", "Password123");
    UserEntity requester = createActiveUser("ban-requester@example.com", "banRequester",
        "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);
    Cookie requesterSession = login(requester.getEmail(), "Password123", csrf, null);

    Integer roomId = createRoomThroughApi(ownerSession, csrf, false, "Ban room",
        "https://chat.example.com/ban");

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), requesterSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    MvcResult pendingResult = mockMvc.perform(get("/api/users/rooms/" + roomId + "/requests/pending")
            .cookie(ownerSession))
        .andExpect(status().isOk())
        .andReturn();

    Integer requestId = objectMapper.readTree(pendingResult.getResponse().getContentAsString())
        .get(0)
        .get("requestId")
        .asInt();

    mockMvc.perform(post("/api/users/requests/" + requestId)
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token())
            .param("action", "REFUSED_WITH_BAN"))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/users/banned-rooms").cookie(requesterSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(roomId))
        .andExpect(jsonPath("$[0].name").value("Ban room"));

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), requesterSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isForbidden());
  }

  @Test
  void ownerCanDemoteAdminAndDemotedUserLosesModerationAccess() throws Exception {
    UserEntity owner = createActiveUser("demote-owner@example.com", "demoteOwner",
        "Password123");
    UserEntity member = createActiveUser("demote-member@example.com", "demoteMember",
        "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);
    Cookie memberSession = login(member.getEmail(), "Password123", csrf, null);

    Integer roomId = createRoomThroughApi(ownerSession, csrf, true, "Demote room",
        "https://chat.example.com/demote");

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), memberSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/users/rooms/" + roomId + "/admins/" + member.getId())
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.newRole").value("ADMIN"));

    mockMvc.perform(delete("/api/users/rooms/" + roomId + "/admins/" + member.getId())
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.newRole").value("PARTICIPANT"))
        .andExpect(jsonPath("$.previousRole").value("ADMIN"));

    mockMvc.perform(get("/api/rooms/" + roomId + "/participants").cookie(memberSession))
        .andExpect(status().isForbidden());
  }

  @Test
  void moderatorsCanManageBulletinBoardWhileParticipantCannotEditIt() throws Exception {
    UserEntity owner = createActiveUser("board-owner@example.com", "boardOwner", "Password123");
    UserEntity admin = createActiveUser("board-admin@example.com", "boardAdmin", "Password123");
    UserEntity participant = createActiveUser("board-member@example.com", "boardMember",
        "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);
    Cookie adminSession = login(admin.getEmail(), "Password123", csrf, null);
    Cookie participantSession = login(participant.getEmail(), "Password123", csrf, null);

    Integer roomId = createRoomThroughApi(ownerSession, csrf, true, "Bulletin room",
        "https://chat.example.com/bulletin");

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), adminSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), participantSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/users/rooms/" + roomId + "/admins/" + admin.getId())
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isOk());

    mockMvc.perform(put("/api/rooms/" + roomId + "/bulletin")
            .cookie(csrf.cookie(), adminSession)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.TEXT_PLAIN)
            .content("Admin bulletin update"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value("Admin bulletin update"))
        .andExpect(jsonPath("$.author.userName").value("boardAdmin"));

    mockMvc.perform(get("/api/rooms/" + roomId).cookie(participantSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasProtectedAccess").value(true))
        .andExpect(jsonPath("$.bulletinBoard.content").value("Admin bulletin update"));

    mockMvc.perform(put("/api/rooms/" + roomId + "/bulletin")
            .cookie(csrf.cookie(), participantSession)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.TEXT_PLAIN)
            .content("Participant should not edit"))
        .andExpect(status().isForbidden());

    mockMvc.perform(delete("/api/rooms/" + roomId + "/bulletin")
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/rooms/" + roomId).cookie(ownerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bulletinBoard").value(nullValue()));
  }

  private Integer createRoomThroughApi(Cookie session, CsrfContext csrf, boolean isPublic,
      String roomName, String chatLink) throws Exception {
    String payload = """
        {
          "isPublic": %s,
          "category": "SPORT",
          "name": "%s",
          "description": "Integration test room",
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
        Instant.now().plusSeconds(7200).toString());

    MvcResult createResult = mockMvc.perform(post("/api/rooms/createRoom")
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isCreated())
        .andReturn();

    JsonNode createPayload = objectMapper.readTree(createResult.getResponse().getContentAsString());
    return createPayload.get("roomId").asInt();
  }
}
