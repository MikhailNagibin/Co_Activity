package com.coactivity.repository.impl;

import com.coactivity.persistence.repository.RoomInvitationJpaRepository;
import com.coactivity.repository.RoomInvitationRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class RoomInvitationRepositoryImpl implements RoomInvitationRepository {

  private final RoomInvitationJpaRepository roomInvitationJpaRepository;

  public RoomInvitationRepositoryImpl(RoomInvitationJpaRepository roomInvitationJpaRepository) {
    this.roomInvitationJpaRepository = roomInvitationJpaRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean exists(Integer roomId, Integer invitedUserId) {
    return roomInvitationJpaRepository.existsByRoom_IdAndInvitedUser_Id(roomId, invitedUserId);
  }

  @Override
  public void createIfAbsent(Integer roomId, Integer invitedUserId, Integer invitedByUserId) {
    roomInvitationJpaRepository.createIfAbsent(roomId, invitedUserId, invitedByUserId);
  }

  @Override
  public void delete(Integer roomId, Integer invitedUserId) {
    roomInvitationJpaRepository.deleteByRoom_IdAndInvitedUser_Id(roomId, invitedUserId);
  }

  @Override
  public void deleteAllByRoom(Integer roomId) {
    roomInvitationJpaRepository.deleteAllByRoom_Id(roomId);
  }

  @Override
  public void deleteAllByUser(Integer userId) {
    roomInvitationJpaRepository.deleteAllByUserId(userId);
  }
}
