package com.coactivity.repository;

import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import java.util.List;
import java.util.Map;

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

  List<Room> getRoomsOwnedByUser(Integer userId);

  /**
   *
   * @param roomId
   * @param userId
   * @param role
   */
  void addUserToRoom(Integer roomId, Integer userId, Role role);

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

  boolean isUserBannedInRoom(Integer roomId, Integer userId);

  int getRoomParticipantCount(Integer roomId);

  Map<User, Role> getUsersInRoom(Integer roomId);

  void setRoleByUserIdAndRoomId(Integer userId, Integer roomId, Role role);

  Role getUserRoleByRoomId(Integer roomId, Integer userId);

  void addUserBan(Integer roomId, Integer userId);
}
