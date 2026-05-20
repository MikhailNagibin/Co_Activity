package com.coactivity.repository;

public interface RoomInvitationRepository {

  boolean exists(Integer roomId, Integer invitedUserId);

  void createIfAbsent(Integer roomId, Integer invitedUserId, Integer invitedByUserId);

  void delete(Integer roomId, Integer invitedUserId);

  void deleteAllByRoom(Integer roomId);

  void deleteAllByUser(Integer userId);
}
