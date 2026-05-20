package com.coactivity.repository.impl;

import com.coactivity.domain.Picture;
import com.coactivity.persistence.CoreDomainMapper;
import com.coactivity.persistence.entity.PictureEntity;
import com.coactivity.persistence.repository.PictureJpaRepository;
import com.coactivity.persistence.repository.RoomJpaRepository;
import com.coactivity.repository.PictureRepository;
import com.coactivity.service.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class PictureRepositoryImpl implements PictureRepository {

  private final PictureJpaRepository pictureJpaRepository;
  private final RoomJpaRepository roomJpaRepository;

  public PictureRepositoryImpl(PictureJpaRepository pictureJpaRepository,
      RoomJpaRepository roomJpaRepository) {
    this.pictureJpaRepository = pictureJpaRepository;
    this.roomJpaRepository = roomJpaRepository;
  }

  @Override
  public Picture createPicture(Integer roomId, String storageKey, String originalFilename,
      String contentType, long sizeBytes, int sortOrder) {
    PictureEntity entity = new PictureEntity();
    entity.setRoom(roomJpaRepository.findById(roomId)
        .orElseThrow(() -> new ResourceNotFoundException("ROOM_NOT_FOUND",
            "Room not found: " + roomId)));
    entity.setStorageKey(storageKey);
    entity.setOriginalFilename(originalFilename);
    entity.setContentType(contentType);
    entity.setSizeBytes(sizeBytes);
    entity.setSortOrder(sortOrder);
    entity.setCreatedAt(Instant.now());
    return toDomain(pictureJpaRepository.saveAndFlush(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Picture> getRoomPictures(Integer roomId) {
    return pictureJpaRepository.findAllByRoom_IdAndStorageKeyIsNotNullOrderBySortOrderAscIdAsc(roomId).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Picture getRoomPicture(Integer roomId, Integer photoId) {
    return pictureJpaRepository.findByIdAndRoom_IdAndStorageKeyIsNotNull(photoId, roomId)
        .map(this::toDomain)
        .orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public long countRoomPictures(Integer roomId) {
    return pictureJpaRepository.countByRoom_IdAndStorageKeyIsNotNull(roomId);
  }

  @Override
  public void deletePicture(Integer photoId) {
    if (!pictureJpaRepository.existsById(photoId)) {
      throw new ResourceNotFoundException("PICTURE_NOT_FOUND",
          "Picture not found with id: " + photoId);
    }
    pictureJpaRepository.deleteById(photoId);
    pictureJpaRepository.flush();
  }

  @Override
  public void updatePictureSortOrder(Integer photoId, int sortOrder) {
    PictureEntity entity = pictureJpaRepository.findById(photoId)
        .orElseThrow(() -> new ResourceNotFoundException("PICTURE_NOT_FOUND",
            "Picture not found with id: " + photoId));
    entity.setSortOrder(sortOrder);
    pictureJpaRepository.saveAndFlush(entity);
  }

  @Override
  public void deleteAllPicturesByRoomId(Integer roomId) {
    pictureJpaRepository.deleteAllByRoom_Id(roomId);
    pictureJpaRepository.flush();
  }

  private Picture toDomain(PictureEntity entity) {
    return CoreDomainMapper.toPicture(entity, CoreDomainMapper.toRoom(entity.getRoom()));
  }
}
