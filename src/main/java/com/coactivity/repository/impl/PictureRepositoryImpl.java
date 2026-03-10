package com.coactivity.repository.impl;

import com.coactivity.domain.Picture;
import com.coactivity.domain.Room;
import com.coactivity.repository.PictureRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Transactional
public class PictureRepositoryImpl implements PictureRepository {

  @PersistenceContext
  private EntityManager entityManager;

  private final RoomRepositoryImpl roomRepository;

  public PictureRepositoryImpl(RoomRepositoryImpl roomRepository) {
    this.roomRepository = roomRepository;
  }

  @Override
  public Picture createPicture(Integer roomId) {
    Room room = roomRepository.getRoomById(roomId);

    Picture picture = new Picture();
    picture.setRoom(room);

    entityManager.persist(picture);
    return picture;
  }

  @Override
  public List<Picture> getRoomPictures(Integer roomId) {
    return entityManager.createQuery(
            "SELECT p FROM Picture p WHERE p.room.id = :roomId",
            Picture.class)
        .setParameter("roomId", roomId)
        .getResultList();
  }

  @Override
  public void deletePicture(Integer photoId) {
    Picture picture = entityManager.find(Picture.class, photoId);
    if (picture != null) {
      entityManager.remove(picture);
    } else {
      throw new RuntimeException("Picture not found with id: " + photoId);
    }
  }
}
