package com.coactivity.repository;

import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import java.util.List;

public interface RoomRepository {

  /**
   * Создание комнаты по запросу
   *
   * @param ownerId
   * @param request
   * @return
   */
  Room createRoom(Integer ownerId, RoomCreationRequest request);


  /**
   *
   * @param roomId
   * @return
   */
  Room getRoomById(Integer roomId);

  /**
   *
   * @return
   */
  List<Room> getAllRooms();

  /**
   *
   * @param roomId
   * @param userId
   * @param role
   */
  void addUserToRoom(Integer roomId, Integer userId, String role);

  /**
   *
   * @param roomId
   */
  void deleteRoom(Integer roomId);

  /**
   *
   * @param roomId
   * @param userId
   * @return
   */
  boolean isUserInMembers(Integer roomId, Integer userId);

  /**
   * Removes the specified user from the room membership list.
   *
   * @param roomId identifier of the room
   * @param userId identifier of the user to remove
   */
  void removeUserFromRoom(Integer roomId, Integer userId);
}
