package com.coactivity.repository.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.persistence.entity.UserEntity;
import com.coactivity.persistence.repository.NotificationLookupRepository;
import com.coactivity.persistence.repository.RoomMemberJpaRepository;
import com.coactivity.persistence.repository.UserJpaRepository;
import com.coactivity.persistence.repository.UserNotificationJpaRepository;
import com.coactivity.service.exception.ConflictException;
import com.coactivity.service.exception.ValidationException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("UserRepository update tests")
class UserRepositoryImplUpdateTest {

  private UserJpaRepository userJpaRepository;
  private UserRepositoryImpl userRepository;

  @BeforeEach
  void setUp() {
    userJpaRepository = Mockito.mock(UserJpaRepository.class);
    userRepository = new UserRepositoryImpl(
        userJpaRepository,
        Mockito.mock(RoomMemberJpaRepository.class),
        Mockito.mock(UserNotificationJpaRepository.class),
        Mockito.mock(NotificationLookupRepository.class),
        Mockito.mock(PasswordEncoder.class));
  }

  @Test
  void updateUserThrowsConflictForCaseInsensitiveDuplicateUsername() {
    UserEntity entity = existingUser();
    when(userJpaRepository.findById(1)).thenReturn(Optional.of(entity));
    when(userJpaRepository.existsByUserNameIgnoreCaseAndIdNot("TakenName", 1)).thenReturn(true);

    UserProfileUpdateRequest request = new UserProfileUpdateRequest("TakenName", null, null, null,
        null, null);

    ConflictException exception = assertThrows(ConflictException.class,
        () -> userRepository.updateUser(1, request));

    assertEquals("USERNAME_ALREADY_TAKEN", exception.getCode());
  }

  @Test
  void updateUserTrimsUsernameAndNormalizesBlankOptionalFields() {
    UserEntity entity = existingUser();
    when(userJpaRepository.findById(1)).thenReturn(Optional.of(entity));
    when(userJpaRepository.existsByUserNameIgnoreCaseAndIdNot("newName", 1)).thenReturn(false);

    UserProfileUpdateRequest request = new UserProfileUpdateRequest(
        "  newName  ",
        Instant.parse("2000-01-01T00:00:00Z"),
        "   ",
        "  ",
        "  about me  ",
        7);

    userRepository.updateUser(1, request);

    assertEquals("newName", entity.getUserName());
    assertEquals(Instant.parse("2000-01-01T00:00:00Z"), entity.getDataOfBirth());
    assertNull(entity.getCity());
    assertNull(entity.getCountry());
    assertEquals("about me", entity.getDescription());
    assertEquals(7, entity.getAvatarId());
  }

  @Test
  void updateUserRejectsBlankUsernameAfterTrimming() {
    UserEntity entity = existingUser();
    when(userJpaRepository.findById(1)).thenReturn(Optional.of(entity));

    UserProfileUpdateRequest request = new UserProfileUpdateRequest("   ", null, null, null,
        null, null);

    ValidationException exception = assertThrows(ValidationException.class,
        () -> userRepository.updateUser(1, request));

    assertEquals("Username cannot be blank", exception.getMessage());
    assertEquals("oldName", entity.getUserName());
  }

  private UserEntity existingUser() {
    UserEntity entity = new UserEntity();
    entity.setId(1);
    entity.setUserName("oldName");
    entity.setCountry("Russia");
    entity.setCity("Moscow");
    entity.setDescription("bio");
    entity.setAvatarId(1);
    return entity;
  }
}
