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
  void ownerCanUpdateRoomFieldsAndStatusIdempotently() throws Exception {
    UserEntity owner = createActiveUser("update-owner@example.com", "updateOwner", "Password123");
    UserEntity requester = createActiveUser("update-requester@example.com", "updateRequester",
        "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);
    Cookie requesterSession = login(requester.getEmail(), "Password123", csrf, null);

    Integer roomId = createRoomThroughApi(ownerSession, csrf, true, "Mutable room",
        "https://chat.example.com/mutable");

    String updatePayload = """
        {
          "isPublic": false,
          "category": "ART",
          "name": "Archived room",
          "description": "Updated integration room",
          "maximumNumberOfPeople": 5,
          "chatLink": "https://chat.example.com/archived",
          "dateOfStartEvent": "2025-01-02T10:00:00Z",
          "dateOfEndEvent": "2025-01-02T12:00:00Z",
          "status": "INACTIVE",
          "ageRating": 18
        }
        """;

    mockMvc.perform(put("/api/rooms/" + roomId)
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content(updatePayload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INACTIVE"))
        .andExpect(jsonPath("$.active").value(false))
        .andExpect(jsonPath("$.name").value("Archived room"))
        .andExpect(jsonPath("$.isPublic").value(false));

    mockMvc.perform(put("/api/rooms/" + roomId)
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content(updatePayload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INACTIVE"))
        .andExpect(jsonPath("$.active").value(false))
        .andExpect(jsonPath("$.name").value("Archived room"));

    mockMvc.perform(get("/api/rooms/" + roomId).cookie(ownerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INACTIVE"))
        .andExpect(jsonPath("$.active").value(false))
        .andExpect(jsonPath("$.name").value("Archived room"))
        .andExpect(jsonPath("$.isPublic").value(false));

    mockMvc.perform(get("/api/rooms").cookie(ownerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), requesterSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Only active rooms can accept new participants"));
  }

  @Test
  void switchingPrivateRoomToPublicClearsPendingRequestsAndAllowsDirectJoin() throws Exception {
    UserEntity owner = createActiveUser("visibility-owner@example.com", "visibilityOwner",
        "Password123");
    UserEntity requester = createActiveUser("visibility-requester@example.com",
        "visibilityRequester", "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);
    Cookie requesterSession = login(requester.getEmail(), "Password123", csrf, null);

    Integer roomId = createRoomThroughApi(ownerSession, csrf, false, "Visibility room",
        "https://chat.example.com/visibility");

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), requesterSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/users/rooms/" + roomId + "/requests/pending").cookie(ownerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));

    mockMvc.perform(put("/api/rooms/" + roomId)
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "isPublic": true,
                  "category": "SPORT",
                  "name": "Visibility room",
                  "description": "Now open",
                  "maximumNumberOfPeople": 10,
                  "chatLink": "https://chat.example.com/visibility",
                  "dateOfStartEvent": "2026-01-02T10:00:00Z",
                  "dateOfEndEvent": "2026-01-02T12:00:00Z",
                  "status": "ACTIVE",
                  "ageRating": 18
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isPublic").value(true))
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    mockMvc.perform(get("/api/users/rooms/" + roomId + "/requests/pending").cookie(ownerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());

    mockMvc.perform(get("/api/users/requests/sent").cookie(requesterSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), requesterSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/users/rooms/" + roomId + "/membership").cookie(requesterSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(true));
  }

  @Test
  void switchingRoomToCompletedClosesPendingRequestsAsRefused() throws Exception {
    UserEntity owner = createActiveUser("completed-owner@example.com", "completedOwner",
        "Password123");
    UserEntity requester = createActiveUser("completed-requester@example.com",
        "completedRequester", "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);
    Cookie requesterSession = login(requester.getEmail(), "Password123", csrf, null);

    Integer roomId = createRoomThroughApi(ownerSession, csrf, false, "Completed room",
        "https://chat.example.com/completed");

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), requesterSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(put("/api/rooms/" + roomId)
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "isPublic": false,
                  "category": "SPORT",
                  "name": "Completed room",
                  "description": "Finished",
                  "maximumNumberOfPeople": 10,
                  "chatLink": "https://chat.example.com/completed",
                  "dateOfStartEvent": "2026-01-02T10:00:00Z",
                  "dateOfEndEvent": "2026-01-02T12:00:00Z",
                  "status": "COMPLETED",
                  "ageRating": 18
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.active").value(false));

    mockMvc.perform(get("/api/users/rooms/" + roomId + "/requests/pending").cookie(ownerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());

    mockMvc.perform(get("/api/users/requests/sent").cookie(requesterSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("REFUSED"));
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
  void adminCanRemoveOrdinaryParticipantAndMembershipIsRevoked() throws Exception {
    UserEntity owner = createActiveUser("remove-owner@example.com", "removeOwner", "Password123");
    UserEntity admin = createActiveUser("remove-admin@example.com", "removeAdmin", "Password123");
    UserEntity participant = createActiveUser("remove-member@example.com", "removeMember",
        "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);
    Cookie adminSession = login(admin.getEmail(), "Password123", csrf, null);
    Cookie participantSession = login(participant.getEmail(), "Password123", csrf, null);

    Integer roomId = createRoomThroughApi(ownerSession, csrf, true, "Moderated removal room",
        "https://chat.example.com/moderated-removal");

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

    mockMvc.perform(delete("/api/rooms/" + roomId + "/participants/" + participant.getId())
            .cookie(csrf.cookie(), adminSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/users/rooms/" + roomId + "/membership").cookie(participantSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(false));

    mockMvc.perform(get("/api/rooms/" + roomId + "/participants").cookie(adminSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void adminCanBanListAndUnbanParticipantAndJoinAccessChangesAccordingly() throws Exception {
    UserEntity owner = createActiveUser("govern-owner@example.com", "governOwner", "Password123");
    UserEntity admin = createActiveUser("govern-admin@example.com", "governAdmin", "Password123");
    UserEntity participant = createActiveUser("govern-member@example.com", "governMember",
        "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);
    Cookie adminSession = login(admin.getEmail(), "Password123", csrf, null);
    Cookie participantSession = login(participant.getEmail(), "Password123", csrf, null);

    Integer roomId = createRoomThroughApi(ownerSession, csrf, true, "Governance room",
        "https://chat.example.com/governance");

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

    mockMvc.perform(post("/api/rooms/" + roomId + "/bans/" + participant.getId())
            .cookie(csrf.cookie(), adminSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/users/rooms/" + roomId + "/membership").cookie(participantSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(false));

    mockMvc.perform(get("/api/rooms/" + roomId + "/bans").cookie(adminSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(participant.getId()))
        .andExpect(jsonPath("$[0].userName").value("governMember"));

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), participantSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isForbidden());

    mockMvc.perform(delete("/api/rooms/" + roomId + "/bans/" + participant.getId())
            .cookie(csrf.cookie(), adminSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), participantSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/users/rooms/" + roomId + "/membership").cookie(participantSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(true));
  }

  @Test
  void adminCannotBanOwner() throws Exception {
    UserEntity owner = createActiveUser("cannot-ban-owner@example.com", "cannotBanOwner",
        "Password123");
    UserEntity admin = createActiveUser("cannot-ban-admin@example.com", "cannotBanAdmin",
        "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);
    Cookie adminSession = login(admin.getEmail(), "Password123", csrf, null);

    Integer roomId = createRoomThroughApi(ownerSession, csrf, true, "Owner protected room",
        "https://chat.example.com/owner-protected");

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), adminSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/users/rooms/" + roomId + "/admins/" + admin.getId())
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/rooms/" + roomId + "/bans/" + owner.getId())
            .cookie(csrf.cookie(), adminSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Room owner cannot be banned"));
  }

  @Test
  void ownerCanTransferOwnershipAndRemainsParticipant() throws Exception {
    UserEntity owner = createActiveUser("transfer-owner@example.com", "transferOwner",
        "Password123");
    UserEntity successor = createActiveUser("transfer-successor@example.com",
        "transferSuccessor", "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);
    Cookie successorSession = login(successor.getEmail(), "Password123", csrf, null);

    Integer roomId = createRoomThroughApi(ownerSession, csrf, true, "Transfer room",
        "https://chat.example.com/transfer");

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), successorSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/rooms/" + roomId + "/ownership/transfer")
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "targetUserId": %d
                }
                """.formatted(successor.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roomId").value(roomId))
        .andExpect(jsonPath("$.previousOwnerId").value(owner.getId()))
        .andExpect(jsonPath("$.newOwnerId").value(successor.getId()))
        .andExpect(jsonPath("$.previousOwnerNewRole").value("PARTICIPANT"))
        .andExpect(jsonPath("$.newOwnerRole").value("OWNER"));

    mockMvc.perform(get("/api/users/rooms/" + roomId + "/membership").cookie(ownerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(true));

    mockMvc.perform(get("/api/rooms/" + roomId + "/participants/" + owner.getId())
            .cookie(successorSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("PARTICIPANT"));

    mockMvc.perform(get("/api/rooms/" + roomId + "/participants").cookie(ownerSession))
        .andExpect(status().isForbidden());
  }

  @Test
  void ownerCannotTransferOwnershipToOutsider() throws Exception {
    UserEntity owner = createActiveUser("transfer-conflict-owner@example.com",
        "transferConflict", "Password123");
    UserEntity outsider = createActiveUser("transfer-outsider@example.com", "transferOutsider",
        "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);

    Integer roomId = createRoomThroughApi(ownerSession, csrf, true, "Transfer conflict room",
        "https://chat.example.com/transfer-conflict");

    mockMvc.perform(post("/api/rooms/" + roomId + "/ownership/transfer")
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "targetUserId": %d
                }
                """.formatted(outsider.getId())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_OWNERSHIP_TRANSFER"))
        .andExpect(jsonPath("$.message")
            .value("Ownership can only be transferred to an existing room participant"));
  }

  @Test
  void governanceEndpointsReturnNotFoundForMissingRoom() throws Exception {
    UserEntity owner = createActiveUser("missing-room-owner@example.com", "missingRoomOwner",
        "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);

    mockMvc.perform(delete("/api/rooms/999999/participants/" + owner.getId())
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Room not found: 999999"));

    mockMvc.perform(post("/api/rooms/999999/bans/" + owner.getId())
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Room not found: 999999"));

    mockMvc.perform(get("/api/rooms/999999/bans").cookie(ownerSession))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Room not found: 999999"));

    mockMvc.perform(delete("/api/rooms/999999/bans/" + owner.getId())
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Room not found: 999999"));

    mockMvc.perform(post("/api/rooms/999999/ownership/transfer")
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "targetUserId": %d
                }
                """.formatted(owner.getId())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Room not found: 999999"));
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
  void nonOwnerCannotUpdateRoom() throws Exception {
    UserEntity owner = createActiveUser("forbidden-owner@example.com", "forbiddenOwner",
        "Password123");
    UserEntity participant = createActiveUser("forbidden-member@example.com", "forbiddenMember",
        "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);
    Cookie participantSession = login(participant.getEmail(), "Password123", csrf, null);

    Integer roomId = createRoomThroughApi(ownerSession, csrf, true, "Protected room",
        "https://chat.example.com/protected");

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), participantSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(put("/api/rooms/" + roomId)
            .cookie(csrf.cookie(), participantSession)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "isPublic": true,
                  "category": "SPORT",
                  "name": "Hijacked room",
                  "description": "Should fail",
                  "maximumNumberOfPeople": 10,
                  "chatLink": "https://chat.example.com/hijacked",
                  "dateOfStartEvent": "2026-01-02T10:00:00Z",
                  "dateOfEndEvent": "2026-01-02T12:00:00Z",
                  "status": "ACTIVE",
                  "ageRating": 18
                }
                """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Only room owner can manage the room"));
  }

  @Test
  void ownerCannotReduceCapacityBelowCurrentParticipantCount() throws Exception {
    UserEntity owner = createActiveUser("capacity-owner@example.com", "capacityOwner",
        "Password123");
    UserEntity participant = createActiveUser("capacity-member@example.com", "capacityMember",
        "Password123");
    UserEntity secondParticipant = createActiveUser("capacity-member-two@example.com",
        "capacityMemberTwo", "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);
    Cookie participantSession = login(participant.getEmail(), "Password123", csrf, null);
    Cookie secondParticipantSession = login(secondParticipant.getEmail(), "Password123", csrf, null);

    Integer roomId = createRoomThroughApi(ownerSession, csrf, true, "Capacity room",
        "https://chat.example.com/capacity");

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), participantSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), secondParticipantSession)
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
                  "name": "Capacity room",
                  "description": "Too small",
                  "maximumNumberOfPeople": 2,
                  "chatLink": "https://chat.example.com/capacity",
                  "dateOfStartEvent": "2026-01-02T10:00:00Z",
                  "dateOfEndEvent": "2026-01-02T12:00:00Z",
                  "status": "ACTIVE",
                  "ageRating": 18
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
            .value("Room capacity cannot be lower than current participant count"));
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
  void directBanOfPendingRequesterClosesRequestAndPublishesRejectedNotification() throws Exception {
    UserEntity owner = createActiveUser("direct-ban-owner@example.com", "directBanOwner",
        "Password123");
    UserEntity requester = createActiveUser("direct-ban-requester@example.com",
        "directBanReq", "Password123");

    CsrfContext csrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", csrf, null);
    Cookie requesterSession = login(requester.getEmail(), "Password123", csrf, null);

    Integer roomId = createRoomThroughApi(ownerSession, csrf, false, "Direct ban room",
        "https://chat.example.com/direct-ban");

    mockMvc.perform(post("/api/rooms/" + roomId + "/join")
            .cookie(csrf.cookie(), requesterSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/rooms/" + roomId + "/bans/" + requester.getId())
            .cookie(csrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    verify(notificationService).sendMembershipRejected(eq(requester.getId()), eq("Direct ban room"));

    mockMvc.perform(get("/api/users/rooms/" + roomId + "/requests/pending").cookie(ownerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());

    mockMvc.perform(get("/api/users/requests/sent").cookie(requesterSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("REFUSED_WITH_BAN"));
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
