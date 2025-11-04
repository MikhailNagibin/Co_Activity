package com.coactivity.repository;

import com.coactivity.domain.Ban;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface BanRepository {
  /*
  создание банна для пользователя в комнату

  @param User user
  @param Room room
  @param Duration durationOfBan
  @param Instant dateOfBan
  @return Ban
   */
  Ban createBan(User user, Room room, Duration durationOfBan, Instant dateOfBan);

  /*
  просмотр всех баннов для комнаты

  @param Room room
  @return List<Ban>
   */
  List<Ban> getBansByRoom(Room room);

  /*
  просмотр всех банов для пользователя

  @param User user
  @return List<Ban>
   */
  List<Ban> getBansByUser(User user);
}