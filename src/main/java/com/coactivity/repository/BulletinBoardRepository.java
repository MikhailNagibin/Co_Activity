package com.coactivity.repository;

import com.coactivity.domain.BulletinBoard;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;

import java.time.Instant;

public interface BulletinBoardRepository {
  /**
   *
   * @param roomId
   * @param content
   * @param authorId
   * @return
   */
  BulletinBoard createBulletinBoard(int roomId, String content, int authorId);

  /**
   *
   * @param roomId
   * @param content
   * @param authorId
   * @return BulletinBoard обновленная доска объявлений
   */
  BulletinBoard updateBulletinBoard(int roomId, String content, int authorId);

  /**
   *
   * @param roomId
   * @return
   */
  BulletinBoard getBulletinBoard(int roomId);

  /**
   *
   * @param roomId
   */
  void deleteBulletinBoard(int roomId);
}