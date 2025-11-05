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

  @param int userId
  @param int roomId
  @param Duration durationOfBan
  @param Instant dateOfBan
  @return Ban
   */
  Ban createBan(int userId, int roomId, Duration durationOfBan, Instant dateOfBan);

  /*
  просмотр всех баннов для комнаты

  @param int roomId
  @return List<Ban>
   */
  List<Ban> getBansByRoom(int roomId);

  /*
  просмотр всех банов для пользователя

  @param int userId
  @return List<Ban>
   */
  List<Ban> getBansByUser(int userId);
}