package com.coactivity.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coactivity.TestcontainersConfiguration;
import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.domain.Room;
import com.coactivity.persistence.entity.PictureEntity;
import com.coactivity.persistence.entity.UserAvatarEntity;
import com.coactivity.persistence.entity.UserEntity;
import com.coactivity.persistence.repository.PictureJpaRepository;
import com.coactivity.persistence.repository.UserAvatarJpaRepository;
import com.coactivity.repository.RoomRepository;
import com.coactivity.service.NotificationService;
import com.coactivity.service.RoomImageService;
import com.coactivity.service.UserAvatarService;
import com.coactivity.storage.FileStorage;
import com.coactivity.storage.S3FileStorage;
import com.coactivity.support.AbstractSessionWebIntegrationTest;
import com.coactivity.support.MinioTestSupport;
import com.coactivity.support.TestImageFactory;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3Client;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Testcontainers
@Tag("docker")
@DisplayName("S3 file storage controller integration tests")
class S3FileStorageControllerIntegrationTest extends AbstractSessionWebIntegrationTest {

  private static final String BUCKET = "coactivity-storage-integration-test";
  private static final String PREFIX = "integration-prefix";

  @Container
  static final GenericContainer<?> minio = MinioTestSupport.newContainer();

  private static S3Client s3Client;

  @DynamicPropertySource
  static void configureS3Storage(DynamicPropertyRegistry registry) {
    registry.add("app.storage.type", () -> "s3");
    registry.add("app.storage.s3.endpoint", () -> MinioTestSupport.endpoint(minio));
    registry.add("app.storage.s3.region", () -> MinioTestSupport.REGION);
    registry.add("app.storage.s3.bucket", () -> BUCKET);
    registry.add("app.storage.s3.access-key", () -> MinioTestSupport.ACCESS_KEY);
    registry.add("app.storage.s3.secret-key", () -> MinioTestSupport.SECRET_KEY);
    registry.add("app.storage.s3.path-style", () -> "true");
    registry.add("app.storage.s3.prefix", () -> PREFIX);
  }

  @Autowired
  private FileStorage fileStorage;

  @Autowired
  private UserAvatarJpaRepository userAvatarJpaRepository;

  @Autowired
  private PictureJpaRepository pictureJpaRepository;

  @Autowired
  private RoomRepository roomRepository;

  @Autowired
  private UserAvatarService userAvatarService;

  @Autowired
  private RoomImageService roomImageService;

  @Autowired
  private TransactionTemplate transactionTemplate;

  @MockitoBean
  private NotificationService notificationService;

  @BeforeAll
  static void startS3Client() {
    s3Client = MinioTestSupport.createClient(MinioTestSupport.endpoint(minio));
    MinioTestSupport.ensureBucket(s3Client, BUCKET);
  }

  @AfterAll
  static void closeS3Client() {
    if (s3Client != null) {
      s3Client.close();
    }
  }

  @BeforeEach
  void setUp() throws Exception {
    MinioTestSupport.ensureBucket(s3Client, BUCKET);
    MinioTestSupport.cleanBucket(s3Client, BUCKET);
    resetState();
  }

  @Test
  void applicationUsesS3StorageBeanWhenStorageTypeIsS3() {
    assertTrue(fileStorage instanceof S3FileStorage);
  }

