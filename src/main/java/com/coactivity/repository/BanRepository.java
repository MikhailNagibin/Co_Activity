package com.coactivity.repository;

import com.coactivity.domain.Ban;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface BanRepository {
  /**
   *
   * @param userId
   * @param roomId
   * @param dateOfBan
   * @return
   */
  Ban createBan(int userId, int roomId, Instant dateOfBan);

  /**
   *
   * @param roomId
   * @return List< Bans>
   */
  List<Ban> getBansByRoom(int roomId);

  /**
   *
   * @param userId
   * @return List< Bans>
   */
  List<Ban> getBansByUser(int userId);
}