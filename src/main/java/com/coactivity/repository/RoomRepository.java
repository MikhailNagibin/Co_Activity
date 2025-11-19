package com.coactivity.repository;

import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.domain.Room;

public interface RoomRepository {

  /**
   * Создание комнаты по запросу
   *
   * @param ownerId
   * @param request
   * @return
   */
  Room createRoom(int ownerId, RoomCreationRequest request);


  /**
   *
   * @param roomId
   * @return
   */
  Room getRoomById(int roomId);

  /**
   *
   * @param roomId
   * @param userId
   * @param roleId
   */
  void addUserToRoom(int roomId, int userId, int roleId);

  /**
   *
   * @param roomId
   */
  void deleteRoom(int roomId);

  /**
   *
   * @param roomId
   * @param userId
   * @return
   */
  boolean isUserInMembers(int roomId, int userId);
}
