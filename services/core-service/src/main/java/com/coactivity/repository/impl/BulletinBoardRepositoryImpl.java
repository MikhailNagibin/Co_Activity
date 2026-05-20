package com.coactivity.repository.impl;

import com.coactivity.domain.BulletinBoard;
import com.coactivity.persistence.CoreDomainMapper;
import com.coactivity.persistence.entity.BulletinBoardEntity;
import com.coactivity.persistence.repository.BulletinBoardJpaRepository;
import com.coactivity.persistence.repository.RoomJpaRepository;
import com.coactivity.persistence.repository.UserJpaRepository;
import com.coactivity.repository.BulletinBoardRepository;
import com.coactivity.service.exception.ResourceNotFoundException;
import java.time.Instant;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class BulletinBoardRepositoryImpl implements BulletinBoardRepository {

  private final BulletinBoardJpaRepository bulletinBoardJpaRepository;
  private final RoomJpaRepository roomJpaRepository;
  private final UserJpaRepository userJpaRepository;

  public BulletinBoardRepositoryImpl(BulletinBoardJpaRepository bulletinBoardJpaRepository,
      RoomJpaRepository roomJpaRepository,
      UserJpaRepository userJpaRepository) {
    this.bulletinBoardJpaRepository = bulletinBoardJpaRepository;
    this.roomJpaRepository = roomJpaRepository;
    this.userJpaRepository = userJpaRepository;
  }

  @Override
  public BulletinBoard createBulletinBoard(Integer roomId, String content, Integer authorId) {
    BulletinBoardEntity entity = new BulletinBoardEntity();
    entity.setRoom(roomJpaRepository.findById(roomId)
        .orElseThrow(() -> new ResourceNotFoundException("ROOM_NOT_FOUND",
            "Room not found: " + roomId)));
    entity.setAuthor(userJpaRepository.findById(authorId)
        .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
            "User not found: " + authorId)));
    entity.setContent(content);
    entity.setUpdatedAt(Instant.now());

    return toDomain(bulletinBoardJpaRepository.saveAndFlush(entity));
  }

  @Override
  public BulletinBoard updateBulletinBoard(Integer roomId, String content, Integer authorId) {
    BulletinBoardEntity entity = bulletinBoardJpaRepository.findByRoom_Id(roomId)
        .orElseThrow(() -> new ResourceNotFoundException("BULLETIN_BOARD_NOT_FOUND",
            "Bulletin board not found for room: " + roomId));
    entity.setContent(content);
    entity.setAuthor(userJpaRepository.findById(authorId)
        .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
            "User not found: " + authorId)));
    entity.setUpdatedAt(Instant.now());
    return toDomain(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public BulletinBoard getBulletinBoard(Integer roomId) {
    return bulletinBoardJpaRepository.findByRoom_Id(roomId)
        .map(this::toDomain)
        .orElse(null);
  }

  @Override
  public void deleteBulletinBoard(Integer roomId) {
    if (!bulletinBoardJpaRepository.existsByRoom_Id(roomId)) {
      throw new ResourceNotFoundException("BULLETIN_BOARD_NOT_FOUND",
          "Bulletin board not found for room: " + roomId);
    }
    bulletinBoardJpaRepository.deleteByRoom_Id(roomId);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isBulletinBoardExists(Integer roomId) {
    return bulletinBoardJpaRepository.existsByRoom_Id(roomId);
  }

  private BulletinBoard toDomain(BulletinBoardEntity entity) {
    return CoreDomainMapper.toBulletinBoard(
        entity,
        CoreDomainMapper.toRoom(entity.getRoom()),
        CoreDomainMapper.toUserSummary(entity.getAuthor()));
  }
}
