package com.coactivity.service;

import com.coactivity.controller.dto.response.RoomImageResponse;
import com.coactivity.domain.Picture;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.repository.PictureRepository;
import com.coactivity.repository.RoomRepository;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.StorageException;
import com.coactivity.service.exception.ValidationException;
import com.coactivity.storage.FileStorage;
import com.coactivity.util.ImageContentValidator;
import com.coactivity.util.RoomImageUrlResolver;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
public class RoomImageService {

  private static final String STORAGE_KEY_PREFIX = "room-images/";
  private static final Logger log = LoggerFactory.getLogger(RoomImageService.class);
  private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
      "image/jpeg",
      "image/png",
      "image/webp");
  private static final int MAX_IMAGES_PER_ROOM = 5;

  private final RoomRepository roomRepository;
  private final PictureRepository pictureRepository;
  private final FileStorage fileStorage;
  private final long maxRoomImageSizeBytes;

  public RoomImageService(RoomRepository roomRepository,
      PictureRepository pictureRepository,
      FileStorage fileStorage,
      @Value("${app.storage.room-image.max-size-bytes:5242880}") long maxRoomImageSizeBytes) {
    this.roomRepository = roomRepository;
    this.pictureRepository = pictureRepository;
    this.fileStorage = fileStorage;
    this.maxRoomImageSizeBytes = maxRoomImageSizeBytes;
  }

  @Transactional(readOnly = true)
  public List<RoomImageResponse> listRoomImages(Integer roomId) {
    validateRoomId(roomId);
    return pictureRepository.getRoomPictures(roomId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public List<RoomImageResponse> uploadRoomImages(Integer requesterId, Integer roomId,
      MultipartFile[] files) {
    requireOwner(requesterId, roomId);
    List<ValidatedRoomImageUpload> uploadFiles = validateUploadRequest(roomId, files);

    int nextSortOrder = (int) pictureRepository.countRoomPictures(roomId) + 1;
    List<String> storedKeys = new ArrayList<>();

    try {
      for (ValidatedRoomImageUpload file : uploadFiles) {
        String contentType = file.contentType();
        String storageKey = generateStorageKey(contentType);
        fileStorage.save(storageKey, new ByteArrayInputStream(file.bytes()));
        storedKeys.add(storageKey);
        pictureRepository.createPicture(
            roomId,
            storageKey,
            file.originalFilename(),
            contentType,
            file.bytes().length,
            nextSortOrder++);
      }
      return listRoomImages(roomId);
    } catch (RuntimeException ex) {
      cleanupStoredFiles(storedKeys);
      throw ex;
    }
  }

  @Transactional
  public List<RoomImageResponse> deleteRoomImage(Integer requesterId, Integer roomId,
      Integer imageId) {
    requireOwner(requesterId, roomId);
    Picture image = getExistingRoomImage(roomId, imageId);

    pictureRepository.deletePicture(imageId);
    reindexRoomImages(roomId);
    registerAfterCommitDeletion(image.getStorageKey());
    return listRoomImages(roomId);
  }

  @Transactional
  public void deleteAllImagesForRoom(Integer roomId) {
    validateRoomId(roomId);
    List<Picture> images = pictureRepository.getRoomPictures(roomId);
    if (images.isEmpty()) {
      pictureRepository.deleteAllPicturesByRoomId(roomId);
      return;
    }

    for (Picture image : images) {
      registerAfterCommitDeletion(image.getStorageKey());
    }
    pictureRepository.deleteAllPicturesByRoomId(roomId);
  }

  @Transactional(readOnly = true)
  public RoomImageContent getRoomImageContent(Integer roomId, Integer imageId) {
    Picture image = getExistingRoomImage(roomId, imageId);
    if (!StringUtils.hasText(image.getStorageKey()) || !fileStorage.exists(image.getStorageKey())) {
      throw new ResourceNotFoundException("ROOM_IMAGE_NOT_FOUND", "Room image not found");
    }

    try (InputStream inputStream = fileStorage.open(image.getStorageKey())) {
      byte[] bytes = inputStream.readAllBytes();
      return new RoomImageContent(
          bytes,
          image.getContentType(),
          image.getSizeBytes() != null ? image.getSizeBytes() : bytes.length);
    } catch (IOException ex) {
      throw new StorageException("Unable to read room image content", ex);
    }
  }

  private List<ValidatedRoomImageUpload> validateUploadRequest(Integer roomId, MultipartFile[] files) {
    if (files == null || files.length == 0) {
      throw new ValidationException("At least one room image is required");
    }
    List<MultipartFile> uploadFiles = Arrays.asList(files);
    uploadFiles.forEach(this::validateImageFile);

    long existingCount = pictureRepository.countRoomPictures(roomId);
    if (existingCount + uploadFiles.size() > MAX_IMAGES_PER_ROOM) {
      throw new ValidationException("Room cannot have more than 5 images");
    }
    List<ValidatedRoomImageUpload> validatedUploads = new ArrayList<>(uploadFiles.size());
    for (MultipartFile file : uploadFiles) {
      validatedUploads.add(readValidatedImage(file));
    }
    return validatedUploads;
  }

  private void validateImageFile(MultipartFile file) {
    if (file == null) {
      throw new ValidationException("Room image is required");
    }
    if (file.isEmpty()) {
      throw new ValidationException("Room image cannot be empty");
    }
    String contentType = file.getContentType();
    if (!StringUtils.hasText(contentType) || !SUPPORTED_CONTENT_TYPES.contains(contentType)) {
      throw new ValidationException("Unsupported room image content type");
    }
    if (file.getSize() > maxRoomImageSizeBytes) {
      throw new ValidationException("Room image exceeds maximum allowed size");
    }
  }

  private ValidatedRoomImageUpload readValidatedImage(MultipartFile file) {
    try {
      byte[] bytes = file.getBytes();
      String contentType = Objects.requireNonNull(file.getContentType());
      if (!ImageContentValidator.matchesContentType(contentType, bytes)) {
        throw new ValidationException("Room image content does not match declared image type");
      }
      return new ValidatedRoomImageUpload(
          bytes,
          contentType,
          normalizeOriginalFilename(file.getOriginalFilename()));
    } catch (IOException ex) {
      throw new StorageException("Unable to read uploaded room image", ex);
    }
  }

  private void requireOwner(Integer requesterId, Integer roomId) {
    validateRoomId(roomId);
    if (requesterId == null) {
      throw new ValidationException("Requester id is required");
    }
    Room room = roomRepository.getRoomById(roomId);
    if (room == null) {
      throw new ResourceNotFoundException("ROOM_NOT_FOUND", "Room not found");
    }
    if (!roomRepository.isUserInMembers(roomId, requesterId)
        || roomRepository.getUserRoleByRoomId(roomId, requesterId) != Role.OWNER) {
      throw new AuthorizationException("Only room owner can manage room images");
    }
  }

  private Picture getExistingRoomImage(Integer roomId, Integer imageId) {
    validateRoomId(roomId);
    if (imageId == null) {
      throw new ValidationException("Room image id is required");
    }
    Picture image = pictureRepository.getRoomPicture(roomId, imageId);
    if (image == null) {
      throw new ResourceNotFoundException("ROOM_IMAGE_NOT_FOUND", "Room image not found");
    }
    return image;
  }

  private void validateRoomId(Integer roomId) {
    if (roomId == null) {
      throw new ValidationException("Room id is required");
    }
  }

  private RoomImageResponse toResponse(Picture image) {
    return new RoomImageResponse(
        image.getPhotoId(),
        RoomImageUrlResolver.resolveRoomImageUrl(image.getRoom().getId(), image.getPhotoId()),
        image.getSortOrder());
  }

  private void reindexRoomImages(Integer roomId) {
    List<Picture> remainingImages = pictureRepository.getRoomPictures(roomId);
    int expectedOrder = 1;
    for (Picture image : remainingImages) {
      if (!Objects.equals(image.getSortOrder(), expectedOrder)) {
        pictureRepository.updatePictureSortOrder(image.getPhotoId(), expectedOrder);
      }
      expectedOrder++;
    }
  }

  private String generateStorageKey(String contentType) {
    return STORAGE_KEY_PREFIX + UUID.randomUUID() + extensionForContentType(contentType);
  }

  private String extensionForContentType(String contentType) {
    return switch (contentType) {
      case "image/jpeg" -> ".jpg";
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      default -> throw new ValidationException("Unsupported room image content type");
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

  private void cleanupStoredFiles(List<String> storedKeys) {
    storedKeys.forEach(this::safelyDeleteStoredFile);
  }

  private void safelyDeleteStoredFile(String storageKey) {
    try {
      fileStorage.delete(storageKey);
    } catch (RuntimeException ex) {
      log.warn("Failed to delete room image {} from storage", storageKey, ex);
    }
  }

  private record ValidatedRoomImageUpload(byte[] bytes, String contentType,
                                          String originalFilename) {
  }
}
