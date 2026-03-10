package com.coactivity.repository.impl;

import com.coactivity.domain.BulletinBoard;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.repository.BulletinBoardRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
@Transactional
public class BulletinBoardRepositoryImpl implements BulletinBoardRepository {

  @PersistenceContext
  private EntityManager entityManager;

  private final UserRepositoryImpl userRepository;
  private final RoomRepositoryImpl roomRepository;

  public BulletinBoardRepositoryImpl(UserRepositoryImpl userRepository,
                                     RoomRepositoryImpl roomRepository) {
    this.userRepository = userRepository;
    this.roomRepository = roomRepository;
  }

  @Override
  public BulletinBoard createBulletinBoard(Integer roomId, String content, Integer authorId) {
    Room room = roomRepository.getRoomById(roomId);
    User author = userRepository.getUserById(authorId);
    Instant now = Instant.now();

    BulletinBoard bulletinBoard = new BulletinBoard();
    bulletinBoard.setRoom(room);
    bulletinBoard.setContent(content);
    bulletinBoard.setAuthor(author);
    bulletinBoard.setUpdatedAt(now);

    entityManager.persist(bulletinBoard);
    return bulletinBoard;
  }

  @Override
  public BulletinBoard updateBulletinBoard(Integer roomId, String content, Integer authorId) {
    BulletinBoard bulletinBoard = getBulletinBoard(roomId);
    if (bulletinBoard == null) {
      throw new RuntimeException("Bulletin board not found for room: " + roomId);
    }

    User author = userRepository.getUserById(authorId);

    bulletinBoard.setContent(content);
    bulletinBoard.setAuthor(author);
    bulletinBoard.setUpdatedAt(Instant.now());

    return entityManager.merge(bulletinBoard);
  }

  @Override
  public BulletinBoard getBulletinBoard(Integer roomId) {
    try {
      return entityManager.createQuery(
              "SELECT b FROM BulletinBoard b WHERE b.room.id = :roomId",
              BulletinBoard.class)
          .setParameter("roomId", roomId)
          .getSingleResult();
    } catch (jakarta.persistence.NoResultException e) {
      return null;
    }
  }

  @Override
  public void deleteBulletinBoard(Integer roomId) {
    BulletinBoard bulletinBoard = getBulletinBoard(roomId);
    if (bulletinBoard != null) {
      entityManager.remove(bulletinBoard);
    } else {
      throw new RuntimeException("Bulletin board not found for room: " + roomId);
    }
  }

  @Override
  public boolean isBulletinBoardExists(Integer roomId) {
    Long count = entityManager.createQuery(
            "SELECT COUNT(b) FROM BulletinBoard b WHERE b.room.id = :roomId",
            Long.class)
        .setParameter("roomId", roomId)
        .getSingleResult();
    return count > 0;
  }
}
