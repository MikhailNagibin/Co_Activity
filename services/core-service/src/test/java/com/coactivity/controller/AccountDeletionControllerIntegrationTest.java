package com.coactivity.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coactivity.TestcontainersConfiguration;
import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.persistence.entity.PictureEntity;
import com.coactivity.persistence.entity.UserAvatarEntity;
import com.coactivity.persistence.entity.UserEntity;
import com.coactivity.persistence.repository.PictureJpaRepository;
import com.coactivity.persistence.repository.RoomJpaRepository;
import com.coactivity.persistence.repository.UserAvatarJpaRepository;
import com.coactivity.repository.RoomRepository;
import com.coactivity.service.NotificationService;
import com.coactivity.support.AbstractSessionWebIntegrationTest;
import com.coactivity.support.TestImageFactory;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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

  @Autowired
  private PictureJpaRepository pictureJpaRepository;

  @Autowired
  private UserAvatarJpaRepository userAvatarJpaRepository;

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
  void deleteWithoutOwnedRoomsAlsoRemovesAvatarMetadataAndStoredFile() throws Exception {
    UserEntity user = createActiveUser("avatar-account-delete@example.com", "avatarDelete",
        "Password123");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(user.getEmail(), "Password123", csrf, null);
    UserAvatarEntity avatarEntity = uploadAvatarAndLoadMetadata(user.getId(), csrf, session,
        "avatar.png", MediaType.IMAGE_PNG_VALUE);

    assertTrue(java.nio.file.Files.exists(resolveStoragePath(avatarEntity.getStorageKey())));

    mockMvc.perform(delete("/api/users/me")
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    assertFalse(userJpaRepository.findById(user.getId()).isPresent());
    assertFalse(userAvatarJpaRepository.findById(avatarEntity.getId()).isPresent());
    assertFalse(java.nio.file.Files.exists(resolveStoragePath(avatarEntity.getStorageKey())));

    mockMvc.perform(get("/api/users/" + user.getId() + "/avatar"))
        .andExpect(status().isNotFound());
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
  void deletionCommandAlsoRemovesAvatarMetadataAndStoredFile() throws Exception {
    UserEntity owner = createActiveUser("avatar-complex-owner@example.com", "avatarComplexOwner",
        "Password123");
    UserEntity newOwner = createActiveUser("avatar-complex-admin@example.com",
        "avatarComplexAdmin", "Password123");

    Room transferredRoom = createRoom(owner.getId(), "Avatar transferred room");
    roomRepository.addUserToRoom(transferredRoom.getId(), newOwner.getId(), Role.ADMIN);

    CsrfContext csrf = fetchCsrf();
    Cookie session = login(owner.getEmail(), "Password123", csrf, null);
    UserAvatarEntity avatarEntity = uploadAvatarAndLoadMetadata(owner.getId(), csrf, session,
        "avatar.webp", "image/webp");

    assertTrue(java.nio.file.Files.exists(resolveStoragePath(avatarEntity.getStorageKey())));

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
                """.formatted(transferredRoom.getId(), newOwner.getId())))
        .andExpect(status().isNoContent());

    assertFalse(userJpaRepository.findById(owner.getId()).isPresent());
    assertFalse(userAvatarJpaRepository.findById(avatarEntity.getId()).isPresent());
    assertFalse(java.nio.file.Files.exists(resolveStoragePath(avatarEntity.getStorageKey())));
    assertEquals(Role.OWNER, roomRepository.getUserRoleByRoomId(transferredRoom.getId(),
        newOwner.getId()));

    mockMvc.perform(get("/api/users/" + owner.getId() + "/avatar"))
        .andExpect(status().isNotFound());
  }

  @Test
  void deletionCommandDeleteRoomActionAlsoRemovesRoomImagesFromDatabaseAndStorage()
      throws Exception {
    UserEntity owner = createActiveUser("room-image-delete-owner@example.com",
        "roomImageDeleteOwner", "Password123");
    Room deletedRoom = createRoom(owner.getId(), "Room with images");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(owner.getEmail(), "Password123", csrf, null);

    uploadRoomImage(deletedRoom.getId(), csrf, session, "room-image.png", MediaType.IMAGE_PNG_VALUE);
    PictureEntity roomImage = pictureJpaRepository
        .findAllByRoom_IdAndStorageKeyIsNotNullOrderBySortOrderAscIdAsc(deletedRoom.getId())
        .getFirst();

    assertTrue(java.nio.file.Files.exists(resolveStoragePath(roomImage.getStorageKey())));

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
                """.formatted(deletedRoom.getId())))
        .andExpect(status().isNoContent());

    assertFalse(roomJpaRepository.findById(deletedRoom.getId()).isPresent());
    assertEquals(0L, pictureJpaRepository.countByRoom_IdAndStorageKeyIsNotNull(deletedRoom.getId()));
    assertFalse(java.nio.file.Files.exists(resolveStoragePath(roomImage.getStorageKey())));
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

  private UserAvatarEntity uploadAvatarAndLoadMetadata(Integer userId, CsrfContext csrf,
      Cookie session, String fileName, String contentType) throws Exception {
    MockMultipartFile avatarFile = new MockMultipartFile(
        "file",
        fileName,
        contentType,
        bytesForContentType(contentType));

    mockMvc.perform(multipart("/api/users/me/avatar")
            .file(avatarFile)
            .with(request -> {
              request.setMethod("PUT");
              return request;
            })
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isOk());

    Integer avatarId = userJpaRepository.findById(userId)
        .orElseThrow()
        .getAvatarFile()
        .getId();
    return userAvatarJpaRepository.findById(avatarId).orElseThrow();
  }

  private void uploadRoomImage(Integer roomId, CsrfContext csrf, Cookie session, String fileName,
      String contentType) throws Exception {
    MockMultipartFile imageFile = new MockMultipartFile(
        "files",
        fileName,
        contentType,
        bytesForContentType(contentType));

    mockMvc.perform(multipart("/api/rooms/" + roomId + "/images")
            .file(imageFile)
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isOk());
  }

  private byte[] bytesForContentType(String contentType) {
    return switch (contentType) {
      case MediaType.IMAGE_PNG_VALUE -> TestImageFactory.png();
      case MediaType.IMAGE_JPEG_VALUE -> TestImageFactory.jpeg();
      case "image/webp" -> TestImageFactory.webp();
      default -> throw new IllegalArgumentException("Unsupported content type for test: " + contentType);
    };
  }
}
