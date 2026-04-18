package com.coactivity.repository.impl;

import com.coactivity.auth.domain.UserStatus;
import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.domain.Notification;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.persistence.CoreDomainMapper;
import com.coactivity.persistence.CoreLookupMapper;
import com.coactivity.persistence.entity.NotificationEntity;
import com.coactivity.persistence.entity.UserEntity;
import com.coactivity.persistence.entity.UserAvatarEntity;
import com.coactivity.persistence.entity.UserNotificationEntity;
import com.coactivity.persistence.entity.UserNotificationId;
import com.coactivity.persistence.repository.NotificationLookupRepository;
import com.coactivity.persistence.repository.RoomMemberJpaRepository;
import com.coactivity.persistence.repository.UserAvatarJpaRepository;
import com.coactivity.persistence.repository.UserJpaRepository;
import com.coactivity.persistence.repository.UserNotificationJpaRepository;
import com.coactivity.repository.UserRepository;
import com.coactivity.service.exception.ConflictException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.ValidationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class UserRepositoryImpl implements UserRepository {

  private final UserJpaRepository userJpaRepository;
  private final RoomMemberJpaRepository roomMemberJpaRepository;
  private final UserNotificationJpaRepository userNotificationJpaRepository;
  private final NotificationLookupRepository notificationLookupRepository;
  private final UserAvatarJpaRepository userAvatarJpaRepository;
  private final PasswordEncoder passwordEncoder;

  @PersistenceContext
  private EntityManager entityManager;

  public UserRepositoryImpl(UserJpaRepository userJpaRepository,
      RoomMemberJpaRepository roomMemberJpaRepository,
      UserNotificationJpaRepository userNotificationJpaRepository,
      NotificationLookupRepository notificationLookupRepository,
      UserAvatarJpaRepository userAvatarJpaRepository,
      PasswordEncoder passwordEncoder) {
    this.userJpaRepository = userJpaRepository;
    this.roomMemberJpaRepository = roomMemberJpaRepository;
    this.userNotificationJpaRepository = userNotificationJpaRepository;
    this.notificationLookupRepository = notificationLookupRepository;
    this.userAvatarJpaRepository = userAvatarJpaRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public User createUser(UserRegistrationRequest request) {
    UserEntity entity = new UserEntity();
    entity.setEmail(request.getEmail().trim());
    entity.setEmailNormalized(request.getEmail().trim().toLowerCase(Locale.ROOT));
    entity.setUserName(request.getUserName().trim());
    entity.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    entity.setStatus(UserStatus.ACTIVE);
    entity.setEmailVerifiedAt(Instant.now());
    entity.setDataOfBirth(request.getDateOfBirth());
    entity.setCountry(request.getCountry());
    entity.setCity(request.getCity());
    entity.setDescription(request.getDescription());
    entity.setAvatarId(request.getAvatarId());

    UserEntity saved = userJpaRepository.saveAndFlush(entity);
    return CoreDomainMapper.toUser(saved, List.of(), List.of());
  }

  @Override
  public void updateUser(Integer userId, UserProfileUpdateRequest request) {
    UserEntity entity = getExistingUserEntity(userId);

    if (request.getUsername() != null) {
      String normalizedUserName = request.getUsername().trim();
      if (normalizedUserName.isEmpty()) {
        throw new ValidationException("Username cannot be blank");
      }
      if (userJpaRepository.existsByUserNameIgnoreCaseAndIdNot(normalizedUserName, userId)) {
        throw new ConflictException("USERNAME_ALREADY_TAKEN", "Username is already taken");
      }
      entity.setUserName(normalizedUserName);
    }
    if (request.getDateOfBirth() != null) {
      entity.setDataOfBirth(request.getDateOfBirth());
    }
    if (request.getCountry() != null) {
      entity.setCountry(blankToNull(request.getCountry()));
    }
    if (request.getCity() != null) {
      entity.setCity(blankToNull(request.getCity()));
    }
    if (request.getDescription() != null) {
      entity.setDescription(blankToNull(request.getDescription()));
    }
    if (request.getAvatarId() != null) {
      entity.setAvatarId(request.getAvatarId());
    }
  }

  @Override
  public void deleteUser(Integer userId) {
    entityManager.createNativeQuery("""
            UPDATE answers
            SET prev_ans_id = NULL
            WHERE prev_ans_id IN (
              SELECT id
              FROM answers
              WHERE owner = :userId
            )
            """)
        .setParameter("userId", userId)
        .executeUpdate();
    entityManager.createNativeQuery("""
            DELETE FROM answers
            WHERE question_id IN (
              SELECT id
              FROM questions
              WHERE owner = :userId
            )
            """)
        .setParameter("userId", userId)
        .executeUpdate();
    entityManager.createNativeQuery("DELETE FROM answers WHERE owner = :userId")
        .setParameter("userId", userId)
        .executeUpdate();
    entityManager.createNativeQuery("DELETE FROM questions WHERE owner = :userId")
        .setParameter("userId", userId)
        .executeUpdate();
    entityManager.createNativeQuery("DELETE FROM room_members WHERE user_id = :userId")
        .setParameter("userId", userId)
        .executeUpdate();
    entityManager.createNativeQuery("DELETE FROM room_requests WHERE user_id = :userId")
        .setParameter("userId", userId)
        .executeUpdate();
    entityManager.createNativeQuery("DELETE FROM bans WHERE user_id = :userId")
        .setParameter("userId", userId)
        .executeUpdate();
    entityManager.createNativeQuery("DELETE FROM user_notifications WHERE user_id = :userId")
        .setParameter("userId", userId)
        .executeUpdate();
    entityManager.createNativeQuery("DELETE FROM bulletin_board WHERE author_id = :userId")
        .setParameter("userId", userId)
        .executeUpdate();
    int affectedRows = entityManager.createNativeQuery("DELETE FROM users WHERE id = :userId")
        .setParameter("userId", userId)
        .executeUpdate();

    if (affectedRows == 0) {
      throw new ResourceNotFoundException("USER_NOT_FOUND", "User not found");
    }
  }

  @Override
  @Transactional(readOnly = true)
  public User getUserById(Integer userId) {
    return userJpaRepository.findById(userId)
        .map(this::toDomain)
        .orElse(null);
  }

  @Override
  public void updateAvatarFile(Integer userId, Integer avatarFileId) {
    UserEntity entity = getExistingUserEntity(userId);
    UserAvatarEntity avatarEntity = userAvatarJpaRepository.findById(avatarFileId)
        .orElseThrow(() -> new ResourceNotFoundException("AVATAR_METADATA_NOT_FOUND",
            "Avatar metadata not found"));
    entity.setAvatarFile(avatarEntity);
    userJpaRepository.flush();
  }

  @Override
  public void clearAvatarFile(Integer userId) {
    UserEntity entity = getExistingUserEntity(userId);
    entity.setAvatarFile(null);
    userJpaRepository.flush();
  }

  @Override
  public void setNotification(Integer userId, Notification notification) {
    UserEntity userEntity = getExistingUserEntity(userId);
    NotificationEntity notificationEntity = notificationLookupRepository.findByNotificationNameIgnoreCase(
            CoreLookupMapper.toDbNotificationName(notification))
        .orElseThrow(() -> new RuntimeException("Notification not found: " + notification));

    UserNotificationId linkId = new UserNotificationId(userId, notificationEntity.getId());
    if (userNotificationJpaRepository.findById(linkId).isPresent()) {
      return;
    }

    UserNotificationEntity link = new UserNotificationEntity();
    link.setId(linkId);
    link.setUser(userEntity);
    link.setNotification(notificationEntity);
    userNotificationJpaRepository.save(link);
  }

  @Override
  public void removeNotification(Integer userId, Notification notification) {
    NotificationEntity notificationEntity = notificationLookupRepository.findByNotificationNameIgnoreCase(
            CoreLookupMapper.toDbNotificationName(notification))
        .orElseThrow(() -> new RuntimeException("Notification not found: " + notification));

    userNotificationJpaRepository.findByUser_IdAndNotification_Id(userId, notificationEntity.getId())
        .ifPresent(userNotificationJpaRepository::delete);
  }

  @Transactional(readOnly = true)
  public List<Integer> getAllUsers() {
    return userJpaRepository.findAll().stream()
        .map(UserEntity::getId)
        .toList();
  }

  private UserEntity getExistingUserEntity(Integer userId) {
    return userJpaRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));
  }

  private User toDomain(UserEntity entity) {
    List<Room> rooms = roomMemberJpaRepository.findAllByUser_Id(entity.getId()).stream()
        .map(link -> CoreDomainMapper.toRoom(link.getRoom()))
        .toList();
    List<Notification> notifications = userNotificationJpaRepository.findAllByUser_Id(entity.getId())
        .stream()
        .map(link -> CoreLookupMapper.toNotification(link.getNotification().getNotificationName()))
        .toList();
    return CoreDomainMapper.toUser(entity, rooms, notifications);
  }

  private String blankToNull(String value) {
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
