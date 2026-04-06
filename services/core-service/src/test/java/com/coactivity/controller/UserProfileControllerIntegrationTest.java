package com.coactivity.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coactivity.TestcontainersConfiguration;
import com.coactivity.persistence.entity.UserEntity;
import com.coactivity.support.AbstractSessionWebIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Tag("docker")
@DisplayName("User profile controller integration tests")
class UserProfileControllerIntegrationTest extends AbstractSessionWebIntegrationTest {

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
                  "newJoinRequest": true
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.membershipAccepted").value(true))
        .andExpect(jsonPath("$.membershipRejected").value(false))
        .andExpect(jsonPath("$.activityClosed").value(true))
        .andExpect(jsonPath("$.newJoinRequest").value(true));

    mockMvc.perform(get("/api/users/me").cookie(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notifications", hasItems(
            "MEMBERSHIP_ACCEPTED",
            "ACTIVITY_CLOSED",
            "NEW_JOIN_REQUEST")));
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
        .andExpect(jsonPath("$.avatarId").value(15));
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
        .andExpect(jsonPath("$.message").value("Validation failed"));
  }

  @Test
  void authenticatedUserGetsNotFoundForMissingPublicProfile() throws Exception {
    UserEntity viewer = createActiveUser("missing-viewer@example.com", "missingViewer",
        "Password123");
    CsrfContext csrf = fetchCsrf();
    Cookie session = login(viewer.getEmail(), "Password123", csrf, null);

    mockMvc.perform(get("/api/users/999999").cookie(session))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("User not found"));
  }
}
