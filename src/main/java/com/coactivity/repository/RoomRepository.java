package com.coactivity.repository;

import com.coactivity.domain.Category;
import com.coactivity.domain.Room;
import jakarta.persistence.criteria.CriteriaBuilder;

import java.time.Instant;

public interface RoomRepository {
  /*
  создание комнаты

  @param boolean isActive
  @param boolean isVisible
  @param String chatLink
  @param Category category
  @param String name
  @param String description
  @param Instant dateOfStartEvent
  @param Instant dateOfEndEvent
  @param int ageRating
  @param int frequency
  @param int maximumNumberOfPeople
  @param SimpleEntry<User, Role> users
  @return Room
   */
  Room createRoom(boolean isActive, boolean isVisible, String chatLink, Category category,
                  String name, String description, Instant dateOfStartEvent, Instant dateOfEndEvent,
                  int ageRating, int frequency, int maximumNumberOfPeople);


  /*
  получение комнату по её идентификатору

  @param int roomId
  @return Room
   */
  Room getRoomById(int roomId);

  /*
  обновление комнаты

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
  Room updateRoom(boolean isActive, boolean isVisible,
                  String description, Instant dateOfStartEvent,
                  Instant dateOfEndEvent, int ageRating,
                  int frequency, int maximumNumberOfPeople);

  /*
  удаление комнаты

  @param Room
   */
  void deleteRoom(Room room);
}
