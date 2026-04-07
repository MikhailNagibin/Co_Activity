package com.coactivity.service;

import com.coactivity.domain.User;
import com.coactivity.domain.UserAvatar;
import com.coactivity.repository.UserAvatarRepository;
import com.coactivity.repository.UserRepository;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.StorageException;
import com.coactivity.service.exception.ValidationException;
import com.coactivity.storage.FileStorage;
import com.coactivity.util.ImageContentValidator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserAvatarService {

  private static final Logger log = LoggerFactory.getLogger(UserAvatarService.class);
  private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
      "image/jpeg",
      "image/png",
      "image/webp");

  private final UserRepository userRepository;
  private final UserAvatarRepository userAvatarRepository;
  private final FileStorage fileStorage;
  private final long maxAvatarSizeBytes;

  public UserAvatarService(UserRepository userRepository,
      UserAvatarRepository userAvatarRepository,
      FileStorage fileStorage,
      @Value("${app.storage.avatar.max-size-bytes:5242880}") long maxAvatarSizeBytes) {
    this.userRepository = userRepository;
    this.userAvatarRepository = userAvatarRepository;
    this.fileStorage = fileStorage;
    this.maxAvatarSizeBytes = maxAvatarSizeBytes;
  }

  @Transactional
  public void uploadAvatar(Integer userId, MultipartFile file) {
    User user = getExistingUser(userId);
    ValidatedAvatarUpload upload = validateAndReadAvatarFile(file);

    UserAvatar previousAvatar = resolveAvatar(user.getAvatarFileId());
    String storageKey = generateStorageKey(upload.contentType());
    boolean stored = false;

    try {
      fileStorage.save(storageKey, new ByteArrayInputStream(upload.bytes()));
      stored = true;

      UserAvatar createdAvatar = userAvatarRepository.createAvatar(
          storageKey,
          upload.originalFilename(),
          upload.contentType(),
          upload.bytes().length);
      userRepository.updateAvatarFile(userId, createdAvatar.getId());

      if (previousAvatar != null) {
        userAvatarRepository.deleteAvatar(previousAvatar.getId());
        registerAfterCommitDeletion(previousAvatar.getStorageKey());
      }
    } catch (RuntimeException ex) {
      if (stored) {
        safelyDeleteStoredFile(storageKey);
      }
      throw ex;
    }
  }

  @Transactional
  public void deleteAvatar(Integer userId) {
    User user = getExistingUser(userId);
    UserAvatar existingAvatar = resolveAvatar(user.getAvatarFileId());
    if (existingAvatar == null) {
      return;
    }

    userRepository.clearAvatarFile(userId);
    userAvatarRepository.deleteAvatar(existingAvatar.getId());
    registerAfterCommitDeletion(existingAvatar.getStorageKey());
  }

  @Transactional(readOnly = true)
  public UserAvatarContent getAvatarContent(Integer userId) {
    User user = getExistingUser(userId);
    UserAvatar avatar = resolveAvatar(user.getAvatarFileId());
    if (avatar == null || !fileStorage.exists(avatar.getStorageKey())) {
      throw new ResourceNotFoundException("Avatar not found");
    }

    try (InputStream inputStream = fileStorage.open(avatar.getStorageKey())) {
      byte[] bytes = inputStream.readAllBytes();
      return new UserAvatarContent(bytes, avatar.getContentType(), avatar.getSizeBytes());
    } catch (IOException ex) {
      throw new StorageException("Unable to read avatar content", ex);
    }
  }

  private void validateAvatarFile(MultipartFile file) {
    if (file == null) {
      throw new ValidationException("Avatar file is required");
    }
    if (file.isEmpty()) {
      throw new ValidationException("Avatar file cannot be empty");
    }
    String contentType = file.getContentType();
    if (!StringUtils.hasText(contentType) || !SUPPORTED_CONTENT_TYPES.contains(contentType)) {
      throw new ValidationException("Unsupported avatar content type");
    }
    if (file.getSize() > maxAvatarSizeBytes) {
      throw new ValidationException("Avatar file exceeds maximum allowed size");
    }
  }

  private ValidatedAvatarUpload validateAndReadAvatarFile(MultipartFile file) {
    validateAvatarFile(file);
    try {
      byte[] bytes = file.getBytes();
      String contentType = file.getContentType();
      if (!ImageContentValidator.matchesContentType(contentType, bytes)) {
        throw new ValidationException("Avatar file content does not match declared image type");
      }
      return new ValidatedAvatarUpload(
          bytes,
          contentType,
          normalizeOriginalFilename(file.getOriginalFilename()));
    } catch (IOException ex) {
      throw new StorageException("Unable to read uploaded avatar", ex);
    }
  }

  private User getExistingUser(Integer userId) {
    if (userId == null) {
      throw new ValidationException("User id is required");
    }
    User user = userRepository.getUserById(userId);
    if (user == null) {
      throw new ResourceNotFoundException("User not found");
    }
    return user;
  }

  private UserAvatar resolveAvatar(Integer avatarFileId) {
    if (avatarFileId == null) {
      return null;
    }
    return userAvatarRepository.getAvatarById(avatarFileId);
  }

  private String generateStorageKey(String contentType) {
    return "avatars/" + UUID.randomUUID() + extensionForContentType(contentType);
  }

  private String extensionForContentType(String contentType) {
    return switch (contentType) {
      case "image/jpeg" -> ".jpg";
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      default -> throw new ValidationException("Unsupported avatar content type");
    };
  }

  private String normalizeOriginalFilename(String originalFilename) {
    if (!StringUtils.hasText(originalFilename)) {
      return null;
    }
    String cleaned = StringUtils.cleanPath(originalFilename).replace('\\', '/');
    int lastSlash = cleaned.lastIndexOf('/');
    String fileName = lastSlash >= 0 ? cleaned.substring(lastSlash + 1) : cleaned;
    return StringUtils.hasText(fileName) ? fileName : null;
  }

  private void registerAfterCommitDeletion(String storageKey) {
    if (!StringUtils.hasText(storageKey)) {
      return;
    }
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      safelyDeleteStoredFile(storageKey);
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        safelyDeleteStoredFile(storageKey);
      }
    });
  }

  private void safelyDeleteStoredFile(String storageKey) {
    try {
      fileStorage.delete(storageKey);
    } catch (RuntimeException ex) {
      log.warn("Failed to delete avatar file {} from storage", storageKey, ex);
    }
  }

  private record ValidatedAvatarUpload(byte[] bytes, String contentType, String originalFilename) {
  }
}
