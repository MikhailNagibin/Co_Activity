package com.coactivity.repository.impl;

import com.coactivity.domain.Picture;
import com.coactivity.persistence.CoreDomainMapper;
import com.coactivity.persistence.entity.PictureEntity;
import com.coactivity.persistence.repository.PictureJpaRepository;
import com.coactivity.persistence.repository.RoomJpaRepository;
import com.coactivity.repository.PictureRepository;
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
  public Picture createPicture(Integer roomId) {
    PictureEntity entity = new PictureEntity();
    entity.setRoom(roomJpaRepository.findById(roomId)
        .orElseThrow(() -> new RuntimeException("Room not found: " + roomId)));
    return toDomain(pictureJpaRepository.saveAndFlush(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Picture> getRoomPictures(Integer roomId) {
    return pictureJpaRepository.findAllByRoom_Id(roomId).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public void deletePicture(Integer photoId) {
    if (!pictureJpaRepository.existsById(photoId)) {
      throw new RuntimeException("Picture not found with id: " + photoId);
    }
    pictureJpaRepository.deleteById(photoId);
  }

  private Picture toDomain(PictureEntity entity) {
    return CoreDomainMapper.toPicture(entity, CoreDomainMapper.toRoom(entity.getRoom()));
  }
}
