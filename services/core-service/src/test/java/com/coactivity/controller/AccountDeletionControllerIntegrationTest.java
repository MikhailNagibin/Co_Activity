package com.coactivity.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coactivity.TestcontainersConfiguration;
import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.persistence.entity.UserEntity;
import com.coactivity.persistence.repository.RoomJpaRepository;
import com.coactivity.repository.RoomRepository;
import com.coactivity.service.NotificationService;
import com.coactivity.support.AbstractSessionWebIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Tag("docker")
@DisplayName("Account deletion integration tests")
class AccountDeletionControllerIntegrationTest extends AbstractSessionWebIntegrationTest {

  @Autowired
  private RoomRepository roomRepository;

  @Autowired
  private RoomJpaRepository roomJpaRepository;

  @MockitoBean
  private NotificationService notificationService;

  @BeforeEach
  void setUp() throws Exception {
    resetState();
  }

  @Test
  void deletionPreviewReportsOwnedRoomsAndTransferCandidates() throws Exception {
    UserEntity owner = createActiveUser("preview-owner@example.com", "previewOwner", "Password123");
    UserEntity admin = createActiveUser("preview-admin@example.com", "previewAdmin", "Password123");
    UserEntity participant = createActiveUser("preview-member@example.com", "previewMember",
        "Password123");

    Room room = createRoom(owner.getId(), "Preview room");
    roomRepository.addUserToRoom(room.getId(), admin.getId(), Role.ADMIN);
    roomRepository.addUserToRoom(room.getId(), participant.getId(), Role.PARTICIPANT);

    CsrfContext csrf = fetchCsrf();
    Cookie session = login(owner.getEmail(), "Password123", csrf, null);

    mockMvc.perform(get("/api/users/me/deletion-preview").cookie(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.canDeleteImmediately").value(false))
        .andExpect(jsonPath("$.ownedRooms[0].roomId").value(room.getId()))
        .andExpect(jsonPath("$.ownedRooms[0].roomName").value("Preview room"))
        .andExpect(jsonPath("$.ownedRooms[0].participantCount").value(3))
        .andExpect(jsonPath("$.ownedRooms[0].transferCandidates.length()").value(2));
  }

  @Test
  void deleteWithoutOwnedRoomsRemovesUserAndInvalidatesAllSessions() throws Exception {
    UserEntity user = createActiveUser("simple-delete@example.com", "simpleDelete", "Password123");
    CsrfContext csrf = fetchCsrf();

    Cookie firstSession = login(user.getEmail(), "Password123", csrf, null);
    Cookie secondSession = login(user.getEmail(), "Password123", csrf, null);

    mockMvc.perform(delete("/api/users/me")
            .cookie(csrf.cookie(), firstSession)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    assertFalse(userJpaRepository.findById(user.getId()).isPresent());

    mockMvc.perform(get("/api/auth/me").cookie(firstSession))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(get("/api/auth/me").cookie(secondSession))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deleteWithoutResolutionReturnsConflictWhenUserOwnsRooms() throws Exception {
    UserEntity owner = createActiveUser("conflict-owner@example.com", "conflictOwner",
        "Password123");
    createRoom(owner.getId(), "Conflict room");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(owner.getEmail(), "Password123", csrf, null);

    mockMvc.perform(delete("/api/users/me")
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("OWNED_ROOMS_RESOLUTION_REQUIRED"));
  }

  @Test
  void deletionCommandTransfersAndDeletesRoomsThenInvalidatesAllSessions() throws Exception {
    UserEntity owner = createActiveUser("complex-owner@example.com", "complexOwner", "Password123");
    UserEntity newOwner = createActiveUser("complex-admin@example.com", "complexAdmin",
        "Password123");
    UserEntity participant = createActiveUser("complex-member@example.com", "complexMember",
        "Password123");

    Room deletedRoom = createRoom(owner.getId(), "Deleted room");
    roomRepository.addUserToRoom(deletedRoom.getId(), participant.getId(), Role.PARTICIPANT);

    Room transferredRoom = createRoom(owner.getId(), "Transferred room");
    roomRepository.addUserToRoom(transferredRoom.getId(), newOwner.getId(), Role.ADMIN);

    CsrfContext csrf = fetchCsrf();
    Cookie firstSession = login(owner.getEmail(), "Password123", csrf, null);
    Cookie secondSession = login(owner.getEmail(), "Password123", csrf, null);

    mockMvc.perform(post("/api/users/me/deletion")
            .cookie(csrf.cookie(), firstSession)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType("application/json")
            .content("""
                {
                  "actions": [
                    {
                      "roomId": %d,
                      "mode": "DELETE_ROOM"
                    },
                    {
                      "roomId": %d,
                      "mode": "TRANSFER_OWNERSHIP",
                      "transferToUserId": %d
                    }
                  ]
                }
                """.formatted(deletedRoom.getId(), transferredRoom.getId(), newOwner.getId())))
        .andExpect(status().isNoContent());

    assertFalse(userJpaRepository.findById(owner.getId()).isPresent());
    assertFalse(roomJpaRepository.findById(deletedRoom.getId()).isPresent());
    assertEquals(Role.OWNER, roomRepository.getUserRoleByRoomId(transferredRoom.getId(),
        newOwner.getId()));
    assertFalse(roomRepository.isUserInMembers(transferredRoom.getId(), owner.getId()));

    mockMvc.perform(get("/api/auth/me").cookie(firstSession))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(get("/api/auth/me").cookie(secondSession))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deletionCommandRejectsInvalidTransferTarget() throws Exception {
    UserEntity owner = createActiveUser("invalid-transfer-owner@example.com", "invalidOwner",
        "Password123");
    UserEntity outsider = createActiveUser("invalid-transfer-outsider@example.com",
        "invalidOutsider", "Password123");
    Room room = createRoom(owner.getId(), "Invalid transfer room");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(owner.getEmail(), "Password123", csrf, null);

    mockMvc.perform(post("/api/users/me/deletion")
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType("application/json")
            .content("""
                {
                  "actions": [
                    {
                      "roomId": %d,
                      "mode": "TRANSFER_OWNERSHIP",
                      "transferToUserId": %d
                    }
                  ]
                }
                """.formatted(room.getId(), outsider.getId())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_OWNERSHIP_TRANSFER"));
  }

  @Test
  void deletionCommandRejectsMissingRoomAction() throws Exception {
    UserEntity owner = createActiveUser("missing-action-owner@example.com", "missingActionOwner",
        "Password123");
    Room firstRoom = createRoom(owner.getId(), "First room");
    createRoom(owner.getId(), "Second room");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(owner.getEmail(), "Password123", csrf, null);

    mockMvc.perform(post("/api/users/me/deletion")
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType("application/json")
            .content("""
                {
                  "actions": [
                    {
                      "roomId": %d,
                      "mode": "DELETE_ROOM"
                    }
                  ]
                }
                """.formatted(firstRoom.getId())))
        .andExpect(status().isBadRequest());
  }

  private Room createRoom(Integer ownerId, String roomName) {
    RoomCreationRequest request = new RoomCreationRequest();
    request.setIsPublic(true);
    request.setCategory("SPORT");
    request.setName(roomName);
    request.setDescription("Room used in account deletion integration tests");
    request.setMaximumNumberOfPeople(10);
    request.setDateOfStartEvent(Instant.now().plusSeconds(3600));
    request.setDateOfEndEvent(Instant.now().plusSeconds(7200));
    request.setAgeRating(18);
    return roomRepository.createRoom(ownerId, request);
  }
}