  @Test
  void avatarUploadGetDeleteUsesS3AndKeepsStorageKeyFormat() throws Exception {
    UserEntity user = createActiveUser("s3-avatar@example.com", "s3Avatar", "Password123");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(user.getEmail(), "Password123", csrf, null);
    byte[] imageBytes = TestImageFactory.png();

    mockMvc.perform(multipart("/api/users/me/avatar")
            .file(new MockMultipartFile("file", "avatar.png", MediaType.IMAGE_PNG_VALUE,
                imageBytes))
            .with(request -> {
              request.setMethod("PUT");
              return request;
            })
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avatarUrl").value("/api/users/" + user.getId() + "/avatar"));

    UserAvatarEntity avatar = userAvatarJpaRepository.findAll().getFirst();
    assertTrue(avatar.getStorageKey().matches("avatars/[0-9a-f\\-]{36}\\.png"));
    assertTrue(fileStorage.exists(avatar.getStorageKey()));
    assertTrue(MinioTestSupport.objectExists(
        s3Client, BUCKET, PREFIX + "/" + avatar.getStorageKey()));

    mockMvc.perform(get("/api/users/" + user.getId() + "/avatar"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
        .andExpect(content().bytes(imageBytes));

    mockMvc.perform(delete("/api/users/me/avatar")
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    assertFalse(fileStorage.exists(avatar.getStorageKey()));
    mockMvc.perform(get("/api/users/" + user.getId() + "/avatar"))
        .andExpect(status().isNotFound());
  }

  @Test
  void roomImageUploadGetDeleteUsesS3AndKeepsStorageKeyFormat() throws Exception {
    UserEntity owner = createActiveUser("s3-room-image@example.com", "s3RoomImage",
        "Password123");
    Room room = createRoom(owner.getId(), "S3 room image");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(owner.getEmail(), "Password123", csrf, null);
    byte[] imageBytes = TestImageFactory.jpeg();

    uploadRoomImages(room.getId(), csrf, session,
        new MockMultipartFile("files", "cover.jpg", MediaType.IMAGE_JPEG_VALUE, imageBytes));

    PictureEntity image = pictureJpaRepository
        .findAllByRoom_IdAndStorageKeyIsNotNullOrderBySortOrderAscIdAsc(room.getId())
        .getFirst();
    assertTrue(image.getStorageKey().matches("room-images/[0-9a-f\\-]{36}\\.jpg"));
    assertTrue(fileStorage.exists(image.getStorageKey()));
    assertTrue(MinioTestSupport.objectExists(
        s3Client, BUCKET, PREFIX + "/" + image.getStorageKey()));

    mockMvc.perform(get("/api/rooms/" + room.getId() + "/images/" + image.getId()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_JPEG))
        .andExpect(content().bytes(imageBytes));

    mockMvc.perform(delete("/api/rooms/" + room.getId() + "/images/" + image.getId())
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isOk());

    assertFalse(fileStorage.exists(image.getStorageKey()));
    mockMvc.perform(get("/api/rooms/" + room.getId() + "/images/" + image.getId()))
        .andExpect(status().isNotFound());
  }

  @Test
  void missingAvatarObjectReturns404WithoutChangingApiContract() throws Exception {
    UserEntity user = createActiveUser("s3-missing-avatar@example.com", "s3MissingAvatar",
        "Password123");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(user.getEmail(), "Password123", csrf, null);

    mockMvc.perform(multipart("/api/users/me/avatar")
            .file(new MockMultipartFile("file", "avatar.webp", "image/webp",
                TestImageFactory.webp()))
            .with(request -> {
              request.setMethod("PUT");
              return request;
            })
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isOk());

    UserAvatarEntity avatar = userAvatarJpaRepository.findAll().getFirst();
    fileStorage.delete(avatar.getStorageKey());

    mockMvc.perform(get("/api/users/" + user.getId() + "/avatar"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Avatar not found"));
  }

  @Test
  void missingRoomImageObjectReturns404WithoutChangingApiContract() throws Exception {
    UserEntity owner = createActiveUser("s3-missing-room-image@example.com", "s3MissingRoom",
        "Password123");
    Room room = createRoom(owner.getId(), "S3 missing room image");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(owner.getEmail(), "Password123", csrf, null);

    uploadRoomImages(room.getId(), csrf, session,
        new MockMultipartFile("files", "cover.png", MediaType.IMAGE_PNG_VALUE,
            TestImageFactory.png()));
    PictureEntity image = pictureJpaRepository
        .findAllByRoom_IdAndStorageKeyIsNotNullOrderBySortOrderAscIdAsc(room.getId())
        .getFirst();
    fileStorage.delete(image.getStorageKey());

    mockMvc.perform(get("/api/rooms/" + room.getId() + "/images/" + image.getId()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Room image not found"));
  }

  @Test
  void rolledBackAvatarDeleteDoesNotDeleteS3ObjectBeforeCommit() throws Exception {
    UserEntity user = createActiveUser("s3-avatar-rollback@example.com", "s3AvatarRollback",
        "Password123");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(user.getEmail(), "Password123", csrf, null);

    mockMvc.perform(multipart("/api/users/me/avatar")
            .file(new MockMultipartFile("file", "avatar.png", MediaType.IMAGE_PNG_VALUE,
                TestImageFactory.png()))
            .with(request -> {
              request.setMethod("PUT");
              return request;
            })
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isOk());

    UserAvatarEntity avatar = userAvatarJpaRepository.findAll().getFirst();
    assertTrue(fileStorage.exists(avatar.getStorageKey()));

    assertThrows(RuntimeException.class, () -> transactionTemplate.executeWithoutResult(status -> {
      userAvatarService.deleteAvatar(user.getId());
      throw new RuntimeException("force rollback");
    }));

    assertTrue(fileStorage.exists(avatar.getStorageKey()));
    assertTrue(userAvatarJpaRepository.findById(avatar.getId()).isPresent());
    assertNotNull(userJpaRepository.findById(user.getId()).orElseThrow().getAvatarFile());
  }

  @Test
  void rolledBackRoomImageDeleteDoesNotDeleteS3ObjectBeforeCommit() throws Exception {
    UserEntity owner = createActiveUser("s3-room-rollback@example.com", "s3RoomRollback",
        "Password123");
    Room room = createRoom(owner.getId(), "S3 room image rollback");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(owner.getEmail(), "Password123", csrf, null);
    uploadRoomImages(room.getId(), csrf, session,
        new MockMultipartFile("files", "cover.png", MediaType.IMAGE_PNG_VALUE,
            TestImageFactory.png()));

    PictureEntity image = pictureJpaRepository
        .findAllByRoom_IdAndStorageKeyIsNotNullOrderBySortOrderAscIdAsc(room.getId())
        .getFirst();
    assertTrue(fileStorage.exists(image.getStorageKey()));

    assertThrows(RuntimeException.class, () -> transactionTemplate.executeWithoutResult(status -> {
      roomImageService.deleteRoomImage(owner.getId(), room.getId(), image.getId());
      throw new RuntimeException("force rollback");
    }));

    assertTrue(fileStorage.exists(image.getStorageKey()));
    assertTrue(pictureJpaRepository.findById(image.getId()).isPresent());
    assertEquals(1L, pictureJpaRepository.countByRoom_IdAndStorageKeyIsNotNull(room.getId()));
  }

  private Room createRoom(Integer ownerId, String roomName) {
    RoomCreationRequest request = new RoomCreationRequest();
    request.setIsPublic(true);
    request.setCategory("SPORT");
    request.setName(roomName);
    request.setDescription("Room used in S3 image integration tests");
    request.setCity("Moscow");
    request.setCountry("Russia");
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
}
