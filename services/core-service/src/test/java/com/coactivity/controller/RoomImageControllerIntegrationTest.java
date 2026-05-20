package com.coactivity.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coactivity.TestcontainersConfiguration;
import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.persistence.entity.PictureEntity;
import com.coactivity.persistence.entity.UserEntity;
import com.coactivity.persistence.repository.PictureJpaRepository;
import com.coactivity.repository.RoomRepository;
import com.coactivity.service.NotificationService;
import com.coactivity.support.AbstractSessionWebIntegrationTest;
import com.coactivity.support.TestImageFactory;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Tag("docker")
@DisplayName("Room image controller integration tests")
class RoomImageControllerIntegrationTest extends AbstractSessionWebIntegrationTest {

  @Autowired
  private RoomRepository roomRepository;

  @Autowired
  private PictureJpaRepository pictureJpaRepository;

  @MockitoBean
  private NotificationService notificationService;

  @BeforeEach
  void setUp() throws Exception {
    resetState();
  }

  @Test
  void ownerCanUploadMultipleImagesAndRoomResponsesExposeThemInOrder() throws Exception {
    UserEntity owner = createActiveUser("room-images-owner@example.com", "roomImagesOwner",
        "Password123");
    Room room = createRoom(owner.getId(), "Room gallery");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(owner.getEmail(), "Password123", csrf, null);

    MockMultipartFile firstFile = new MockMultipartFile(
        "files",
        "cover.png",
        MediaType.IMAGE_PNG_VALUE,
        TestImageFactory.png());
    MockMultipartFile secondFile = new MockMultipartFile(
        "files",
        "gallery.jpg",
        MediaType.IMAGE_JPEG_VALUE,
        TestImageFactory.jpeg());

    mockMvc.perform(multipart("/api/rooms/" + room.getId() + "/images")
            .file(firstFile)
            .file(secondFile)
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].order").value(1))
        .andExpect(jsonPath("$[1].order").value(2));

    List<PictureEntity> storedImages =
        pictureJpaRepository.findAllByRoom_IdAndStorageKeyIsNotNullOrderBySortOrderAscIdAsc(
            room.getId());
    assertEquals(2, storedImages.size());
    assertTrue(java.nio.file.Files.exists(resolveStoragePath(storedImages.get(0).getStorageKey())));
    assertTrue(java.nio.file.Files.exists(resolveStoragePath(storedImages.get(1).getStorageKey())));

    mockMvc.perform(get("/api/rooms/" + room.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.imageIds[0]").value(storedImages.get(0).getId()))
        .andExpect(jsonPath("$.imageIds[1]").value(storedImages.get(1).getId()))
        .andExpect(jsonPath("$.images[0].id").value(storedImages.get(0).getId()))
        .andExpect(jsonPath("$.images[0].url")
            .value("/api/rooms/" + room.getId() + "/images/" + storedImages.get(0).getId()))
        .andExpect(jsonPath("$.images[0].order").value(1))
        .andExpect(jsonPath("$.images[1].id").value(storedImages.get(1).getId()))
        .andExpect(jsonPath("$.images[1].order").value(2));

    mockMvc.perform(get("/api/rooms"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].imageIds[0]").value(storedImages.get(0).getId()))
        .andExpect(jsonPath("$[0].images[0].id").value(storedImages.get(0).getId()))
        .andExpect(jsonPath("$[0].images[1].id").value(storedImages.get(1).getId()));
  }

  @Test
  void publicUserCanFetchRoomImageBinary() throws Exception {
    UserEntity owner = createActiveUser("room-images-public@example.com", "roomImagesPublic",
        "Password123");
    Room room = createRoom(owner.getId(), "Public room images");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(owner.getEmail(), "Password123", csrf, null);
    byte[] imageBytes = TestImageFactory.webp();

    uploadRoomImages(room.getId(), csrf, session, new MockMultipartFile(
        "files",
        "cover.webp",
        "image/webp",
        imageBytes));
    PictureEntity storedImage = pictureJpaRepository
        .findAllByRoom_IdAndStorageKeyIsNotNullOrderBySortOrderAscIdAsc(room.getId())
        .getFirst();

    mockMvc.perform(get("/api/rooms/" + room.getId() + "/images/" + storedImage.getId()))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/webp"))
        .andExpect(content().bytes(imageBytes));
  }

  @Test
  void uploadRoomImagesRejectsInvalidFilesAndSixthImage() throws Exception {
    UserEntity owner = createActiveUser("room-images-invalid@example.com", "roomImagesInvalid",
        "Password123");
    Room room = createRoom(owner.getId(), "Invalid room images");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(owner.getEmail(), "Password123", csrf, null);

    MockMultipartFile emptyFile = new MockMultipartFile(
        "files", "empty.png", MediaType.IMAGE_PNG_VALUE, new byte[0]);
    mockMvc.perform(multipart("/api/rooms/" + room.getId() + "/images")
            .file(emptyFile)
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Room image cannot be empty"));

    MockMultipartFile unsupportedFile = new MockMultipartFile(
        "files", "image.gif", MediaType.IMAGE_GIF_VALUE, TestImageFactory.invalidImagePayload());
    mockMvc.perform(multipart("/api/rooms/" + room.getId() + "/images")
            .file(unsupportedFile)
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Unsupported room image content type"));

    MockMultipartFile spoofedFile = new MockMultipartFile(
        "files", "image.png", MediaType.IMAGE_PNG_VALUE, TestImageFactory.invalidImagePayload());
    mockMvc.perform(multipart("/api/rooms/" + room.getId() + "/images")
            .file(spoofedFile)
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail")
            .value("Room image content does not match declared image type"));

    MockMultipartFile oversizedFile = new MockMultipartFile(
        "files", "big.png", MediaType.IMAGE_PNG_VALUE, new byte[5_242_881]);
    mockMvc.perform(multipart("/api/rooms/" + room.getId() + "/images")
            .file(oversizedFile)
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Room image exceeds maximum allowed size"));

    uploadRoomImages(room.getId(), csrf, session,
        image("files", "1.png", MediaType.IMAGE_PNG_VALUE, TestImageFactory.png()),
        image("files", "2.png", MediaType.IMAGE_PNG_VALUE, TestImageFactory.png()),
        image("files", "3.png", MediaType.IMAGE_PNG_VALUE, TestImageFactory.png()),
        image("files", "4.png", MediaType.IMAGE_PNG_VALUE, TestImageFactory.png()),
        image("files", "5.png", MediaType.IMAGE_PNG_VALUE, TestImageFactory.png()));

    mockMvc.perform(multipart("/api/rooms/" + room.getId() + "/images")
            .file(image("files", "6.png", MediaType.IMAGE_PNG_VALUE, TestImageFactory.png()))
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Room cannot have more than 5 images"));

    assertEquals(5L, pictureJpaRepository.countByRoom_IdAndStorageKeyIsNotNull(room.getId()));
  }

  @Test
  void nonOwnerCannotUploadOrDeleteRoomImages() throws Exception {
    UserEntity owner = createActiveUser("room-images-owner2@example.com", "roomImagesOwner2",
        "Password123");
    UserEntity participant = createActiveUser("room-images-participant@example.com",
        "roomImgParticipant", "Password123");
    Room room = createRoom(owner.getId(), "Protected room images");
    roomRepository.addUserToRoom(room.getId(), participant.getId(), Role.PARTICIPANT);

    CsrfContext ownerCsrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", ownerCsrf, null);
    uploadRoomImages(room.getId(), ownerCsrf, ownerSession,
        image("files", "cover.png", MediaType.IMAGE_PNG_VALUE, TestImageFactory.png()));
    PictureEntity storedImage = pictureJpaRepository
        .findAllByRoom_IdAndStorageKeyIsNotNullOrderBySortOrderAscIdAsc(room.getId())
        .getFirst();

    CsrfContext participantCsrf = fetchCsrf();
    Cookie participantSession = login(participant.getEmail(), "Password123", participantCsrf, null);

    mockMvc.perform(multipart("/api/rooms/" + room.getId() + "/images")
            .file(image("files", "new.png", MediaType.IMAGE_PNG_VALUE, TestImageFactory.png()))
            .cookie(participantCsrf.cookie(), participantSession)
            .header("X-XSRF-TOKEN", participantCsrf.token()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.detail").value("Only room owner can manage room images"));

    mockMvc.perform(delete("/api/rooms/" + room.getId() + "/images/" + storedImage.getId())
            .cookie(participantCsrf.cookie(), participantSession)
            .header("X-XSRF-TOKEN", participantCsrf.token()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.detail").value("Only room owner can manage room images"));
  }

  @Test
  void deleteRoomImageRemovesStoredFileAndReordersRemainingImages() throws Exception {
    UserEntity owner = createActiveUser("room-images-delete@example.com", "roomImagesDelete",
        "Password123");
    Room room = createRoom(owner.getId(), "Delete room images");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(owner.getEmail(), "Password123", csrf, null);

    uploadRoomImages(room.getId(), csrf, session,
        image("files", "1.png", MediaType.IMAGE_PNG_VALUE, TestImageFactory.png()),
        image("files", "2.png", MediaType.IMAGE_PNG_VALUE, TestImageFactory.png()),
        image("files", "3.png", MediaType.IMAGE_PNG_VALUE, TestImageFactory.png()));

    List<PictureEntity> storedImages =
        pictureJpaRepository.findAllByRoom_IdAndStorageKeyIsNotNullOrderBySortOrderAscIdAsc(
            room.getId());
    PictureEntity deletedImage = storedImages.get(1);

    mockMvc.perform(delete("/api/rooms/" + room.getId() + "/images/" + deletedImage.getId())
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].order").value(1))
        .andExpect(jsonPath("$[1].order").value(2));

    List<PictureEntity> remainingImages =
        pictureJpaRepository.findAllByRoom_IdAndStorageKeyIsNotNullOrderBySortOrderAscIdAsc(
            room.getId());
    assertEquals(2, remainingImages.size());
    assertEquals(List.of(1, 2), remainingImages.stream().map(PictureEntity::getSortOrder).toList());
    assertFalse(java.nio.file.Files.exists(resolveStoragePath(deletedImage.getStorageKey())));

    mockMvc.perform(get("/api/rooms/" + room.getId() + "/images/" + deletedImage.getId()))
        .andExpect(status().isNotFound());
  }

  @Test
  void getRoomImageRejectsImageThatBelongsToAnotherRoom() throws Exception {
    UserEntity owner = createActiveUser("room-images-cross@example.com", "roomImagesCross",
        "Password123");
    Room firstRoom = createRoom(owner.getId(), "First room");
    Room secondRoom = createRoom(owner.getId(), "Second room");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(owner.getEmail(), "Password123", csrf, null);

    uploadRoomImages(firstRoom.getId(), csrf, session,
        image("files", "1.png", MediaType.IMAGE_PNG_VALUE, TestImageFactory.png()));
    uploadRoomImages(secondRoom.getId(), csrf, session,
        image("files", "2.png", MediaType.IMAGE_PNG_VALUE, TestImageFactory.png()));

    PictureEntity secondRoomImage = pictureJpaRepository
        .findAllByRoom_IdAndStorageKeyIsNotNullOrderBySortOrderAscIdAsc(secondRoom.getId())
        .getFirst();

    mockMvc.perform(get("/api/rooms/" + firstRoom.getId() + "/images/" + secondRoomImage.getId()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Room image not found"));
  }

  @Test
  void deletingRoomAlsoRemovesAllRoomImagesFromDatabaseAndStorage() throws Exception {
    UserEntity owner = createActiveUser("room-images-room-delete@example.com",
        "roomImagesRoomDelete", "Password123");
    Room room = createRoom(owner.getId(), "Room delete cleanup");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(owner.getEmail(), "Password123", csrf, null);

    uploadRoomImages(room.getId(), csrf, session,
        image("files", "1.png", MediaType.IMAGE_PNG_VALUE, TestImageFactory.png()),
        image("files", "2.png", MediaType.IMAGE_PNG_VALUE, TestImageFactory.png()));
    List<PictureEntity> storedImages =
        pictureJpaRepository.findAllByRoom_IdAndStorageKeyIsNotNullOrderBySortOrderAscIdAsc(
            room.getId());

    mockMvc.perform(delete("/api/rooms/" + room.getId())
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    assertEquals(0L, pictureJpaRepository.countByRoom_IdAndStorageKeyIsNotNull(room.getId()));
    for (PictureEntity image : storedImages) {
      assertFalse(java.nio.file.Files.exists(resolveStoragePath(image.getStorageKey())));
    }
  }

  private Room createRoom(Integer ownerId, String roomName) {
    RoomCreationRequest request = new RoomCreationRequest();
    request.setIsPublic(true);
    request.setCategory("SPORT");
    request.setName(roomName);
    request.setDescription("Room used in room image integration tests");
    request.setMaximumNumberOfPeople(10);
    request.setDateOfStartEvent(Instant.now().plusSeconds(3600));
    request.setDateOfEndEvent(Instant.now().plusSeconds(7200));
    request.setAgeRating(18);
    return roomRepository.createRoom(ownerId, request);
  }

  private void uploadRoomImages(Integer roomId, CsrfContext csrf, Cookie session,
      MockMultipartFile... files) throws Exception {
    var request = multipart("/api/rooms/" + roomId + "/images");
    for (MockMultipartFile file : files) {
      request.file(file);
    }
    mockMvc.perform(request
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isOk());
  }

  private MockMultipartFile image(String fieldName, String fileName, String contentType,
      byte[] body) {
    return new MockMultipartFile(
        fieldName,
        fileName,
        contentType,
        body);
  }
}
