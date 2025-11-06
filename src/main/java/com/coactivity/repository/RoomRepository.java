package com.coactivity.repository;

import com.coactivity.domain.Room;
import java.time.Instant;
import java.util.AbstractMap;

public interface RoomRepository {
  /*
  создание комнаты

  @param boolean isActive
  @param boolean isVisible
  @param String chatLink
  @param int categoryId
  @param String name
  @param String description
  @param Instant dateOfStartEvent
  @param Instant dateOfEndEvent
  @param int ageRating
  @param int frequency
  @param int maximumNumberOfPeople
  @param AbstractMap.SimpleEntry<Integer, Integer> users (userId, roleId)
  @return Room
   */
  Room createRoom(boolean isActive, boolean isVisible, String chatLink, int categoryId,
                  String name, String description, Instant dateOfStartEvent, Instant dateOfEndEvent,
                  int ageRating, int frequency, int maximumNumberOfPeople,
                  AbstractMap.SimpleEntry<Integer, Integer> users);


  /*
  получение комнату по её идентификатору

  @param int roomId
  @return Room
   */
  Room getRoomById(int roomId);

  /*
  обновление комнаты

  @param int roomId
  @param boolean isActive
  @param boolean isVisible
  @param String description
  @param Instant dateOfStartEvent
  @param Instant dateOfEndEvent
  @param int ageRating
  @param int frequency
  @param int maximumNumberOfPeople
  @return Room
   */
  Room updateRoom(int roomId, boolean isActive, boolean isVisible,
                  String description, Instant dateOfStartEvent,
                  Instant dateOfEndEvent, int ageRating,
                  int frequency, int maximumNumberOfPeople);

  /*
  добавление человека в комнату

  @param int roomId
  @param int userId
  @param int roleId
   */
  void addUserToRoom(int roomId, int userId, int roleId);

  /*
  удаление комнаты

  @param int roomId
   */
  void deleteRoom(int roomId);
}
