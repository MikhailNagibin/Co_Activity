package com.coactivity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coactivity.controller.dto.request.NotificationSettingsRequest;
import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.response.NotificationSettingsResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.Notification;
import com.coactivity.domain.User;
import com.coactivity.repository.UserRepository;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.ValidationException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("UserProfileService tests")
class UserProfileServiceTest {

  private UserRepository userRepository;
  private UserProfileService userProfileService;

  @BeforeEach
  void setUp() {
    userRepository = Mockito.mock(UserRepository.class);
    userProfileService = new UserProfileService(userRepository);
  }

  @Test
  void getUserProfile_returnsMappedProfile() {
    User user = user(7, List.of(Notification.MEMBERSHIP_ACCEPTED, Notification.NEW_JOIN_REQUEST));
    when(userRepository.getUserById(7)).thenReturn(user);

    UserProfileResponse response = userProfileService.getUserProfile(7);

    assertEquals(7, response.getId());
    assertEquals("user@example.com", response.getEmail());
    assertEquals("userName", response.getUsername());
    assertEquals("Moscow", response.getCity());
    assertEquals(2, response.getNotifications().size());
  }

  @Test
  void getPublicUserProfileById_rejectsNullUserId() {
    ValidationException exception = assertThrows(ValidationException.class,
        () -> userProfileService.getPublicUserProfileById(null));

    assertEquals("User id is required", exception.getMessage());
  }

  @Test
  void getUserProfile_rejectsNullUserId() {
    ValidationException exception = assertThrows(ValidationException.class,
        () -> userProfileService.getUserProfile(null));

    assertEquals("User id is required", exception.getMessage());
  }

  @Test
  void getPublicUserProfileById_returnsPublicSummary() {
    User user = user(7, List.of(Notification.MEMBERSHIP_ACCEPTED));
    when(userRepository.getUserById(7)).thenReturn(user);

    UserSummaryResponse response = userProfileService.getPublicUserProfileById(7);

    assertEquals(7, response.getId());
    assertEquals("userName", response.getUserName());
    assertEquals("Moscow", response.getCity());
    assertEquals("Russia", response.getCountry());
  }

  @Test
  void updateUserProfile_requiresExistingUser() {
    when(userRepository.getUserById(7)).thenReturn(null);

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> userProfileService.updateUserProfile(7, new UserProfileUpdateRequest()));

    assertEquals("User not found", exception.getMessage());
    verify(userRepository, never()).updateUser(Mockito.anyInt(), Mockito.any());
  }

  @Test
  void updateUserProfile_forwardsRequestToRepository() {
    User user = user(7, List.of());
    UserProfileUpdateRequest request = new UserProfileUpdateRequest(
        "newName",
        Instant.parse("2001-01-01T00:00:00Z"),
        "Saint Petersburg",
        "Russia",
        "Updated profile",
        42);

    when(userRepository.getUserById(7)).thenReturn(user);

    userProfileService.updateUserProfile(7, request);

    verify(userRepository).updateUser(7, request);
  }

  @Test
  void configureNotificationSettings_updatesRepositoryAndReturnsEffectiveState() {
    NotificationSettingsRequest request = new NotificationSettingsRequest(
        true,
        false,
        true,
        null);
    User updatedUser = user(7, List.of(Notification.MEMBERSHIP_ACCEPTED, Notification.ACTIVITY_CLOSED));

    when(userRepository.getUserById(7)).thenReturn(updatedUser);

    NotificationSettingsResponse response =
        userProfileService.configureNotificationSettings(7, request);

    verify(userRepository).setNotification(7, Notification.MEMBERSHIP_ACCEPTED);
    verify(userRepository).removeNotification(7, Notification.MEMBERSHIP_REJECTED);
    verify(userRepository).setNotification(7, Notification.ACTIVITY_CLOSED);
    assertEquals(true, response.getMembershipAccepted());
    assertEquals(false, response.getMembershipRejected());
    assertEquals(true, response.getActivityClosed());
    assertEquals(false, response.getNewJoinRequest());
    assertNotNull(response.getUpdatedAt());
  }

  @Test
  void configureNotificationSettings_rejectsMissingRequest() {
    ValidationException exception = assertThrows(ValidationException.class,
        () -> userProfileService.configureNotificationSettings(7, null));

    assertEquals("Notification settings are required", exception.getMessage());
  }

  @Test
  void configureNotificationSettings_rejectsUnknownUserEvenWhenRequestHasNoChanges() {
    NotificationSettingsRequest request = new NotificationSettingsRequest(null, null, null, null);
    when(userRepository.getUserById(404)).thenReturn(null);

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> userProfileService.configureNotificationSettings(404, request));

    assertEquals("User not found", exception.getMessage());
    verify(userRepository, never()).setNotification(Mockito.anyInt(), Mockito.any());
    verify(userRepository, never()).removeNotification(Mockito.anyInt(), Mockito.any());
  }

  private User user(Integer id, List<Notification> notifications) {
    return new User(
        id,
        "user@example.com",
        "userName",
        Instant.parse("2000-01-01T00:00:00Z"),
        "Russia",
        "Moscow",
        "About user",
        5,
        List.of(),
        notifications);
  }
}
