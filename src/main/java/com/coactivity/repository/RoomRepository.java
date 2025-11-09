package com.coactivity.repository;

import com.coactivity.domain.Room;
import java.time.Instant;
import java.util.AbstractMap;

public interface RoomRepository {
  /**
   *
   * @param isActive
   * @param isVisible
   * @param chatLink
   * @param categoryId
   * @param name
   * @param description
   * @param dateOfStartEvent
   * @param dateOfEndEvent
   * @param ageRating
   * @param frequency
   * @param maximumNumberOfPeople
   * @param users
   * @return
   */
  Room createRoom(boolean isActive, boolean isVisible, String chatLink, int categoryId,
                  String name, String description, Instant dateOfStartEvent, Instant dateOfEndEvent,
                  int ageRating, int frequency, int maximumNumberOfPeople,
                  AbstractMap.SimpleEntry<Integer, Integer> users);


  /**
   *
   * @param roomId
   * @return
   */
  Room getRoomById(int roomId);

  /**
   *
   * @param room
   * @param roomId
   * @param isActive
   * @param isVisible
   * @param description
   * @param dateOfStartEvent
   * @param dateOfEndEvent
   * @param ageRating
   * @param frequency
   * @param maximumNumberOfPeople
   * @return
   */
  Room updateRoom(Room room, int roomId, boolean isActive, boolean isVisible,
                  String description, Instant dateOfStartEvent,
                  Instant dateOfEndEvent, int ageRating,
                  int frequency, int maximumNumberOfPeople);

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
