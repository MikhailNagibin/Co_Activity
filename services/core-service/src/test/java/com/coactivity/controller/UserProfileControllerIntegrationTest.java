package com.coactivity.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coactivity.TestcontainersConfiguration;
import com.coactivity.persistence.entity.UserAvatarEntity;
import com.coactivity.persistence.entity.UserEntity;
import com.coactivity.persistence.repository.UserAvatarJpaRepository;
import com.coactivity.support.AbstractSessionWebIntegrationTest;
import com.coactivity.support.TestImageFactory;
import jakarta.servlet.http.Cookie;
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

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Tag("docker")
@DisplayName("User profile controller integration tests")
class UserProfileControllerIntegrationTest extends AbstractSessionWebIntegrationTest {

  @Autowired
  private UserAvatarJpaRepository userAvatarJpaRepository;

  @BeforeEach
  void setUp() throws Exception {
    resetState();
  }

  @Test
  void authenticatedUserCanUpdateProfileAndLoadItBack() throws Exception {
    UserEntity user = createActiveUser("profile-user@example.com", "profileUser", "Password123");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(user.getEmail(), "Password123", csrf, null);

    mockMvc.perform(put("/api/users/me")
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "username": "updatedName",
                  "dateOfBirth": "2001-02-03T00:00:00Z",
                  "city": "Saint Petersburg",
                  "country": "Russia",
                  "description": "Updated from integration test",
                  "avatarId": 77
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("updatedName"))
        .andExpect(jsonPath("$.city").value("Saint Petersburg"))
        .andExpect(jsonPath("$.avatarId").value(77));

    mockMvc.perform(get("/api/users/me").cookie(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("profile-user@example.com"))
        .andExpect(jsonPath("$.username").value("updatedName"))
        .andExpect(jsonPath("$.country").value("Russia"))
        .andExpect(jsonPath("$.description").value("Updated from integration test"));
  }

  @Test
  void authenticatedUserCanUploadAvatarAndPublicEndpointReturnsIt() throws Exception {
    UserEntity user = createActiveUser("avatar-user@example.com", "avatarUser", "Password123");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(user.getEmail(), "Password123", csrf, null);
    byte[] imageBytes = TestImageFactory.png();
    MockMultipartFile avatarFile = new MockMultipartFile(
        "file",
        "avatar.png",
        MediaType.IMAGE_PNG_VALUE,
        imageBytes);

    mockMvc.perform(multipart("/api/users/me/avatar")
            .file(avatarFile)
            .with(request -> {
              request.setMethod("PUT");
              return request;
            })
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avatarUrl").value("/api/users/" + user.getId() + "/avatar"));

    mockMvc.perform(get("/api/users/" + user.getId() + "/avatar"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
        .andExpect(content().bytes(imageBytes));
  }

  @Test
  void uploadingAvatarTwiceReplacesPreviousMetadataAndStoredFile() throws Exception {
    UserEntity user = createActiveUser("replace-avatar@example.com", "replaceAvatar",
        "Password123");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(user.getEmail(), "Password123", csrf, null);

    byte[] firstImageBytes = TestImageFactory.png();
    MockMultipartFile firstAvatar = new MockMultipartFile(
        "file",
        "first.png",
        MediaType.IMAGE_PNG_VALUE,
        firstImageBytes);

    mockMvc.perform(multipart("/api/users/me/avatar")
            .file(firstAvatar)
            .with(request -> {
              request.setMethod("PUT");
              return request;
            })
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isOk());

    Integer firstAvatarId = userJpaRepository.findById(user.getId())
        .orElseThrow()
        .getAvatarFile()
        .getId();
    UserAvatarEntity firstAvatarEntity = userAvatarJpaRepository.findById(firstAvatarId)
        .orElseThrow();
    assertEquals(1L, userAvatarJpaRepository.count());
    assertTrue(java.nio.file.Files.exists(resolveStoragePath(firstAvatarEntity.getStorageKey())));

    byte[] secondImageBytes = TestImageFactory.jpeg();
    MockMultipartFile secondAvatar = new MockMultipartFile(
        "file",
        "second.jpg",
        MediaType.IMAGE_JPEG_VALUE,
        secondImageBytes);

    mockMvc.perform(multipart("/api/users/me/avatar")
            .file(secondAvatar)
            .with(request -> {
              request.setMethod("PUT");
              return request;
            })
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avatarUrl").value("/api/users/" + user.getId() + "/avatar"));

    Integer secondAvatarId = userJpaRepository.findById(user.getId())
        .orElseThrow()
        .getAvatarFile()
        .getId();
    UserAvatarEntity secondAvatarEntity = userAvatarJpaRepository.findById(secondAvatarId)
        .orElseThrow();

    assertNotEquals(firstAvatarId, secondAvatarId);
    assertEquals(1L, userAvatarJpaRepository.count());
    assertFalse(userAvatarJpaRepository.findById(firstAvatarId).isPresent());
    assertFalse(java.nio.file.Files.exists(resolveStoragePath(firstAvatarEntity.getStorageKey())));
    assertTrue(java.nio.file.Files.exists(resolveStoragePath(secondAvatarEntity.getStorageKey())));

    mockMvc.perform(get("/api/users/" + user.getId() + "/avatar"))
        .andExpect(status().isOk())
        .andExpect(header().longValue("Content-Length", secondImageBytes.length))
        .andExpect(header().string("Cache-Control", containsString("max-age=3600")))
        .andExpect(content().contentType(MediaType.IMAGE_JPEG))
        .andExpect(content().bytes(secondImageBytes));
  }

  @Test
  void authenticatedUserCanDeleteAvatarIdempotently() throws Exception {
    UserEntity user = createActiveUser("delete-avatar@example.com", "deleteAvatar", "Password123");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(user.getEmail(), "Password123", csrf, null);
    MockMultipartFile avatarFile = new MockMultipartFile(
        "file",
        "avatar.webp",
        "image/webp",
        TestImageFactory.webp());

    mockMvc.perform(multipart("/api/users/me/avatar")
            .file(avatarFile)
            .with(request -> {
              request.setMethod("PUT");
              return request;
            })
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isOk());

    mockMvc.perform(delete("/api/users/me/avatar")
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(delete("/api/users/me/avatar")
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/users/" + user.getId() + "/avatar"))
        .andExpect(status().isNotFound());
  }

  @Test
  void unauthenticatedUserCannotUploadOrDeleteAvatar() throws Exception {
    CsrfContext csrf = fetchCsrf();
    MockMultipartFile avatarFile = new MockMultipartFile(
        "file",
        "avatar.jpg",
        MediaType.IMAGE_JPEG_VALUE,
        TestImageFactory.jpeg());

    mockMvc.perform(multipart("/api/users/me/avatar")
            .file(avatarFile)
            .with(request -> {
              request.setMethod("PUT");
              return request;
            })
            .cookie(csrf.cookie())
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(delete("/api/users/me/avatar")
            .cookie(csrf.cookie())
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void uploadAvatarRejectsEmptyUnsupportedAndOversizedFiles() throws Exception {
    UserEntity user = createActiveUser("invalid-avatar@example.com", "invalidAvatar", "Password123");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(user.getEmail(), "Password123", csrf, null);

    MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png",
        MediaType.IMAGE_PNG_VALUE, new byte[0]);
    mockMvc.perform(multipart("/api/users/me/avatar")
            .file(emptyFile)
            .with(request -> {
              request.setMethod("PUT");
              return request;
            })
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Avatar file cannot be empty"));

    MockMultipartFile unsupportedFile = new MockMultipartFile("file", "avatar.gif",
        MediaType.IMAGE_GIF_VALUE, TestImageFactory.invalidImagePayload());
    mockMvc.perform(multipart("/api/users/me/avatar")
            .file(unsupportedFile)
            .with(request -> {
              request.setMethod("PUT");
              return request;
            })
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Unsupported avatar content type"));

    MockMultipartFile spoofedFile = new MockMultipartFile("file", "avatar.png",
        MediaType.IMAGE_PNG_VALUE, TestImageFactory.invalidImagePayload());
    mockMvc.perform(multipart("/api/users/me/avatar")
            .file(spoofedFile)
            .with(request -> {
              request.setMethod("PUT");
              return request;
            })
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail")
            .value("Avatar file content does not match declared image type"));

    MockMultipartFile oversizedFile = new MockMultipartFile("file", "avatar.png",
        MediaType.IMAGE_PNG_VALUE, new byte[5_242_881]);
    mockMvc.perform(multipart("/api/users/me/avatar")
            .file(oversizedFile)
            .with(request -> {
              request.setMethod("PUT");
              return request;
            })
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Avatar file exceeds maximum allowed size"));
  }

  @Test
  void uploadedAvatarIsReturnedInOwnAndPublicProfileResponses() throws Exception {
    UserEntity owner = createActiveUser("profile-avatar-owner@example.com", "profileAvatarOwner",
        "Password123");
    UserEntity viewer = createActiveUser("profile-avatar-viewer@example.com",
        "profileAvatarViewer", "Password123");

    CsrfContext ownerCsrf = fetchCsrf();
    Cookie ownerSession = login(owner.getEmail(), "Password123", ownerCsrf, null);
    MockMultipartFile avatarFile = new MockMultipartFile(
        "file",
        "avatar.webp",
        "image/webp",
        TestImageFactory.webp());

    mockMvc.perform(multipart("/api/users/me/avatar")
            .file(avatarFile)
            .with(request -> {
              request.setMethod("PUT");
              return request;
            })
            .cookie(ownerCsrf.cookie(), ownerSession)
            .header("X-XSRF-TOKEN", ownerCsrf.token()))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/users/me").cookie(ownerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avatarUrl").value("/api/users/" + owner.getId() + "/avatar"));

    CsrfContext viewerCsrf = fetchCsrf();
    Cookie viewerSession = login(viewer.getEmail(), "Password123", viewerCsrf, null);

    mockMvc.perform(get("/api/users/" + owner.getId()).cookie(viewerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(owner.getId()))
        .andExpect(jsonPath("$.avatarUrl").value("/api/users/" + owner.getId() + "/avatar"));

    Integer avatarId = userJpaRepository.findById(owner.getId())
        .orElseThrow()
        .getAvatarFile()
        .getId();
    UserAvatarEntity avatarEntity = userAvatarJpaRepository.findById(avatarId).orElseThrow();
    assertNotNull(avatarEntity.getStorageKey());
  }

  @Test
  void updatingProfileRejectsCaseInsensitiveDuplicateUsername() throws Exception {
    UserEntity owner = createActiveUser("dup-owner@example.com", "ownerName", "Password123");
    UserEntity target = createActiveUser("dup-target@example.com", "targetName", "Password123");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(target.getEmail(), "Password123", csrf, null);

    mockMvc.perform(put("/api/users/me")
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "username": "OWNERNAME"
                }
                """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("USERNAME_ALREADY_TAKEN"));

    assertEquals("targetName", userJpaRepository.findById(target.getId()).orElseThrow().getUserName());
    assertEquals("ownerName", userJpaRepository.findById(owner.getId()).orElseThrow().getUserName());
  }

  @Test
  void authenticatedUserCanUpdateNotificationSettings() throws Exception {
    UserEntity user = createActiveUser("notify-user@example.com", "notifyUser", "Password123");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(user.getEmail(), "Password123", csrf, null);

    mockMvc.perform(put("/api/users/me/notifications")
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "membershipAccepted": true,
                  "membershipRejected": false,
                  "activityClosed": true,
                  "newJoinRequest": true,
                  "importantRoomUpdates": true
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.membershipAccepted").value(true))
        .andExpect(jsonPath("$.membershipRejected").value(false))
        .andExpect(jsonPath("$.activityClosed").value(true))
        .andExpect(jsonPath("$.newJoinRequest").value(true))
        .andExpect(jsonPath("$.importantRoomUpdates").value(true));

    mockMvc.perform(get("/api/users/me/notifications").cookie(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.membershipAccepted").value(true))
        .andExpect(jsonPath("$.membershipRejected").value(false))
        .andExpect(jsonPath("$.activityClosed").value(true))
        .andExpect(jsonPath("$.newJoinRequest").value(true))
        .andExpect(jsonPath("$.importantRoomUpdates").value(true));

    mockMvc.perform(get("/api/users/me").cookie(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notifications", hasItems(
            "MEMBERSHIP_ACCEPTED",
            "ACTIVITY_CLOSED",
            "NEW_JOIN_REQUEST",
            "IMPORTANT_ROOM_UPDATES")));
  }

  @Test
  void authenticatedUserCanFetchAnotherUsersPublicProfile() throws Exception {
    UserEntity viewer = createActiveUser("viewer@example.com", "viewer", "Password123");
    UserEntity target = createActiveUser("public-target@example.com", "publicTarget", "Password123");
    userJpaRepository.findById(target.getId()).ifPresent(entity -> {
      entity.setCity("Kazan");
      entity.setCountry("Russia");
      entity.setDescription("Visible public bio");
      entity.setAvatarId(15);
      userJpaRepository.saveAndFlush(entity);
    });

    CsrfContext csrf = fetchCsrf();
    Cookie session = login(viewer.getEmail(), "Password123", csrf, null);

    mockMvc.perform(get("/api/users/" + target.getId()).cookie(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(target.getId()))
        .andExpect(jsonPath("$.userName").value("publicTarget"))
        .andExpect(jsonPath("$.city").value("Kazan"))
        .andExpect(jsonPath("$.country").value("Russia"))
        .andExpect(jsonPath("$.description").value("Visible public bio"))
        .andExpect(jsonPath("$.avatarId").value(15))
        .andExpect(jsonPath("$.followersCount").value(0));
  }

  @Test
  void followSelfReturnsValidationError() throws Exception {
    UserEntity user = createActiveUser("self-follow@example.com", "selfFollow", "Password123");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(user.getEmail(), "Password123", csrf, null);

    mockMvc.perform(post("/api/users/" + user.getId() + "/follow")
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  void followCreatesSubscription() throws Exception {
    UserEntity follower = createActiveUser("follow-ok-follower@example.com", "followOkFollower",
        "Password123");
    UserEntity followed = createActiveUser("follow-ok-target@example.com", "followOkTarget",
        "Password123");
    CsrfContext followerCsrf = fetchCsrf();
    Cookie followerSession = login(follower.getEmail(), "Password123", followerCsrf, null);

    mockMvc.perform(post("/api/users/" + followed.getId() + "/follow")
            .cookie(followerCsrf.cookie(), followerSession)
            .header("X-XSRF-TOKEN", followerCsrf.token()))
        .andExpect(status().isNoContent());
  }

  @Test
  void duplicateFollowReturnsConflictWithExpectedCode() throws Exception {
    UserEntity follower = createActiveUser("dup-follow-follower@example.com", "dupFollowFollower",
        "Password123");
    UserEntity followed = createActiveUser("dup-follow-target@example.com", "dupFollowTarget",
        "Password123");
    CsrfContext followerCsrf = fetchCsrf();
    Cookie followerSession = login(follower.getEmail(), "Password123", followerCsrf, null);

    mockMvc.perform(post("/api/users/" + followed.getId() + "/follow")
            .cookie(followerCsrf.cookie(), followerSession)
            .header("X-XSRF-TOKEN", followerCsrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/users/" + followed.getId() + "/follow")
            .cookie(followerCsrf.cookie(), followerSession)
            .header("X-XSRF-TOKEN", followerCsrf.token()))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("ALREADY_FOLLOWING"));
  }

  @Test
  void unfollowWithoutExistingSubscriptionReturnsConflictWithExpectedCode() throws Exception {
    UserEntity follower = createActiveUser("not-following-follower@example.com",
        "notFollowingFollower", "Password123");
    UserEntity followed = createActiveUser("not-following-target@example.com", "notFollowingTarget",
        "Password123");
    CsrfContext followerCsrf = fetchCsrf();
    Cookie followerSession = login(follower.getEmail(), "Password123", followerCsrf, null);

    mockMvc.perform(delete("/api/users/" + followed.getId() + "/follow")
            .cookie(followerCsrf.cookie(), followerSession)
            .header("X-XSRF-TOKEN", followerCsrf.token()))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("NOT_FOLLOWING"));
  }

  @Test
  void currentUserCanReadFollowingAndFollowersLists() throws Exception {
    UserEntity follower = createActiveUser("list-follower@example.com", "listFollower", "Password123");
    UserEntity followed = createActiveUser("list-followed@example.com", "listFollowed", "Password123");

    CsrfContext followerCsrf = fetchCsrf();
    Cookie followerSession = login(follower.getEmail(), "Password123", followerCsrf, null);
    CsrfContext followedCsrf = fetchCsrf();
    Cookie followedSession = login(followed.getEmail(), "Password123", followedCsrf, null);

    mockMvc.perform(post("/api/users/" + followed.getId() + "/follow")
            .cookie(followerCsrf.cookie(), followerSession)
            .header("X-XSRF-TOKEN", followerCsrf.token()))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/users/me/following").cookie(followerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(followed.getId()))
        .andExpect(jsonPath("$[0].userName").value("listFollowed"));

    mockMvc.perform(get("/api/users/me/followers").cookie(followedSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(follower.getId()))
        .andExpect(jsonPath("$[0].userName").value("listFollower"));
  }

  @Test
  void unauthenticatedUserCannotReadOwnProfile() throws Exception {
    mockMvc.perform(get("/api/users/me"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void updatingProfileRejectsFutureBirthDate() throws Exception {
    UserEntity user = createActiveUser("future-birth@example.com", "futureBirth", "Password123");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(user.getEmail(), "Password123", csrf, null);

    mockMvc.perform(put("/api/users/me")
            .cookie(csrf.cookie(), session)
            .header("X-XSRF-TOKEN", csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "dateOfBirth": "2999-01-01T00:00:00Z"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Validation failed"));
  }

  @Test
  void authenticatedUserGetsNotFoundForMissingPublicProfile() throws Exception {
    UserEntity viewer = createActiveUser("missing-viewer@example.com", "missingViewer",
        "Password123");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(viewer.getEmail(), "Password123", csrf, null);

    mockMvc.perform(get("/api/users/999999").cookie(session))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("User not found"));
  }
}
